# Message Queue Design Specification

## Table of Contents
- [Overview](#overview)
- [Technology Selection](#technology-selection)
- [Queue/Topic Structure](#queuetopic-structure)
- [Event Schemas](#event-schemas)
- [Producer Configuration](#producer-configuration)
- [Consumer Configuration](#consumer-configuration)
- [Message Ordering](#message-ordering)
- [Idempotency](#idempotency)
- [Dead Letter Queues](#dead-letter-queues)
- [Monitoring and Observability](#monitoring-and-observability)
- [Security](#security)
- [Performance Tuning](#performance-tuning)

## Overview

The Novelist application uses message queues for asynchronous communication between the Novelist service and RAG ingestion service. This enables loose coupling, scalability, and fault tolerance.

### Key Requirements

1. **Reliability**: Messages must be delivered at least once
2. **Ordering**: Maintain order for events related to the same entity
3. **Scalability**: Support high throughput and horizontal scaling
4. **Fault Tolerance**: Handle failures gracefully with retries and DLQ
5. **Observability**: Track message flow and processing metrics
6. **Security**: Encrypt messages in transit and at rest

## Technology Selection

### Option 1: Apache Kafka (Recommended for Production)

**Pros**:
- High throughput (millions of messages/second)
- Horizontal scalability
- Message persistence and replay capability
- Strong ordering guarantees per partition
- Event streaming capabilities
- Large ecosystem and community

**Cons**:
- Complex setup and operations
- Higher resource requirements
- Steeper learning curve

**Use Cases**:
- High-volume event processing
- Event sourcing
- Real-time analytics
- Log aggregation

**Setup**:
```yaml
# docker-compose.yml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
      - "9093:9093"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:9093
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"
```

### Option 2: RabbitMQ (Recommended for Getting Started)

**Pros**:
- Simpler setup and operations
- Flexible routing with exchanges
- Good for traditional messaging patterns
- Lower resource requirements
- Management UI included
- Multiple protocol support (AMQP, MQTT, STOMP)

**Cons**:
- Lower throughput than Kafka
- No built-in message replay
- Limited event streaming capabilities

**Use Cases**:
- Request-response patterns
- Task queues
- Traditional messaging
- Moderate throughput requirements

**Setup**:
```yaml
# docker-compose.yml
version: '3.8'
services:
  rabbitmq:
    image: rabbitmq:3.12-management
    ports:
      - "5672:5672"   # AMQP
      - "15672:15672" # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: novelist
      RABBITMQ_DEFAULT_PASS: password
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

volumes:
  rabbitmq_data:
```

## Queue/Topic Structure

### Kafka Topics

```yaml
topics:
  # Book lifecycle events
  - name: novelist.book.events
    partitions: 3
    replication_factor: 2
    retention_ms: 604800000  # 7 days
    config:
      cleanup.policy: delete
      compression.type: snappy
      min.insync.replicas: 1
    
  # User lifecycle events
  - name: novelist.user.events
    partitions: 3
    replication_factor: 2
    retention_ms: 604800000
    
  # Rating events
  - name: novelist.rating.events
    partitions: 5
    replication_factor: 2
    retention_ms: 604800000
    
  # Embedding generation results
  - name: novelist.embedding.events
    partitions: 3
    replication_factor: 2
    retention_ms: 604800000
    
  # Search requests
  - name: novelist.search.requests
    partitions: 5
    replication_factor: 2
    retention_ms: 86400000  # 1 day
    config:
      cleanup.policy: delete
      
  # Search responses
  - name: novelist.search.responses
    partitions: 5
    replication_factor: 2
    retention_ms: 86400000
    
  # Recommendation requests
  - name: novelist.recommendation.requests
    partitions: 3
    replication_factor: 2
    retention_ms: 86400000
    
  # Recommendation responses
  - name: novelist.recommendation.responses
    partitions: 3
    replication_factor: 2
    retention_ms: 86400000
    
  # Dead letter queue
  - name: novelist.dlq
    partitions: 1
    replication_factor: 2
    retention_ms: 2592000000  # 30 days
```

### RabbitMQ Exchanges and Queues

```yaml
exchanges:
  # Topic exchange for book events
  - name: novelist.book.events
    type: topic
    durable: true
    auto_delete: false
    
  # Topic exchange for user events
  - name: novelist.user.events
    type: topic
    durable: true
    
  # Direct exchange for search
  - name: novelist.search
    type: direct
    durable: true
    
  # Direct exchange for recommendations
  - name: novelist.recommendations
    type: direct
    durable: true
    
  # Dead letter exchange
  - name: novelist.dlx
    type: topic
    durable: true

queues:
  # Book event queues
  - name: novelist.book.created
    exchange: novelist.book.events
    routing_key: book.created
    durable: true
    arguments:
      x-message-ttl: 604800000  # 7 days
      x-dead-letter-exchange: novelist.dlx
      x-dead-letter-routing-key: book.created.failed
      
  - name: novelist.book.updated
    exchange: novelist.book.events
    routing_key: book.updated
    durable: true
    arguments:
      x-message-ttl: 604800000
      x-dead-letter-exchange: novelist.dlx
      
  - name: novelist.book.deleted
    exchange: novelist.book.events
    routing_key: book.deleted
    durable: true
    
  # Embedding queues
  - name: novelist.embedding.generated
    exchange: novelist.book.events
    routing_key: embedding.generated
    durable: true
    
  # Search queues
  - name: novelist.search.requests
    exchange: novelist.search
    routing_key: request
    durable: true
    arguments:
      x-message-ttl: 86400000  # 1 day
      
  - name: novelist.search.responses
    exchange: novelist.search
    routing_key: response
    durable: true
    arguments:
      x-message-ttl: 86400000
      
  # Dead letter queue
  - name: novelist.dlq
    exchange: novelist.dlx
    routing_key: "#"
    durable: true
    arguments:
      x-message-ttl: 2592000000  # 30 days
```

## Event Schemas

### Base Event Schema

All events extend this base schema:

```json
{
  "eventId": "string (UUID)",
  "eventType": "string",
  "version": "integer",
  "timestamp": "string (ISO 8601)",
  "correlationId": "string (UUID, optional)",
  "source": "string (service name)",
  "metadata": {
    "userId": "string (optional)",
    "traceId": "string (optional)"
  }
}
```

### Book Events

#### BookCreatedEvent

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "BookCreated",
  "version": 1,
  "timestamp": "2026-06-18T10:30:00.000Z",
  "correlationId": "660e8400-e29b-41d4-a716-446655440001",
  "source": "novelist-service",
  "metadata": {
    "userId": "user-123",
    "traceId": "trace-456"
  },
  "payload": {
    "bookId": "book-789",
    "title": "The Great Gatsby",
    "author": "F. Scott Fitzgerald",
    "isbn": "978-0-7432-7356-5",
    "publishedYear": 1925,
    "genre": ["Fiction", "Classic"],
    "description": "A novel set in the Jazz Age...",
    "content": "Full text content for embedding generation...",
    "language": "en",
    "pageCount": 180
  }
}
```

**Java DTO**:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCreatedEvent extends BaseEvent {
    private String bookId;
    private String title;
    private String author;
    private String isbn;
    private Integer publishedYear;
    private List<String> genre;
    private String description;
    private String content;
    private String language;
    private Integer pageCount;
}
```

#### BookUpdatedEvent

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440002",
  "eventType": "BookUpdated",
  "version": 1,
  "timestamp": "2026-06-18T11:00:00.000Z",
  "source": "novelist-service",
  "payload": {
    "bookId": "book-789",
    "title": "The Great Gatsby (Updated Edition)",
    "author": "F. Scott Fitzgerald",
    "updatedFields": ["title", "description"],
    "previousValues": {
      "title": "The Great Gatsby"
    },
    "content": "Updated full text content..."
  }
}
```

#### BookDeletedEvent

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440003",
  "eventType": "BookDeleted",
  "version": 1,
  "timestamp": "2026-06-18T11:30:00.000Z",
  "source": "novelist-service",
  "payload": {
    "bookId": "book-789",
    "deletedAt": "2026-06-18T11:30:00.000Z",
    "reason": "User requested deletion"
  }
}
```

### User Events

#### UserCreatedEvent

```json
{
  "eventId": "660e8400-e29b-41d4-a716-446655440004",
  "eventType": "UserCreated",
  "version": 1,
  "timestamp": "2026-06-18T09:00:00.000Z",
  "source": "novelist-service",
  "payload": {
    "userId": "user-123",
    "name": "John Doe",
    "email": "john.doe@example.com",
    "age": 30,
    "preferences": {
      "genres": ["Fiction", "Science Fiction"],
      "authors": ["Isaac Asimov", "Arthur C. Clarke"]
    }
  }
}
```

#### RatingAddedEvent

```json
{
  "eventId": "770e8400-e29b-41d4-a716-446655440005",
  "eventType": "RatingAdded",
  "version": 1,
  "timestamp": "2026-06-18T12:00:00.000Z",
  "source": "novelist-service",
  "payload": {
    "ratingId": "rating-456",
    "userId": "user-123",
    "bookId": "book-789",
    "rating": 5,
    "review": "Excellent book!",
    "ratedAt": "2026-06-18T12:00:00.000Z"
  }
}
```

### Embedding Events

#### EmbeddingGeneratedEvent

```json
{
  "eventId": "880e8400-e29b-41d4-a716-446655440006",
  "eventType": "EmbeddingGenerated",
  "version": 1,
  "timestamp": "2026-06-18T10:35:00.000Z",
  "source": "rag-service",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "payload": {
    "bookId": "book-789",
    "embeddingId": "embedding-321",
    "model": "text-embedding-ada-002",
    "vectorDimensions": 1536,
    "numChunks": 15,
    "processingTimeMs": 2500,
    "status": "success",
    "vectorStoreId": "pinecone-index-123"
  }
}
```

#### EmbeddingFailedEvent

```json
{
  "eventId": "880e8400-e29b-41d4-a716-446655440007",
  "eventType": "EmbeddingFailed",
  "version": 1,
  "timestamp": "2026-06-18T10:35:00.000Z",
  "source": "rag-service",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "payload": {
    "bookId": "book-789",
    "errorCode": "EMBEDDING_GENERATION_FAILED",
    "errorMessage": "OpenAI API rate limit exceeded",
    "retryable": true,
    "retryAfterSeconds": 60
  }
}
```

### Search Events

#### SearchRequestEvent

```json
{
  "eventId": "990e8400-e29b-41d4-a716-446655440008",
  "eventType": "SearchRequest",
  "version": 1,
  "timestamp": "2026-06-18T13:00:00.000Z",
  "source": "novelist-service",
  "correlationId": "search-query-123",
  "payload": {
    "queryId": "search-query-123",
    "query": "fantasy books with dragons",
    "filters": {
      "genres": ["Fantasy"],
      "minRating": 4.0,
      "publishedAfter": 2000
    },
    "limit": 10,
    "userId": "user-123",
    "searchType": "semantic"
  }
}
```

#### SearchResponseEvent

```json
{
  "eventId": "990e8400-e29b-41d4-a716-446655440009",
  "eventType": "SearchResponse",
  "version": 1,
  "timestamp": "2026-06-18T13:00:05.000Z",
  "source": "rag-service",
  "correlationId": "search-query-123",
  "payload": {
    "queryId": "search-query-123",
    "results": [
      {
        "bookId": "book-101",
        "title": "The Hobbit",
        "author": "J.R.R. Tolkien",
        "score": 0.95,
        "relevanceReason": "High semantic similarity to query"
      },
      {
        "bookId": "book-102",
        "title": "Eragon",
        "author": "Christopher Paolini",
        "score": 0.89,
        "relevanceReason": "Contains dragons and fantasy elements"
      }
    ],
    "totalResults": 2,
    "processingTimeMs": 150
  }
}
```

### Recommendation Events

#### RecommendationRequestEvent

```json
{
  "eventId": "aa0e8400-e29b-41d4-a716-446655440010",
  "eventType": "RecommendationRequest",
  "version": 1,
  "timestamp": "2026-06-18T14:00:00.000Z",
  "source": "novelist-service",
  "correlationId": "rec-request-456",
  "payload": {
    "requestId": "rec-request-456",
    "userId": "user-123",
    "basedOn": {
      "ratedBooks": ["book-789", "book-101"],
      "preferences": ["Fantasy", "Science Fiction"]
    },
    "limit": 5,
    "excludeBooks": ["book-789", "book-101"]
  }
}
```

#### RecommendationResponseEvent

```json
{
  "eventId": "aa0e8400-e29b-41d4-a716-446655440011",
  "eventType": "RecommendationResponse",
  "version": 1,
  "timestamp": "2026-06-18T14:00:03.000Z",
  "source": "rag-service",
  "correlationId": "rec-request-456",
  "payload": {
    "requestId": "rec-request-456",
    "userId": "user-123",
    "recommendations": [
      {
        "bookId": "book-201",
        "title": "Dune",
        "author": "Frank Herbert",
        "score": 0.92,
        "reason": "Similar to your highly-rated books"
      },
      {
        "bookId": "book-202",
        "title": "The Name of the Wind",
        "author": "Patrick Rothfuss",
        "score": 0.88,
        "reason": "Popular among users with similar tastes"
      }
    ],
    "totalRecommendations": 2,
    "algorithm": "collaborative-filtering-with-embeddings",
    "processingTimeMs": 300
  }
}
```

## Producer Configuration

### Kafka Producer (Spring Boot)

```java
@Configuration
public class KafkaProducerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Reliability settings
        config.put(ProducerConfig.ACKS_CONFIG, "all");  // Wait for all replicas
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);  // Ordering
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Performance settings
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### RabbitMQ Producer (Spring Boot)

```java
@Configuration
public class RabbitMQProducerConfig {
    
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        
        // Reliability settings
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("Message not confirmed: {}", cause);
            }
        });
        
        template.setReturnsCallback(returned -> {
            log.error("Message returned: {}", returned.getMessage());
        });
        
        return template;
    }
    
    @Bean
    public DirectExchange bookEventsExchange() {
        return new DirectExchange("novelist.book.events", true, false);
    }
    
    @Bean
    public Queue bookCreatedQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 604800000);  // 7 days
        args.put("x-dead-letter-exchange", "novelist.dlx");
        args.put("x-dead-letter-routing-key", "book.created.failed");
        
        return new Queue("novelist.book.created", true, false, false, args);
    }
    
    @Bean
    public Binding bookCreatedBinding() {
        return BindingBuilder
            .bind(bookCreatedQueue())
            .to(bookEventsExchange())
            .with("book.created");
    }
}
```

### Event Publisher Service

```java
@Service
@Slf4j
public class EventPublisherService {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    private static final String BOOK_EVENTS_TOPIC = "novelist.book.events";
    
    public CompletableFuture<SendResult<String, Object>> publishBookCreated(Book book) {
        BookCreatedEvent event = buildBookCreatedEvent(book);
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return kafkaTemplate.send(BOOK_EVENTS_TOPIC, book.getBookId(), event)
            .whenComplete((result, ex) -> {
                sample.stop(Timer.builder("event.publish.duration")
                    .tag("event_type", "BookCreated")
                    .tag("status", ex == null ? "success" : "failure")
                    .register(meterRegistry));
                
                if (ex == null) {
                    log.info("Published BookCreatedEvent: bookId={}, partition={}, offset={}",
                        book.getBookId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                    
                    meterRegistry.counter("events.published",
                        "event_type", "BookCreated",
                        "status", "success"
                    ).increment();
                } else {
                    log.error("Failed to publish BookCreatedEvent: bookId={}, error={}",
                        book.getBookId(), ex.getMessage(), ex);
                    
                    meterRegistry.counter("events.published",
                        "event_type", "BookCreated",
                        "status", "failure"
                    ).increment();
                }
            });
    }
    
    private BookCreatedEvent buildBookCreatedEvent(Book book) {
        return BookCreatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("BookCreated")
            .version(1)
            .timestamp(Instant.now())
            .correlationId(MDC.get("correlationId"))
            .source("novelist-service")
            .bookId(book.getBookId())
            .title(book.getTitle())
            .author(book.getAuthor())
            .content(book.getContent())
            .build();
    }
}
```

## Consumer Configuration

### Kafka Consumer (Spring Boot)

```java
@Configuration
public class KafkaConsumerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "novelist-consumer-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Reliability settings
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);  // Manual commit
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);  // 5 minutes
        
        // Deserialization settings
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.prince.novelist.event");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.prince.novelist.event.BaseEvent");
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> 
            kafkaListenerContainerFactory() {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);  // 3 consumer threads
        factory.getContainerProperties().setAckMode(AckMode.MANUAL);
        
        // Error handling
        factory.setCommonErrorHandler(
            new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate()),
                new FixedBackOff(1000L, 3L)  // 3 retries with 1s delay
            )
        );
        
        return factory;
    }
}
```

### Event Consumer Service

```java
@Service
@Slf4j
public class EmbeddingEventConsumer {
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private ProcessedEventRepository processedEventRepo;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @KafkaListener(
        topics = "novelist.embedding.events",
        groupId = "novelist-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEmbeddingEvent(
            ConsumerRecord<String, EmbeddingGeneratedEvent> record,
            Acknowledgment acknowledgment) {
        
        EmbeddingGeneratedEvent event = record.value();
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Check idempotency
            if (processedEventRepo.existsByEventId(event.getEventId())) {
                log.info("Event already processed: {}", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }
            
            // Process event
            log.info("Processing EmbeddingGeneratedEvent: bookId={}, embeddingId={}",
                event.getBookId(), event.getEmbeddingId());
            
            bookService.updateEmbeddingMetadata(
                event.getBookId(),
                event.getEmbeddingId(),
                event.getVectorDimensions(),
                event.getModel()
            );
            
            // Mark as processed
            processedEventRepo.save(new ProcessedEvent(event.getEventId()));
            
            // Acknowledge
            acknowledgment.acknowledge();
            
            // Metrics
            meterRegistry.counter("events.consumed",
                "event_type", "EmbeddingGenerated",
                "status", "success"
            ).increment();
            
            log.info("Successfully processed EmbeddingGeneratedEvent: bookId={}",
                event.getBookId());
            
        } catch (Exception e) {
            log.error("Failed to process EmbeddingGeneratedEvent: bookId={}, error={}",
                event.getBookId(), e.getMessage(), e);
            
            meterRegistry.counter("events.consumed",
                "event_type", "EmbeddingGenerated",
                "status", "failure"
            ).increment();
            
            throw e;  // Will trigger retry and eventually DLQ
            
        } finally {
            sample.stop(Timer.builder("event.consume.duration")
                .tag("event_type", "EmbeddingGenerated")
                .register(meterRegistry));
        }
    }
}
```

## Message Ordering

### Kafka Partitioning Strategy

```java
@Service
public class BookEventPublisher {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishBookEvent(BookEvent event) {
        // Use bookId as partition key to ensure ordering
        // All events for the same book go to the same partition
        kafkaTemplate.send(
            "novelist.book.events",
            event.getBookId(),  // Partition key
            event
        );
    }
}
```

### Custom Partitioner

```java
public class BookIdPartitioner implements Partitioner {
    
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();
        
