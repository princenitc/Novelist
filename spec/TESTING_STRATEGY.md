# Testing Strategy

## Table of Contents
- [Overview](#overview)
- [Testing Pyramid](#testing-pyramid)
- [Unit Testing](#unit-testing)
- [Integration Testing](#integration-testing)
- [Contract Testing](#contract-testing)
- [End-to-End Testing](#end-to-end-testing)
- [Performance Testing](#performance-testing)
- [Security Testing](#security-testing)
- [Test Coverage Targets](#test-coverage-targets)
- [Testing Tools and Frameworks](#testing-tools-and-frameworks)
- [CI/CD Integration](#cicd-integration)
- [Best Practices](#best-practices)

## Overview

This document defines the comprehensive testing strategy for the Novelist application, covering all aspects from unit tests to end-to-end testing, including message queue testing and RAG service integration.

### Testing Goals

1. **Quality Assurance**: Ensure code meets functional requirements
2. **Regression Prevention**: Catch bugs before production
3. **Documentation**: Tests serve as living documentation
4. **Confidence**: Enable safe refactoring and deployments
5. **Performance**: Validate system performance under load

### Testing Principles

- **Test Early, Test Often**: Shift-left testing approach
- **Automate Everything**: Minimize manual testing
- **Fast Feedback**: Tests should run quickly
- **Isolation**: Tests should be independent
- **Maintainability**: Tests should be easy to understand and maintain

## Testing Pyramid

```
        /\
       /  \
      / E2E \
     /--------\
    /          \
   / Integration \
  /--------------\
 /                \
/   Unit Tests     \
--------------------
```

### Distribution

- **Unit Tests**: 70% (Fast, isolated, numerous)
- **Integration Tests**: 20% (Medium speed, component interaction)
- **E2E Tests**: 10% (Slow, full system, critical paths)

## Unit Testing

### Scope

Test individual components in isolation:
- Service layer methods
- Utility functions
- Mappers
- Validators
- Business logic

### Framework

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

### Example: Service Unit Test

```java
package com.prince.novelist.service;

import com.prince.novelist.dto.request.CreateBookRequest;
import com.prince.novelist.exception.ResourceNotFoundException;
import com.prince.novelist.mapper.BookMapper;
import com.prince.novelist.model.Book;
import com.prince.novelist.publisher.BookEventPublisher;
import com.prince.novelist.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Unit Tests")
class BookServiceTest {
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private BookMapper bookMapper;
    
    @Mock
    private BookEventPublisher eventPublisher;
    
    @InjectMocks
    private BookService bookService;
    
    private Book testBook;
    private CreateBookRequest createRequest;
    
    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setBookId(UUID.randomUUID().toString());
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        
        createRequest = CreateBookRequest.builder()
            .title("Test Book")
            .author("Test Author")
            .build();
    }
    
    @Test
    @DisplayName("Should create book successfully")
    void shouldCreateBookSuccessfully() {
        // Given
        when(bookMapper.toEntity(createRequest)).thenReturn(testBook);
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        // When
        Book result = bookService.createBook(createRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Book");
        assertThat(result.getAuthor()).isEqualTo("Test Author");
        
        verify(bookRepository).save(any(Book.class));
        verify(eventPublisher).publishBookCreated(any(Book.class));
    }
    
    @Test
    @DisplayName("Should throw exception when book not found")
    void shouldThrowExceptionWhenBookNotFound() {
        // Given
        String bookId = "non-existent-id";
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> bookService.getBookById(bookId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Book not found");
        
        verify(bookRepository).findById(bookId);
    }
    
    @Test
    @DisplayName("Should update book successfully")
    void shouldUpdateBookSuccessfully() {
        // Given
        String bookId = testBook.getBookId();
        UpdateBookRequest updateRequest = UpdateBookRequest.builder()
            .title("Updated Title")
            .build();
        
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        // When
        Book result = bookService.updateBook(bookId, updateRequest);
        
        // Then
        assertThat(result).isNotNull();
        verify(bookMapper).updateEntityFromRequest(updateRequest, testBook);
        verify(bookRepository).save(testBook);
        verify(eventPublisher).publishBookUpdated(any(Book.class));
    }
    
    @Test
    @DisplayName("Should delete book successfully")
    void shouldDeleteBookSuccessfully() {
        // Given
        String bookId = testBook.getBookId();
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(testBook));
        
        // When
        bookService.deleteBook(bookId);
        
        // Then
        verify(bookRepository).delete(testBook);
        verify(eventPublisher).publishBookDeleted(bookId);
    }
}
```

### Example: Mapper Unit Test

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("BookMapper Unit Tests")
class BookMapperTest {
    
    private BookMapper bookMapper = Mappers.getMapper(BookMapper.class);
    
    @Test
    @DisplayName("Should map CreateBookRequest to Book entity")
    void shouldMapRequestToEntity() {
        // Given
        CreateBookRequest request = CreateBookRequest.builder()
            .title("Test Book")
            .author("Test Author")
            .isbn("978-0-123456-78-9")
            .build();
        
        // When
        Book book = bookMapper.toEntity(request);
        
        // Then
        assertThat(book).isNotNull();
        assertThat(book.getTitle()).isEqualTo(request.getTitle());
        assertThat(book.getAuthor()).isEqualTo(request.getAuthor());
        assertThat(book.getIsbn()).isEqualTo(request.getIsbn());
        assertThat(book.getBookId()).isNull(); // Should not be set
    }
    
    @Test
    @DisplayName("Should map Book entity to BookResponse")
    void shouldMapEntityToResponse() {
        // Given
        Book book = new Book();
        book.setBookId("123");
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        
        // When
        BookResponse response = bookMapper.toResponse(book);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBookId()).isEqualTo(book.getBookId());
        assertThat(response.getTitle()).isEqualTo(book.getTitle());
        assertThat(response.getAuthor()).isEqualTo(book.getAuthor());
    }
}
```

### Unit Test Best Practices

1. **Use Descriptive Names**: `shouldCreateBookSuccessfully()`
2. **Follow AAA Pattern**: Arrange, Act, Assert
3. **One Assertion Per Test**: Focus on single behavior
4. **Use AssertJ**: Fluent assertions for readability
5. **Mock External Dependencies**: Isolate unit under test
6. **Test Edge Cases**: Null values, empty lists, boundaries

## Integration Testing

### Scope

Test component interactions:
- Controller → Service → Repository
- Database operations
- Message queue integration
- External API calls

### Framework: Testcontainers

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>neo4j</artifactId>
    <version>1.20.4</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>1.20.4</version>
    <scope>test</scope>
</dependency>
```

### Example: Repository Integration Test

```java
@SpringBootTest
@Testcontainers
@DisplayName("BookRepository Integration Tests")
class BookRepositoryIntegrationTest {
    
    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5.15.0")
        .withAdminPassword("testpassword");
    
    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "testpassword");
    }
    
    @Autowired
    private BookRepository bookRepository;
    
    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }
    
    @Test
    @DisplayName("Should save and retrieve book")
    void shouldSaveAndRetrieveBook() {
        // Given
        Book book = new Book();
        book.setBookId(UUID.randomUUID().toString());
        book.setTitle("Integration Test Book");
        book.setAuthor("Test Author");
        
        // When
        Book saved = bookRepository.save(book);
        Optional<Book> retrieved = bookRepository.findById(saved.getBookId());
        
        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTitle()).isEqualTo("Integration Test Book");
    }
    
    @Test
    @DisplayName("Should find books by title")
    void shouldFindBooksByTitle() {
        // Given
        Book book1 = createBook("Java Programming", "Author 1");
        Book book2 = createBook("Java Advanced", "Author 2");
        Book book3 = createBook("Python Basics", "Author 3");
        
        bookRepository.saveAll(List.of(book1, book2, book3));
        
        // When
        List<Book> javaBooks = bookRepository.findByTitleContaining("Java");
        
        // Then
        assertThat(javaBooks).hasSize(2);
        assertThat(javaBooks).extracting(Book::getTitle)
            .containsExactlyInAnyOrder("Java Programming", "Java Advanced");
    }
    
    private Book createBook(String title, String author) {
        Book book = new Book();
        book.setBookId(UUID.randomUUID().toString());
        book.setTitle(title);
        book.setAuthor(author);
        return book;
    }
}
```

### Example: API Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Book API Integration Tests")
class BookApiIntegrationTest {
    
    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5.15.0")
        .withAdminPassword("testpassword");
    
    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "testpassword");
    }
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private BookRepository bookRepository;
    
    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }
    
    @Test
    @DisplayName("Should create book via API")
    void shouldCreateBookViaApi() {
        // Given
        CreateBookRequest request = CreateBookRequest.builder()
            .title("API Test Book")
            .author("API Test Author")
            .build();
        
        // When
        ResponseEntity<BookResponse> response = restTemplate.postForEntity(
            "/api/v1/books",
            request,
            BookResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("API Test Book");
        
        // Verify in database
        Optional<Book> saved = bookRepository.findById(response.getBody().getBookId());
        assertThat(saved).isPresent();
    }
    
    @Test
    @DisplayName("Should return 404 for non-existent book")
    void shouldReturn404ForNonExistentBook() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/books/non-existent-id",
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
```

### Example: Message Queue Integration Test

```java
@SpringBootTest
@Testcontainers
@DisplayName("Message Queue Integration Tests")
class MessageQueueIntegrationTest {
    
    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );
    
    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }
    
    @Autowired
    private BookEventPublisher eventPublisher;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Test
    @DisplayName("Should publish and consume book created event")
    void shouldPublishAndConsumeBookCreatedEvent() throws Exception {
        // Given
        Book book = new Book();
        book.setBookId("test-123");
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<BookCreatedEvent> receivedEvent = new AtomicReference<>();
        
        // Setup consumer
        kafkaTemplate.setConsumerFactory(consumerFactory());
        
        // When
        eventPublisher.publishBookCreated(book);
        
        // Then
        boolean messageReceived = latch.await(10, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();
        assertThat(receivedEvent.get()).isNotNull();
        assertThat(receivedEvent.get().getBookId()).isEqualTo("test-123");
    }
}
```

## Contract Testing

### Purpose

Verify service contracts between Novelist service and RAG service.

### Framework: Spring Cloud Contract

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-contract-verifier</artifactId>
    <scope>test</scope>
</dependency>
```

### Example: Contract Definition

```groovy
// src/test/resources/contracts/bookCreatedEvent.groovy
package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should publish BookCreatedEvent when book is created"
    
    input {
        triggeredBy('publishBookCreatedEvent()')
    }
    
    outputMessage {
        sentTo('book-events')
        body([
            eventId: $(anyUuid()),
            eventType: 'BookCreated',
            version: 1,
            timestamp: $(anyIso8601WithOffset()),
            bookId: $(anyUuid()),
            title: 'Test Book',
            author: 'Test Author',
            content: 'Test content for embedding'
        ])
        headers {
            messagingContentType(applicationJson())
        }
    }
}
```

### Example: Contract Test

```java
@SpringBootTest
@AutoConfigureMessageVerifier
class BookEventContractTest {
    
    @Autowired
    private BookEventPublisher eventPublisher;
    
    public void publishBookCreatedEvent() {
        Book book = new Book();
        book.setBookId(UUID.randomUUID().toString());
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setContent("Test content for embedding");
        
        eventPublisher.publishBookCreated(book);
    }
}
```

## End-to-End Testing

### Scope

Test complete user workflows:
- User registration → Book creation → Rating → Search
- Book creation → Embedding generation → Semantic search
- User preferences → Personalized recommendations

### Framework: REST Assured

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

### Example: E2E Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@DisplayName("End-to-End Tests")
class EndToEndTest {
    
    private static final String BASE_URL = "http://localhost:8081/api/v1";
    
    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5.15.0")
        .withAdminPassword("testpassword");
    
    @Test
    @DisplayName("Complete book lifecycle workflow")
    void completeBookLifecycleWorkflow() {
        // 1. Create a book
        String bookId = given()
            .contentType(ContentType.JSON)
            .body(new CreateBookRequest("E2E Test Book", "E2E Author"))
        .when()
            .post(BASE_URL + "/books")
        .then()
            .statusCode(201)
            .extract()
            .path("bookId");
        
        // 2. Retrieve the book
        given()
            .pathParam("bookId", bookId)
        .when()
            .get(BASE_URL + "/books/{bookId}")
        .then()
            .statusCode(200)
            .body("title", equalTo("E2E Test Book"));
        
        // 3. Create a user
        String userId = given()
            .contentType(ContentType.JSON)
            .body(new CreateUserRequest("E2E User", 25))
        .when()
            .post(BASE_URL + "/users")
        .then()
            .statusCode(201)
            .extract()
            .path("userId");
        
        // 4. User rates the book
        given()
            .contentType(ContentType.JSON)
            .pathParam("userId", userId)
            .body(new AddRatingRequest(userId, bookId, 5))
        .when()
            .post(BASE_URL + "/users/{userId}/reviews")
        .then()
            .statusCode(201);
        
        // 5. Verify rating
        given()
            .pathParam("userId", userId)
        .when()
            .get(BASE_URL + "/users/{userId}")
        .then()
            .statusCode(200)
            .body("ratedBooks.size()", equalTo(1))
            .body("ratedBooks[0].rating", equalTo(5));
        
        // 6. Delete book
        given()
            .pathParam("bookId", bookId)
        .when()
            .delete(BASE_URL + "/books/{bookId}")
        .then()
            .statusCode(204);
    }
}
```

## Performance Testing

### Tools

- **JMeter**: Load testing
- **Gatling**: Scala-based performance testing
- **K6**: Modern load testing tool

### Example: Gatling Test

```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class BookApiPerformanceTest extends Simulation {
  
  val httpProtocol = http
    .baseUrl("http://localhost:8081")
    .acceptHeader("application/json")
  
  val scn = scenario("Book API Load Test")
    .exec(http("Get All Books")
      .get("/api/v1/books")
      .check(status.is(200)))
    .pause(1)
    .exec(http("Get Book By ID")
      .get("/api/v1/books/${bookId}")
      .check(status.is(200)))
  
  setUp(
    scn.inject(
      rampUsers(100) during (30 seconds),
      constantUsersPerSec(50) during (60 seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.max.lt(1000),
     global.successfulRequests.percent.gt(95)
   )
}
```

### Performance Targets

| Metric | Target | Critical |
|--------|--------|----------|
| Response Time (p95) | <200ms | <500ms |
| Response Time (p99) | <500ms | <1000ms |
| Throughput | >1000 req/s | >500 req/s |
| Error Rate | <1% | <5% |
| CPU Usage | <70% | <90% |
| Memory Usage | <80% | <95% |

## Security Testing

### Static Analysis

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>8.4.0</version>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Penetration Testing

```java
@SpringBootTest
@DisplayName("Security Tests")
class SecurityTest {
    
    @Test
    @DisplayName("Should prevent SQL injection")
    void shouldPreventSqlInjection() {
        String maliciousInput = "'; DROP TABLE books; --";
        
        // Attempt injection
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/books?title=" + maliciousInput,
            String.class
        );
        
        // Should handle safely
        assertThat(response.getStatusCode()).isIn(
            HttpStatus.OK, HttpStatus.BAD_REQUEST
        );
    }
    
    @Test
    @DisplayName("Should enforce authentication")
    void shouldEnforceAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/users/123",
            String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
```

## Test Coverage Targets

### Overall Targets

- **Line Coverage**: >80%
- **Branch Coverage**: >75%
- **Method Coverage**: >85%

### Component-Specific Targets

| Component | Line Coverage | Branch Coverage |
|-----------|---------------|-----------------|
| Service Layer | >90% | >85% |
| Repository Layer | >85% | >80% |
| Controller Layer | >80% | >75% |
| Utility Classes | >95% | >90% |
| Mappers | >90% | >85% |

### Coverage Tool

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Testing Tools and Frameworks

### Core Testing Stack

```xml
<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- AssertJ -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>

<!-- REST Assured -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

## CI/CD Integration

### GitHub Actions Workflow

```yaml
name: Test Suite

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
      
      - name: Run Unit Tests
        run: mvn test -Dtest=*Test
      
      - name: Run Integration Tests
        run: mvn test -Dtest=*IntegrationTest
      
      - name: Generate Coverage Report
        run: mvn jacoco:report
      
      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: ./target/site/jacoco/jacoco.xml
      
      - name: Check Coverage Threshold
        run: mvn jacoco:check
```

## Best Practices

### 1. Test Naming Convention

```java
// Pattern: should[ExpectedBehavior]When[StateUnderTest]
@Test
void shouldReturnBookWhenValidIdProvided() { }

@Test
void shouldThrowExceptionWhenBookNotFound() { }

@Test
void shouldUpdateBookWhenValidDataProvided() { }
```

### 2. Test Data Builders

```java
public class BookTestDataBuilder {
    
    private String bookId = UUID.randomUUID().toString();
    private String title = "Default Title";
    private String author = "Default Author";
    
    public BookTestDataBuilder withBookId(String bookId) {
        this.bookId = bookId;
        return this;
    }
    
    public BookTestDataBuilder withTitle(String title) {
        this.title = title;
        return this;
    }
    
    public Book build() {
        Book book = new Book();
        book.setBookId(bookId);
        book.setTitle(title);
        book.setAuthor(author);
        return book;
    }
}

// Usage
Book book = new BookTestDataBuilder()
    .withTitle("Custom Title")
    .build();
```

### 3. Parameterized Tests

```java
@ParameterizedTest
@ValueSource(ints = {1, 2, 3, 4, 5})
@DisplayName("Should accept valid ratings")
void shouldAcceptValidRatings(int rating) {
    AddRatingRequest request = new AddRatingRequest("user-1", "book-1", rating);
    assertThat(validator.validate(request)).isEmpty();
}

@ParameterizedTest
@CsvSource({
    "0, Rating must be between 1 and 5",
    "6, Rating must be between 1 and 5",
    "-1, Rating must be between 1 and 5"
})
@DisplayName("Should reject invalid ratings")
void shouldRejectInvalidRatings(int rating, String expectedMessage) {
    AddRatingRequest request = new AddRatingRequest("user-1", "book-1", rating);
    Set<ConstraintViolation<AddRatingRequest>> violations = validator.validate(request);
    
    assertThat(violations).isNotEmpty();
    assertThat(violations.iterator().next().getMessage()).contains(expectedMessage);
}
```

### 4. Test Fixtures

```java
@TestConfiguration
public class TestFixtures {
    
    @Bean
    public Book testBook() {
        Book book = new Book();
        book.setBookId("test-book-1");
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        return book;
    }
    
    @Bean
    public User testUser() {
        User user = new User();
        user.setUserId("test-user-1");
        user.setName("Test User");
        user.setAge(25L);
        return user;
    }
}
```

### 5. Clean Up After Tests

```java
@AfterEach
void tearDown() {
    bookRepository.deleteAll();
    userRepository.deleteAll();
}

@AfterAll
static void tearDownAll() {
    // Clean up shared resources
}
```

## References

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)

---

**Document Version**: 1.0  
**Last Updated**: 2026-06-18  
**Author**: Bob (AI Assistant)  
**Status**: Draft