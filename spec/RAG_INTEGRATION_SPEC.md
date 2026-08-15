
# Novelist Service Event Handlers
class NovelistEventHandlers:
    
    @staticmethod
    async def handle_embedding_generated(event: BaseEvent):
        content_id = event.payload['contentId']
        content_type = event.payload['contentType']
        embedding_count = event.payload['embeddingCount']
        
        # Update content metadata in Neo4j
        await update_content_metadata(
            content_id=content_id,
            content_type=content_type,
            embedding_status='COMPLETED',
            embedding_count=embedding_count
        )
```

## RAG Application API Integration

### Connection Configuration

**File**: `application.yml`

```yaml
rag:
  application:
    base-url: http://rag-service:8080
    api-key: ${RAG_API_KEY}
    timeout: 30s
    connect-timeout: 10s
    retry:
      max-attempts: 3
      backoff: 2s
      multiplier: 2
    circuit-breaker:
      failure-threshold: 5
      timeout: 60s
      half-open-requests: 3
```

### REST Client Implementation

```java
@Service
public class RAGClient {
    
    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    
    public RAGClient(
        @Value("${rag.application.base-url}") String baseUrl,
        @Value("${rag.application.api-key}") String apiKey,
        CircuitBreakerFactory circuitBreakerFactory
    ) {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("X-API-Key", apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
        
        this.circuitBreaker = circuitBreakerFactory.create("ragService");
    }
    
    public Mono<EmbeddingResponse> generateEmbedding(String text) {
        return circuitBreaker.run(
            webClient.post()
                .uri("/api/v1/rag/embeddings/generate")
                .bodyValue(Map.of("text", text))
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .timeout(Duration.ofSeconds(30)),
            throwable -> fallbackEmbedding(text, throwable)
        );
    }
    
    public Flux<SearchResult> semanticSearch(SearchRequest request) {
        return circuitBreaker.run(
            webClient.post()
                .uri("/api/v1/rag/search")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(SearchResult.class)
                .timeout(Duration.ofSeconds(30)),
            throwable -> fallbackSearch(request, throwable)
        );
    }
    
    public Flux<Recommendation> getRecommendations(RecommendationRequest request) {
        return circuitBreaker.run(
            webClient.post()
                .uri("/api/v1/rag/recommendations")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(Recommendation.class)
                .timeout(Duration.ofSeconds(30)),
            throwable -> fallbackRecommendations(request, throwable)
        );
    }
    
    private Mono<EmbeddingResponse> fallbackEmbedding(String text, Throwable t) {
        log.warn("RAG service unavailable, using fallback", t);
        return Mono.just(new EmbeddingResponse(null, "FALLBACK"));
    }
    
    private Flux<SearchResult> fallbackSearch(SearchRequest request, Throwable t) {
        log.warn("RAG service unavailable, using keyword search", t);
        return performKeywordSearch(request);
    }
    
    private Flux<Recommendation> fallbackRecommendations(
        RecommendationRequest request,
        Throwable t
    ) {
        log.warn("RAG service unavailable, using basic recommendations", t);
        return performBasicRecommendations(request);
    }
}
```

### Service Integration

```java
@Service
public class BookService {
    
    private final BookRepository bookRepository;
    private final RAGClient ragClient;
    private final EventPublisher eventPublisher;
    
    @Transactional
    public Book createBook(BookDTO bookDTO) {
        // Save book to Neo4j
        Book book = bookRepository.save(bookDTO.toEntity());
        
        // Publish event for RAG processing
        eventPublisher.publishBookCreatedEvent(book);
        
        return book;
    }
    
    public Flux<SearchResult> searchBooks(String query) {
        // Use RAG service for semantic search
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .contentTypes(List.of("BOOK"))
            .topK(10)
            .build();
        
        return ragClient.semanticSearch(request);
    }
    
    public Flux<Recommendation> getRecommendations(String userId) {
        // Get user's reading history
        List<String> bookIds = getUserBookIds(userId);
        
        // Request recommendations from RAG service
        RecommendationRequest request = RecommendationRequest.builder()
            .userId(userId)
            .basedOn(Map.of("bookIds", bookIds))
            .count(10)
            .strategy("HYBRID")
            .build();
        
        return ragClient.getRecommendations(request);
    }
}
```

## Error Handling and Fallback

### Circuit Breaker Configuration

```java
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowSize(10)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build())
            .timeLimiterConfig(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(30))
                .build())
            .build());
    }
}
```

### Retry Strategy

```java
@Configuration
public class RetryConfig {
    
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Exponential backoff
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        // Retry on specific exceptions
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }
}
```

### Fallback Mechanisms

```java
@Service
public class SearchFallbackService {
    