        if (key == null) {
            return ThreadLocalRandom.current().nextInt(numPartitions);
        }
        
        // Hash bookId to partition
        return Math.abs(key.hashCode()) % numPartitions;
    }
    
    @Override
    public void close() {}
    
    @Override
    public void configure(Map<String, ?> configs) {}
}
```

## Idempotency

### Processed Events Tracking

```java
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    
    @Id
    private String eventId;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false)
    private Instant processedAt;
    
    @Column
    private String processingResult;
    
    // Constructors, getters, setters
}

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
    boolean existsByEventId(String eventId);
}
```

### Idempotent Event Handler

```java
@Service
public class IdempotentEventHandler {
    
    @Autowired
    private ProcessedEventRepository processedEventRepo;
    
    @Transactional
    public void handleEvent(BaseEvent event, Consumer<BaseEvent> handler) {
        // Check if already processed
        if (processedEventRepo.existsByEventId(event.getEventId())) {
            log.info("Event already processed: {}", event.getEventId());
            return;
        }
        
        try {
            // Process event
            handler.accept(event);
            
            // Mark as processed
            ProcessedEvent processedEvent = new ProcessedEvent();
            processedEvent.setEventId(event.getEventId());
            processedEvent.setEventType(event.getEventType());
            processedEvent.setProcessedAt(Instant.now());
            processedEvent.setProcessingResult("SUCCESS");
            
            processedEventRepo.save(processedEvent);
            
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getEventId(), e);
            
            // Mark as failed
            ProcessedEvent processedEvent = new ProcessedEvent();
            processedEvent.setEventId(event.getEventId());
            processedEvent.setEventType(event.getEventType());
            processedEvent.setProcessedAt(Instant.now());
            processedEvent.setProcessingResult("FAILED: " + e.getMessage());
            
            processedEventRepo.save(processedEvent);
            
            throw e;
        }
    }
}
```

## Dead Letter Queues

### Kafka DLQ Configuration

```java
@Configuration
public class KafkaDLQConfig {
    
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, Object> kafkaTemplate) {
        
        return new DeadLetterPublishingRecoverer(kafkaTemplate,
            (record, ex) -> {
                // Send to DLQ topic
                return new TopicPartition("novelist.dlq", -1);
            });
    }
    
    @Bean
    public DefaultErrorHandler errorHandler(
            DeadLetterPublishingRecoverer recoverer) {
        
        // Retry 3 times with exponential backoff
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);
        
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        
        // Don't retry for certain exceptions
        handler.addNotRetryableExceptions(
            InvalidRequestException.class,
            JsonProcessingException.class
        );
        
        return handler;
    }
}
```

### DLQ Consumer for Manual Processing

```java
@Service
@Slf4j
public class DLQConsumer {
    
