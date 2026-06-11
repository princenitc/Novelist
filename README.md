# Novelist - Book Rating Application

A Spring Boot 3 application using Neo4j graph database for managing books, users, and ratings with a RESTful API.

## 🚀 Features

- **Book Management**: Create, read, update, and delete books
- **User Management**: Manage user profiles
- **Rating System**: Users can rate books (1-5 stars) with relationship properties
- **RESTful API**: Clean API design with `/api/v1/` versioning
- **Input Validation**: Jakarta Bean Validation for all inputs
- **Global Error Handling**: Consistent error responses across the application
- **API Documentation**: Interactive Swagger UI
- **Docker Support**: Multi-stage Dockerfile and Docker Compose setup
- **Integration Tests**: Testcontainers for Neo4j integration testing

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- Neo4j 5.15.0 (or use Docker Compose)
- Docker & Docker Compose (optional, for containerized deployment)

## 🛠️ Technology Stack

- **Spring Boot 3.4.1**
- **Spring Data Neo4j 7**
- **Neo4j 5.15.0**
- **Jakarta Validation**
- **SpringDoc OpenAPI 3** (Swagger UI)
- **Testcontainers** (for integration testing)
- **JUnit 5** & **Mockito**

## 📦 Installation & Setup

### Option 1: Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Novelist
   ```

2. **Start Neo4j Database**
   ```bash
   # Using Docker
   docker run -d \
     --name neo4j \
     -p 7474:7474 -p 7687:7687 \
     -e NEO4J_AUTH=neo4j/thisIsPassword \
     neo4j:5.15.0
   ```

3. **Build the application**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   # Using default profile (for tests with Testcontainers)
   mvn spring-boot:run
   
   # Using dev profile (connects to local Neo4j)
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Option 2: Docker Compose

1. **Start all services**
   ```bash
   docker-compose up -d
   ```

2. **View logs**
   ```bash
   docker-compose logs -f novelist-app
   ```

3. **Stop services**
   ```bash
   docker-compose down
   ```

## 🌐 API Endpoints

Base URL: `http://localhost:8081/api/v1`

### Book Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/books` | Create a new book |
| GET | `/books` | Get all books |
| GET | `/books/{bookId}` | Get book by ID |
| PUT | `/books/{bookId}` | Update book |
| DELETE | `/books/{bookId}` | Delete book |

### User Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users` | Create a new user |
| GET | `/users` | Get all users |
| GET | `/users/{userId}` | Get user by ID |
| PUT | `/users/{userId}` | Update user |
| DELETE | `/users/{userId}` | Delete user |
| POST | `/users/{userId}/reviews` | Add a book review/rating |

## 📝 API Request/Response Examples

### Create a Book

**Request:**
```bash
curl -X POST http://localhost:8081/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "The Great Gatsby",
    "author": "F. Scott Fitzgerald"
  }'
```

**Response (201 Created):**
```json
{
  "bookId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "The Great Gatsby",
  "author": "F. Scott Fitzgerald"
}
```

### Create a User

**Request:**
```bash
curl -X POST http://localhost:8081/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "age": 30
  }'
```

**Response (201 Created):**
```json
{
  "userId": "660e8400-e29b-41d4-a716-446655440001",
  "name": "John Doe",
  "age": 30,
  "ratedBooks": []
}
```

### Add a Review/Rating

**Request:**
```bash
curl -X POST http://localhost:8081/api/v1/users/660e8400-e29b-41d4-a716-446655440001/reviews \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "660e8400-e29b-41d4-a716-446655440001",
    "bookId": "550e8400-e29b-41d4-a716-446655440000",
    "rating": 5
  }'
```

**Response (201 Created):**
```json
{
  "userId": "660e8400-e29b-41d4-a716-446655440001",
  "name": "John Doe",
  "age": 30,
  "ratedBooks": [
    {
      "book": {
        "bookId": "550e8400-e29b-41d4-a716-446655440000",
        "title": "The Great Gatsby",
        "author": "F. Scott Fitzgerald"
      },
      "rating": 5
    }
  ]
}
```

## 🧪 Manual Testing Guide

### 1. Setup Test Environment