    private final Neo4jClient neo4jClient;
    
    /**
     * Fallback to keyword search when RAG service is unavailable
     */
    public Flux<SearchResult> keywordSearch(String query) {
        String cypherQuery = """
            MATCH (b:Book)
            WHERE b.title CONTAINS $query 
               OR b.author CONTAINS $query
               OR b.description CONTAINS $query
            RETURN b
            ORDER BY b.rating DESC
            LIMIT 10
            """;
        
        return neo4jClient.query(cypherQuery)
            .bind(query).to("query")
            .fetchAs(SearchResult.class)
            .mappedBy((typeSystem, record) -> {
                Node book = record.get("b").asNode();
                return SearchResult.fromNode(book);
            })
            .all();
    }
    
    /**
     * Fallback to rating-based recommendations
     */
    public Flux<Recommendation> basicRecommendations(String userId) {
        String cypherQuery = """
            MATCH (u:User {userId: $userId})-[r:RATED]->(b:Book)
            WHERE r.rating >= 4
            MATCH (b)-[:SIMILAR_TO]->(rec:Book)
            WHERE NOT EXISTS((u)-[:RATED]->(rec))
            RETURN rec
            ORDER BY rec.rating DESC
            LIMIT 10
            """;
        
        return neo4jClient.query(cypherQuery)
            .bind(userId).to("userId")
            .fetchAs(Recommendation.class)
            .mappedBy((typeSystem, record) -> {
                Node book = record.get("rec").asNode();
                return Recommendation.fromNode(book);
            })
            .all();
    }
}
```

### Dead Letter Queue Handling

```python
class DeadLetterQueueHandler:
    """
    Handle failed events
    """
    def __init__(self, kafka_producer):
        self.producer = kafka_producer
    
    async def send_to_dlq(
        self,
        original_event: BaseEvent,
        error: Exception,
        retry_count: int
    ):
        dlq_event = {
            'originalEvent': original_event.to_dict(),
            'error': {
                'type': type(error).__name__,
                'message': str(error),
                'traceback': traceback.format_exc()
            },
            'retryCount': retry_count,
            'timestamp': datetime.utcnow().isoformat(),
            'canRetry': is_retryable_error(error)
        }
        
        await self.producer.send(
            topic='dlq-events',
            key=original_event.event_id,
            value=json.dumps(dlq_event)
        )
    
    async def process_dlq(self):
        """
        Process events from DLQ for manual intervention
        """
        async for message in self.dlq_consumer:
            dlq_event = json.loads(message.value)
            
            if dlq_event['canRetry']:
                # Attempt to reprocess
                await self.retry_event(dlq_event['originalEvent'])
            else:
                # Log for manual intervention
                logger.error(
                    f"Non-retryable error in DLQ: {dlq_event}"
                )
```

## Performance Optimization

### Caching Strategy

```python
from functools import lru_cache
from cachetools import TTLCache
import asyncio

class EmbeddingCache:
    """
    Cache embeddings to reduce API calls
    """
    def __init__(self, max_size=10000, ttl=3600):
        self.cache = TTLCache(maxsize=max_size, ttl=ttl)
        self.lock = asyncio.Lock()
    
    async def get_embedding(self, text: str) -> Optional[List[float]]:
        cache_key = hashlib.md5(text.encode()).hexdigest()
        
        async with self.lock:
            return self.cache.get(cache_key)
    
    async def set_embedding(self, text: str, embedding: List[float]):
        cache_key = hashlib.md5(text.encode()).hexdigest()
        
        async with self.lock:
            self.cache[cache_key] = embedding
    
    async def get_or_generate(
        self,
        text: str,
        generator: Callable
    ) -> List[float]:
        # Check cache first
        cached = await self.get_embedding(text)
        if cached:
            return cached
        
        # Generate if not cached
        embedding = await generator(text)
        await self.set_embedding(text, embedding)
        
        return embedding