    @KafkaListener(topics = "novelist.dlq", groupId = "dlq-processor")
    public void processDLQMessage(ConsumerRecord<String, Object> record) {
        log.error("Processing DLQ message: topic={}, partition={}, offset={}, key={}",
            record.topic(), record.partition(), record.offset(), record.key());
        
        // Log for manual investigation
        // Could also send alerts, store in database, etc.
        
        // Example: Store in database for manual retry
        DLQMessage dlqMessage = new DLQMessage();
        dlqMessage.setOriginalTopic(record.topic());
        dlqMessage.setKey(record.key());
        dlqMessage.setValue(record.value().toString());
        dlqMessage.setReceivedAt(Instant.now());
        
        dlqMessageRepository.save(dlqMessage);
    }
}
```

## Monitoring and Observability

### Metrics

```java
@Component
public class MessageQueueMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public MessageQueueMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Register custom metrics
        Gauge.builder("kafka.consumer.lag", this, MessageQueueMetrics::getConsumerLag)
            .tag("group", "novelist-consumer-group")
            .register(meterRegistry);
    }
    
    public void recordMessagePublished(String eventType, boolean success) {
        meterRegistry.counter("messages.published",
            "event_type", eventType,
            "status", success ? "success" : "failure"
        ).increment();
    }
    
    public void recordMessageConsumed(String eventType, long durationMs) {
        meterRegistry.timer("messages.consumed",
            "event_type", eventType
        ).record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordDLQMessage(String eventType) {
        meterRegistry.counter("messages.dlq",
            "event_type", eventType
        ).increment();
    }
    
    private double getConsumerLag() {
        // Implementation to get consumer lag from Kafka
        return 0.0;
    }
}
```

### Logging

```java
@Aspect
@Component
@Slf4j
public class MessageLoggingAspect {
    
    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public Object logMessageConsumption(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        ConsumerRecord<?, ?> record = (ConsumerRecord<?, ?>) args[0];
        
        String correlationId = extractCorrelationId(record);
        MDC.put("correlationId", correlationId);
        
        log.info("Consuming message: topic={}, partition={}, offset={}, key={}",
            record.topic(), record.partition(), record.offset(), record.key());
        
        try {
            Object result = joinPoint.proceed();
            log.info("Successfully consumed message: topic={}, key={}",
                record.topic(), record.key());
            return result;
        } catch (Exception e) {
            log.error("Failed to consume message: topic={}, key={}, error={}",
                record.topic(), record.key(), e.getMessage(), e);
            throw e;
        } finally {
            MDC.remove("correlationId");
        }
    }
    