```bash
# Start the application with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or using Docker Compose
docker-compose up -d
```

### 2. Test Book Management

#### Create Books
```bash
# Book 1
curl -X POST http://localhost:8081/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{"title": "1984", "author": "George Orwell"}'

# Book 2
curl -X POST http://localhost:8081/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{"title": "To Kill a Mockingbird", "author": "Harper Lee"}'

# Save the bookId from responses for later use
```

#### Get All Books
```bash
curl http://localhost:8081/api/v1/books
```

#### Get Book by ID
```bash
curl http://localhost:8081/api/v1/books/{bookId}
```

#### Update Book
```bash
curl -X PUT http://localhost:8081/api/v1/books/{bookId} \
  -H "Content-Type: application/json" \
  -d '{"title": "1984 (Updated)", "author": "George Orwell"}'
```

#### Delete Book
```bash
curl -X DELETE http://localhost:8081/api/v1/books/{bookId}
```

### 3. Test User Management

#### Create Users
```bash
# User 1
curl -X POST http://localhost:8081/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice Smith", "age": 28}'

# User 2
curl -X POST http://localhost:8081/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Bob Johnson", "age": 35}'

# Save the userId from responses
```

#### Get All Users
```bash
curl http://localhost:8081/api/v1/users
```

#### Get User by ID
```bash
curl http://localhost:8081/api/v1/users/{userId}
```

#### Update User
```bash
curl -X PUT http://localhost:8081/api/v1/users/{userId} \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice Smith Updated", "age": 29}'
```

### 4. Test Rating System

#### Add Multiple Ratings
```bash
# User 1 rates Book 1 (5 stars)
curl -X POST http://localhost:8081/api/v1/users/{userId}/reviews \
  -H "Content-Type: application/json" \
  -d '{"userId": "{userId}", "bookId": "{bookId}", "rating": 5}'

# User 1 rates Book 2 (4 stars)
curl -X POST http://localhost:8081/api/v1/users/{userId}/reviews \
  -H "Content-Type: application/json" \
  -d '{"userId": "{userId}", "bookId": "{bookId2}", "rating": 4}'

# User 2 rates Book 1 (3 stars)
curl -X POST http://localhost:8081/api/v1/users/{userId2}/reviews \
  -H "Content-Type: application/json" \
  -d '{"userId": "{userId2}", "bookId": "{bookId}", "rating": 3}'
```

#### Verify Ratings
```bash
# Get user with their ratings
curl http://localhost:8081/api/v1/users/{userId}
```

### 5. Test Validation

#### Invalid Book (missing required fields)
```bash
curl -X POST http://localhost:8081/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{"title": ""}'
# Expected: 400 Bad Request with validation errors
```

#### Invalid Rating (out of range)
```bash
curl -X POST http://localhost:8081/api/v1/users/{userId}/reviews \
  -H "Content-Type: application/json" \
  -d '{"userId": "{userId}", "bookId": "{bookId}", "rating": 10}'
# Expected: 400 Bad Request - rating must be between 1 and 5
```

#### Invalid User Age (negative)
```bash
curl -X POST http://localhost:8081/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Test User", "age": -5}'
# Expected: 400 Bad Request - age must be positive
```

### 6. Test Error Handling

#### Non-existent Resource
```bash
curl http://localhost:8081/api/v1/books/non-existent-id
# Expected: 404 Not Found with error message
```

#### Delete Non-existent Resource
```bash
curl -X DELETE http://localhost:8081/api/v1/users/non-existent-id
# Expected: 404 Not Found
```

### 7. Test Complete Workflow