```

### Batch Processing

```python
class BatchProcessor:
    """
    Process multiple documents in batches
    """
    def __init__(self, batch_size=50, max_wait_time=5):
        self.batch_size = batch_size
        self.max_wait_time = max_wait_time
        self.queue = asyncio.Queue()
        self.processing = False
    
    async def add_to_batch(self, document: dict):
        await self.queue.put(document)
        
        if not self.processing:
            asyncio.create_task(self.process_batch())
    
    async def process_batch(self):
        self.processing = True
        batch = []
        
        try:
            # Collect documents for batch
            while len(batch) < self.batch_size:
                try:
                    doc = await asyncio.wait_for(
                        self.queue.get(),
                        timeout=self.max_wait_time
                    )
                    batch.append(doc)
                except asyncio.TimeoutError:
                    break
            
            if batch:
                # Process batch
                await self.process_documents_batch(batch)
        
        finally:
            self.processing = False
            
            # Check if more documents in queue
            if not self.queue.empty():
                asyncio.create_task(self.process_batch())
    
    async def process_documents_batch(self, documents: List[dict]):
        # Extract texts
        texts = [doc['content'] for doc in documents]
        
        # Generate embeddings in batch
        embeddings = await generate_embeddings_batch(texts)
        
        # Store embeddings
        for doc, embedding in zip(documents, embeddings):
            await store_embedding(
                content_id=doc['id'],
                content_type=doc['type'],
                embedding=embedding,
                metadata=doc['metadata']
            )
```

### Connection Pooling

```python
from qdrant_client import QdrantClient
from qdrant_client.http import AsyncApis

class VectorDBConnectionPool:
    """
    Manage connection pool to vector database
    """
    def __init__(self, url: str, api_key: str, pool_size: int = 10):
        self.clients = [
            QdrantClient(url=url, api_key=api_key)
            for _ in range(pool_size)
        ]
        self.semaphore = asyncio.Semaphore(pool_size)
        self.current_index = 0
    
    async def get_client(self) -> QdrantClient:
        await self.semaphore.acquire()
        client = self.clients[self.current_index]
        self.current_index = (self.current_index + 1) % len(self.clients)
        return client
    
    def release_client(self):
        self.semaphore.release()
    
    async def execute(self, operation: Callable):
        client = await self.get_client()
        try:
            return await operation(client)
        finally:
            self.release_client()
```

### Query Optimization

```python
async def optimized_search(
    query: str,
    content_types: List[str],
    top_k: int = 10
) -> List[SearchResult]:
    """
    Optimized search with parallel queries
    """
    # Generate query embedding once
    query_embedding = await generate_embedding(query)
    
    # Search all content types in parallel
    search_tasks = [
        search_content_type(
            content_type=ct,
            query_embedding=query_embedding,
            top_k=top_k
        )
        for ct in content_types
    ]
    
    # Wait for all searches to complete
    results = await asyncio.gather(*search_tasks)
    
    # Merge and rank results
    merged_results = merge_search_results(results)
    
    return merged_results[:top_k]
```

## Security Considerations

### API Authentication

```python
from fastapi import Security, HTTPException
from fastapi.security import APIKeyHeader

api_key_header = APIKeyHeader(name="X-API-Key")

async def verify_api_key(api_key: str = Security(api_key_header)):
    """
    Verify API key for RAG service
    """
    valid_keys = get_valid_api_keys()  # From secure storage
    
    if api_key not in valid_keys:
        raise HTTPException(
            status_code=403,
            detail="Invalid API key"
        )
    
    return api_key

@app.post("/api/v1/rag/search")
async def search(
    request: SearchRequest,
    api_key: str = Depends(verify_api_key)
):
    return await perform_search(request)
```

### Data Encryption

```python
from cryptography.fernet import Fernet

class DataEncryption:
    """
    Encrypt sensitive data in embeddings
    """
    def __init__(self, encryption_key: str):
        self.cipher = Fernet(encryption_key.encode())
    
    def encrypt_metadata(self, metadata: dict) -> dict:
        """
        Encrypt sensitive fields in metadata
        """
        sensitive_fields = ['userId', 'email', 'personalInfo']
        
        encrypted_metadata = metadata.copy()
        
        for field in sensitive_fields:
            if field in encrypted_metadata:
                value = str(encrypted_metadata[field])
                encrypted_value = self.cipher.encrypt(value.encode())
                encrypted_metadata[field] = encrypted_value.decode()
        
        return encrypted_metadata
    
    def decrypt_metadata(self, metadata: dict) -> dict:
        """
        Decrypt sensitive fields
        """
        sensitive_fields = ['userId', 'email', 'personalInfo']
        
        decrypted_metadata = metadata.copy()
        
        for field in sensitive_fields:
            if field in decrypted_metadata:
                encrypted_value = decrypted_metadata[field].encode()
                decrypted_value = self.cipher.decrypt(encrypted_value)
                decrypted_metadata[field] = decrypted_value.decode()
        
        return decrypted_metadata