    private String extractCorrelationId(ConsumerRecord<?, ?> record) {
        // Extract from headers or generate new one
        return UUID.randomUUID().toString();
    }
}
```

## Security

### Kafka Security Configuration

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      sasl.jaas.config: |
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${KAFKA_USERNAME}"
        password="${KAFKA_PASSWORD}";
      ssl.endpoint.identification.algorithm: https
```

### RabbitMQ Security Configuration

```yaml
# application.yml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: 5671
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    ssl:
      enabled: true
      verify-hostname: true
```

### Message Encryption

```java
@Component
public class MessageEncryption {
    
    @Value("${encryption.key}")
    private String encryptionKey;
    
    public String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(
            encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }
    
    public String decrypt(String encryptedText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(
            encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        
        byte[] decrypted = cipher.doFinal(
            Base64.getDecoder().decode(encryptedText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
```

## Performance Tuning

### Producer Tuning

```yaml
# High throughput configuration
spring:
  kafka:
    producer:
      batch-size: 32768  # 32KB
      linger-ms: 20
      buffer-memory: 67108864  # 64MB
      compression-type: snappy
      acks: 1  # Leader acknowledgment only (less reliable but faster)
```

### Consumer Tuning

```yaml
# High throughput configuration
spring:
  kafka:
    consumer:
      max-poll-records: 500
      fetch-min-size: 1048576  # 1MB
      fetch-max-wait: 500
    listener:
      concurrency: 5  # 5 consumer threads
```