```bash
# 1. Create a book
BOOK_RESPONSE=$(curl -s -X POST http://localhost:8081/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{"title": "The Hobbit", "author": "J.R.R. Tolkien"}')
BOOK_ID=$(echo $BOOK_RESPONSE | jq -r '.bookId')

# 2. Create a user
USER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Charlie Brown", "age": 25}')
USER_ID=$(echo $USER_RESPONSE | jq -r '.userId')

# 3. User rates the book
curl -X POST http://localhost:8081/api/v1/users/$USER_ID/reviews \
  -H "Content-Type: application/json" \
  -d "{\"userId\": \"$USER_ID\", \"bookId\": \"$BOOK_ID\", \"rating\": 5}"

# 4. Verify the rating
curl http://localhost:8081/api/v1/users/$USER_ID

# 5. Update the book
curl -X PUT http://localhost:8081/api/v1/books/$BOOK_ID \
  -H "Content-Type: application/json" \
  -d '{"title": "The Hobbit (Annotated)", "author": "J.R.R. Tolkien"}'

# 6. Delete the user (cascade deletes ratings)
curl -X DELETE http://localhost:8081/api/v1/users/$USER_ID

# 7. Delete the book
curl -X DELETE http://localhost:8081/api/v1/books/$BOOK_ID
```

## 📚 API Documentation

Access the interactive Swagger UI at:
```
http://localhost:8081/swagger-ui.html
```

OpenAPI JSON specification:
```
http://localhost:8081/api-docs
```

## 🧪 Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BookServiceTest

# Run integration tests
mvn test -Dtest=BookIntegrationTest

# Run with coverage
mvn clean test jacoco:report
```

## 🐳 Docker Commands

```bash
# Build Docker image
docker build -t novelist-app .

# Run container
docker run -p 8081:8081 \
  -e SPRING_NEO4J_URI=bolt://host.docker.internal:7687 \
  -e SPRING_NEO4J_AUTHENTICATION_USERNAME=neo4j \
  -e SPRING_NEO4J_AUTHENTICATION_PASSWORD=thisIsPassword \
  novelist-app

# Using Docker Compose
docker-compose up -d          # Start services
docker-compose logs -f        # View logs
docker-compose down           # Stop services
docker-compose down -v        # Stop and remove volumes
```

## 🔍 Neo4j Browser

Access Neo4j Browser at:
```
http://localhost:7474
```

**Credentials:**
- Username: `neo4j`
- Password: `thisIsPassword`

**Useful Cypher Queries:**
```cypher
// View all books
MATCH (b:Book) RETURN b

// View all users with their ratings
MATCH (u:User)-[r:RATED]->(b:Book) RETURN u, r, b

// View books with average ratings
MATCH (u:User)-[r:RATED]->(b:Book)
RETURN b.title, b.author, AVG(r.rating) as avgRating, COUNT(r) as numRatings
ORDER BY avgRating DESC

// Delete all data
MATCH (n) DETACH DELETE n
```

## 📁 Project Structure

```
Novelist/
├── src/
│   ├── main/
│   │   ├── java/com/prince/novelist/
│   │   │   ├── exception/          # Custom exceptions
│   │   │   ├── model/              # Domain models
│   │   │   ├── repository/         # Neo4j repositories
│   │   │   ├── resource/           # REST controllers
│   │   │   ├── service/            # Business logic
│   │   │   ├── DataConfig.java     # Configuration
│   │   │   └── NovelistApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-dev.properties
│   └── test/
│       └── java/com/prince/novelist/
│           ├── service/            # Unit & integration tests
│           └── TestSetup.java
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

## 🔧 Configuration

### Application Profiles

- **Default**: Uses Testcontainers for Neo4j (for testing)
- **dev**: Connects to local Neo4j instance

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_NEO4J_URI` | Neo4j connection URI | `bolt://localhost:7687` |
| `SPRING_NEO4J_AUTHENTICATION_USERNAME` | Neo4j username | `neo4j` |
| `SPRING_NEO4J_AUTHENTICATION_PASSWORD` | Neo4j password | `thisIsPassword` |
| `SERVER_PORT` | Application port | `8081` |

## 🐛 Troubleshooting

### Connection Issues

If you can't connect to Neo4j:
```bash
# Check if Neo4j is running
docker ps | grep neo4j

# Check Neo4j logs
docker logs neo4j

# Restart Neo4j
docker restart neo4j
```

### Port Conflicts

If port 8081 is already in use:
```bash
# Change port in application.properties
server.port=8082

# Or set environment variable
export SERVER_PORT=8082
mvn spring-boot:run
```

## 📄 License

This project is licensed under the MIT License.

## 👥 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

For issues and questions, please open an issue on GitHub.