```

### Rate Limiting

```python
from fastapi_limiter import FastAPILimiter
from fastapi_limiter.depends import RateLimiter

@app.on_event("startup")
async def startup():
    redis = await aioredis.create_redis_pool("redis://localhost")
    await FastAPILimiter.init(redis)

@app.post("/api/v1/rag/search")
@limiter.limit("100/minute")
async def search(
    request: SearchRequest,
    api_key: str = Depends(verify_api_key)
):
    return await perform_search(request)
```

### Input Validation

```python
from pydantic import BaseModel, validator, Field

class SearchRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=1000)
    content_types: List[str] = Field(..., min_items=1, max_items=3)
    top_k: int = Field(10, ge=1, le=100)
    min_score: float = Field(0.7, ge=0.0, le=1.0)
    
    @validator('content_types')
    def validate_content_types(cls, v):
        valid_types = ['BOOK', 'REVIEW', 'ARTICLE']
        for ct in v:
            if ct not in valid_types:
                raise ValueError(f'Invalid content type: {ct}')
        return v
    
    @validator('query')
    def sanitize_query(cls, v):
        # Remove potentially harmful characters
        sanitized = re.sub(r'[<>{}]', '', v)
        return sanitized.strip()
```

### Audit Logging

```python
class AuditLogger:
    """
    Log all RAG service operations for security audit
    """
    def __init__(self, logger):
        self.logger = logger
    
    async def log_search(
        self,
        user_id: str,
        query: str,
        results_count: int,
        api_key: str
    ):
        self.logger.info(
            "SEARCH_AUDIT",
            extra={
                'userId': user_id,
                'query': query,
                'resultsCount': results_count,
                'apiKey': mask_api_key(api_key),
                'timestamp': datetime.utcnow().isoformat(),
                'ipAddress': get_client_ip()
            }
        )
    
    async def log_embedding_generation(
        self,
        content_id: str,
        content_type: str,
        token_count: int,
        cost: float
    ):
        self.logger.info(
            "EMBEDDING_AUDIT",
            extra={
                'contentId': content_id,
                'contentType': content_type,
                'tokenCount': token_count,
                'cost': cost,
                'timestamp': datetime.utcnow().isoformat()
            }
        )
```

## Testing Strategy

### Unit Tests

```python
import pytest
from unittest.mock import Mock, AsyncMock

@pytest.mark.asyncio
async def test_generate_embedding():
    # Mock OpenAI client
    mock_client = AsyncMock()
    mock_client.embeddings.create.return_value = Mock(
        data=[Mock(embedding=[0.1, 0.2, 0.3])]
    )
    
    # Test embedding generation
    embedding = await generate_embedding(
        text="Test text",
        client=mock_client
    )
    
    assert len(embedding) == 3
    assert embedding == [0.1, 0.2, 0.3]

@pytest.mark.asyncio
async def test_semantic_search():
    # Mock vector database
    mock_db = AsyncMock()
    mock_db.search.return_value = [
        {'id': '1', 'score': 0.9},
        {'id': '2', 'score': 0.8}
    ]
    
    # Test search
    results = await semantic_search(
        query="test query",
        vector_db=mock_db
    )
    
    assert len(results) == 2
    assert results[0]['score'] > results[1]['score']
```

### Integration Tests

```python
@pytest.mark.integration
@pytest.mark.asyncio
async def test_end_to_end_ingestion():
    # Create test book
    book = {
        'bookId': 'test-123',
        'title': 'Test Book',
        'content': 'Test content for embedding'
    }
    
    # Publish event
    await event_publisher.publish_book_created(book)
    
    # Wait for processing
    await asyncio.sleep(5)
    
    # Verify embedding was created
    embedding = await vector_db.get_embedding('test-123')
    assert embedding is not None
    assert len(embedding) == 1536
```

### Performance Tests

```python
@pytest.mark.performance
@pytest.mark.asyncio
async def test_search_performance():
    # Generate test data
    test_queries = generate_test_queries(100)
    
    # Measure search time
    start_time = time.time()
    
    tasks = [
        semantic_search(query)
        for query in test_queries
    ]
    
    results = await asyncio.gather(*tasks)
    
    end_time = time.time()
    avg_time = (end_time - start_time) / len(test_queries)
    
    # Assert performance requirements
    assert avg_time < 0.5  # Less than 500ms per search