### Batch Processing

```java
@Service
public class BatchEventProcessor {
    
    @KafkaListener(topics = "novelist.book.events")
    public void processBatch(List<BookEvent> events) {
        log.info("Processing batch of {} events", events.size());
        
        // Process events in batch for better performance
        List<Book> books = events.stream()
            .map(this::convertToBook)
            .collect(Collectors.toList());
        
        bookRepository.saveAll(books);
    }
}
```

## Best Practices

1. **Use Correlation IDs**: Track message flow across services
2. **Implement Idempotency**: Ensure handlers can process same message multiple times
3. **Monitor Consumer Lag**: Alert when lag exceeds threshold
4. **Use Dead Letter Queues**: Handle failed messages gracefully
5. **Version Events**: Support schema evolution
6. **Encrypt Sensitive Data**: Protect PII in messages
7. **Set Appropriate TTLs**: Prevent message accumulation
8. **Test Failure Scenarios**: Verify retry and DLQ behavior
9. **Use Structured Logging**: Include correlation IDs and context
10. **Monitor Message Queue Health**: Track throughput, latency, errors

## References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/reference/)
- [Spring AMQP Documentation](https://docs.spring.io/spring-amqp/reference/)
- [Event-Driven Architecture Patterns](https://martinfowler.com/articles/201701-event-driven.html)

---

**Document Version**: 1.0  
**Last Updated**: 2026-06-18  
**Author**: Bob (AI Assistant)  
**Status**: Draft