```

## Monitoring and Metrics

### Custom Metrics

```python
from prometheus_client import Counter, Histogram, Gauge

# Embedding metrics
embedding_requests = Counter(
    'rag_embedding_requests_total',
    'Total embedding generation requests',
    ['content_type', 'status']
)

embedding_duration = Histogram(
    'rag_embedding_duration_seconds',
    'Embedding generation duration',
    ['content_type']
)

embedding_tokens = Counter(
    'rag_embedding_tokens_total',
    'Total tokens processed',
    ['content_type']
)

# Search metrics
search_requests = Counter(
    'rag_search_requests_total',
    'Total search requests',
    ['content_type']
)

search_duration = Histogram(
    'rag_search_duration_seconds',
    'Search duration',
    ['content_type']
)

search_results = Histogram(
    'rag_search_results_count',
    'Number of search results',
    ['content_type']
)

# Vector database metrics
vector_db_size = Gauge(
    'rag_vector_db_size',
    'Total vectors in database',
    ['collection']
)
```

### Health Checks

```python
@app.get("/health")
async def health_check():
    """
    Comprehensive health check
    """
    health_status = {
        'status': 'healthy',
        'timestamp': datetime.utcnow().isoformat(),
        'components': {}
    }
    
    # Check vector database
    try:
        await vector_db.health_check()
        health_status['components']['vectorDB'] = 'healthy'
    except Exception as e:
        health_status['components']['vectorDB'] = 'unhealthy'
        health_status['status'] = 'degraded'
    
    # Check OpenAI API
    try:
        await openai_client.models.list()
        health_status['components']['openai'] = 'healthy'
    except Exception as e:
        health_status['components']['openai'] = 'unhealthy'
        health_status['status'] = 'degraded'
    
    # Check Kafka
    try:
        await kafka_producer.send('health-check', b'ping')
        health_status['components']['kafka'] = 'healthy'
    except Exception as e:
        health_status['components']['kafka'] = 'unhealthy'
        health_status['status'] = 'degraded'
    
    return health_status
```

## Best Practices

### 1. Idempotency

Ensure all operations are idempotent to handle duplicate events:

```python
async def process_book_idempotent(book_id: str, book_data: dict):
    # Check if already processed
    if await is_already_processed(book_id):
        logger.info(f"Book {book_id} already processed, skipping")
        return
    
    # Process book
    await process_book_content(book_data)
    
    # Mark as processed
    await mark_as_processed(book_id)
```

### 2. Graceful Degradation

Provide fallback mechanisms when RAG service is unavailable:

```python
async def search_with_fallback(query: str):
    try:
        # Try semantic search
        return await semantic_search(query)
    except Exception as e:
        logger.warning(f"Semantic search failed: {e}")
        # Fallback to keyword search
        return await keyword_search(query)
```

### 3. Monitoring and Alerting

Set up comprehensive monitoring:

```python
# Alert on high error rate
if error_rate > 0.05:
    send_alert("High error rate in RAG service")

# Alert on high latency
if avg_latency > 1.0:
    send_alert("High latency in RAG service")

# Alert on low throughput
if throughput < 10:
    send_alert("Low throughput in RAG service")
```

### 4. Cost Optimization

Monitor and optimize embedding generation costs:

```python
class CostOptimizer:
    def __init__(self):
        self.daily_budget = 100.0  # $100 per day
        self.current_cost = 0.0
    
    async def check_budget(self, estimated_cost: float) -> bool:
        if self.current_cost + estimated_cost > self.daily_budget:
            logger.warning("Daily budget exceeded")
            return False
        return True
    
    async def track_cost(self, tokens: int):
        cost = (tokens / 1000) * 0.0001
        self.current_cost += cost
```

## References

- [OpenAI Embeddings API](https://platform.openai.com/docs/guides/embeddings)
- [Qdrant Documentation](https://qdrant.tech/documentation/)
- [Pinecone Documentation](https://docs.pinecone.io/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [Neo4j Vector Indexes](https://neo4j.com/docs/cypher-manual/current/indexes-for-vector-search/)
- [RAG Application Repository](https://github.com/princenitc/rag_application)

---

**Document Version**: 1.0  
**Last Updated**: 2026-06-18  
**Author**: Bob (AI Assistant)  
**Status**: Production Ready