# API Specification

## Table of Contents
- [Overview](#overview)
- [Base URL and Versioning](#base-url-and-versioning)
- [Authentication](#authentication)
- [Common Response Formats](#common-response-formats)
- [Error Handling](#error-handling)
- [Pagination](#pagination)
- [Current Endpoints](#current-endpoints)
- [RAG-Powered Endpoints (Planned)](#rag-powered-endpoints-planned)
- [Rate Limiting](#rate-limiting)
- [API Examples](#api-examples)

## Overview

The Novelist API provides RESTful endpoints for managing books, users, ratings, and RAG-powered semantic search and recommendations. All endpoints follow REST conventions and return JSON responses.

### API Principles

1. **RESTful Design**: Resources are nouns, HTTP methods indicate actions
2. **Versioning**: API version in URL path (`/api/v1/`)
3. **Stateless**: Each request contains all necessary information
4. **JSON Format**: All requests and responses use JSON
5. **HTTP Status Codes**: Standard codes for success/error indication
6. **Pagination**: Large result sets are paginated
7. **HATEOAS**: Links to related resources (planned)

## Base URL and Versioning

### Base URL
```
http://localhost:8081/api/v1
```

### Production URL (Example)
```
https://api.novelist.com/v1
```

### Versioning Strategy

- **URL Path Versioning**: `/api/v1/`, `/api/v2/`
- **Breaking Changes**: Require new version
- **Non-Breaking Changes**: Same version with deprecation notices
- **Version Support**: Support N-1 versions (current + previous)

## Authentication

### Current: No Authentication (Development)

Currently, the API has no authentication. All endpoints are publicly accessible.

### Planned: JWT Authentication

```http
Authorization: Bearer <jwt_token>
```

**Token Structure**:
```json
{
  "sub": "user-123",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "roles": ["USER", "ADMIN"],
  "iat": 1718712000,
  "exp": 1718798400
}
```

**Authentication Endpoints** (Planned):
```
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

See [SECURITY_DESIGN.md](./SECURITY_DESIGN.md) for detailed authentication specification.

## Common Response Formats

### Success Response

```json
{
  "data": { ... },
  "metadata": {
    "timestamp": "2026-06-18T10:30:00.000Z",
    "version": "1.0"
  }
}
```

### List Response with Pagination

```json
{
  "data": [ ... ],
  "pagination": {
    "page": 1,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  },
  "metadata": {
    "timestamp": "2026-06-18T10:30:00.000Z",
    "version": "1.0"
  }
}
```

## Error Handling

### Error Response Format

```json
{
  "timestamp": "2026-06-18T10:30:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Book not found with id: abc123",
  "path": "/api/v1/books/abc123",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### HTTP Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| 200 | OK | Successful GET, PUT requests |
| 201 | Created | Successful POST requests |
| 204 | No Content | Successful DELETE requests |
| 400 | Bad Request | Invalid input, validation errors |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Resource already exists |
| 422 | Unprocessable Entity | Semantic errors |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server-side errors |
| 503 | Service Unavailable | Service temporarily unavailable |

### Validation Error Response

```json
{
  "timestamp": "2026-06-18T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/books",
  "validationErrors": [
    {
      "field": "title",
      "message": "Title is required",
      "rejectedValue": ""
    },
    {
      "field": "author",
      "message": "Author must be between 1 and 100 characters",
      "rejectedValue": null
    }
  ]
}
```

## Pagination

### Query Parameters

```
GET /api/v1/books?page=0&size=20&sort=title,asc
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 20 | Items per page (max: 100) |
| `sort` | string | - | Sort field and direction (e.g., `title,asc`) |

### Pagination Response

```json
{
  "data": [...],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false,
    "first": true,
    "last": false
  }
}
```

## Current Endpoints

### Book Endpoints

#### Create Book

```http
POST /api/v1/books
Content-Type: application/json
```

**Request Body**:
```json
{
  "title": "The Great Gatsby",
  "author": "F. Scott Fitzgerald"
}
```

**Response** (201 Created):
```json
{
  "bookId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "The Great Gatsby",
  "author": "F. Scott Fitzgerald"
}
```

**Validation Rules**:
- `title`: Required, 1-200 characters
- `author`: Required, 1-100 characters

---

#### Get All Books

```http
GET /api/v1/books
```

**Query Parameters**:
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)

**Response** (200 OK):
```json
[
  {
    "bookId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "The Great Gatsby",
    "author": "F. Scott Fitzgerald"
  },
  {
    "bookId": "660e8400-e29b-41d4-a716-446655440001",
    "title": "1984",
    "author": "George Orwell"
  }
]
```

---

#### Get Book by ID

```http
GET /api/v1/books/{bookId}
```

**Path Parameters**:
- `bookId`: Book UUID

**Response** (200 OK):
```json
{
  "bookId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "The Great Gatsby",
  "author": "F. Scott Fitzgerald"
}
```

**Error Response** (404 Not Found):
```json
{
  "timestamp": "2026-06-18T10:30:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Book not found with id: abc123",
  "path": "/api/v1/books/abc123"
}
```

---

#### Update Book

```http
PUT /api/v1/books/{bookId}
Content-Type: application/json
```

**Path Parameters**:
- `bookId`: Book UUID

**Request Body**:
```json
{
  "title": "The Great Gatsby (Updated Edition)",
  "author": "F. Scott Fitzgerald"
}
```

**Response** (200 OK):
```json
{
  "bookId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "The Great Gatsby (Updated Edition)",
  "author": "F. Scott Fitzgerald"
}
```

---

#### Delete Book

```http
DELETE /api/v1/books/{bookId}
```

**Path Parameters**:
- `bookId`: Book UUID

**Response** (204 No Content)

---

### User Endpoints

#### Create User

```http
POST /api/v1/users
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "John Doe",
  "age": 30
}
```

**Response** (201 Created):
```json
{
  "userId": "660e8400-e29b-41d4-a716-446655440001",
  "name": "John Doe",
  "age": 30,
  "ratedBooks": []
}
```

**Validation Rules**:
- `name`: Required, 1-100 characters
- `age`: Required, positive number

---

#### Get All Users

```http
GET /api/v1/users
```

**Response** (200 OK):
```json
[
  {
    "userId": "660e8400-e29b-41d4-a716-446655440001",
    "name": "John Doe",
    "age": 30,
    "ratedBooks": []
  }
]
```

---

#### Get User by ID

```http
GET /api/v1/users/{userId}
```

**Path Parameters**:
- `userId`: User UUID

**Response** (200 OK):
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

---

#### Update User

```http
PUT /api/v1/users/{userId}
Content-Type: application/json
```

**Path Parameters**:
- `userId`: User UUID

**Request Body**:
```json
{
  "name": "John Doe Updated",
  "age": 31
}
```

**Response** (200 OK):
```json
{
  "userId": "660e8400-e29b-41d4-a716-446655440001",
  "name": "John Doe Updated",
  "age": 31,
  "ratedBooks": []
}
```

---

#### Delete User

```http
DELETE /api/v1/users/{userId}
```

**Path Parameters**:
- `userId`: User UUID

**Response** (204 No Content)

---

#### Add Book Rating

```http
POST /api/v1/users/{userId}/reviews
Content-Type: application/json
```

**Path Parameters**:
- `userId`: User UUID

**Request Body**:
```json
{
  "userId": "660e8400-e29b-41d4-a716-446655440001",
  "bookId": "550e8400-e29b-41d4-a716-446655440000",
  "rating": 5
}
```

**Response** (201 Created):
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

**Validation Rules**:
- `userId`: Required, must exist
- `bookId`: Required, must exist
- `rating`: Required, integer between 1 and 5

---

## RAG-Powered Endpoints (Planned)

### Semantic Search

#### Search Books by Query

```http
GET /api/v1/search/books
```

**Query Parameters**:
- `q` (required): Search query
- `limit` (optional): Number of results (default: 10, max: 50)
- `minScore` (optional): Minimum similarity score (0.0-1.0)
- `genres` (optional): Filter by genres (comma-separated)
- `minRating` (optional): Minimum average rating
- `publishedAfter` (optional): Filter by publication year

**Example Request**:
```http
GET /api/v1/search/books?q=fantasy+books+with+dragons&limit=10&genres=Fantasy&minRating=4.0
```

**Response** (200 OK):
```json
{
  "queryId": "search-query-123",
  "query": "fantasy books with dragons",
  "results": [
    {
      "bookId": "book-101",
      "title": "The Hobbit",
      "author": "J.R.R. Tolkien",
      "score": 0.95,
      "relevanceReason": "High semantic similarity to query",
      "excerpt": "...Smaug the dragon lay sleeping...",
      "genres": ["Fantasy", "Adventure"],
      "averageRating": 4.8,
      "publishedYear": 1937
    },
    {
      "bookId": "book-102",
      "title": "Eragon",
      "author": "Christopher Paolini",
      "score": 0.89,
      "relevanceReason": "Contains dragons and fantasy elements",
      "excerpt": "...the dragon egg began to hatch...",
      "genres": ["Fantasy", "Young Adult"],
      "averageRating": 4.2,
      "publishedYear": 2003
    }
  ],
  "totalResults": 2,
  "processingTimeMs": 150,
  "metadata": {
    "timestamp": "2026-06-18T13:00:05.000Z",
    "searchType": "semantic",
    "model": "text-embedding-ada-002"
  }
}
```

---

#### Async Search (For Long-Running Queries)

**Step 1: Submit Search Request**

```http
POST /api/v1/search/books/async
Content-Type: application/json
```

**Request Body**:
```json
{
  "query": "complex search query",
  "filters": {
    "genres": ["Fantasy", "Science Fiction"],
    "minRating": 4.0,
    "publishedAfter": 2000
  },
  "limit": 50
}
```

**Response** (202 Accepted):
```json
{
  "queryId": "search-query-456",
  "status": "processing",
  "estimatedTimeSeconds": 5,
  "statusUrl": "/api/v1/search/books/async/search-query-456"
}
```

**Step 2: Poll for Results**

```http
GET /api/v1/search/books/async/{queryId}
```

**Response** (200 OK - Processing):
```json
{
  "queryId": "search-query-456",
  "status": "processing",
  "progress": 60
}
```

**Response** (200 OK - Completed):
```json
{
  "queryId": "search-query-456",
  "status": "completed",
  "results": [...],
  "totalResults": 25,
  "processingTimeMs": 4500
}
```

---

### Recommendations

#### Get Personalized Recommendations

```http
GET /api/v1/recommendations/users/{userId}
```

**Path Parameters**:
- `userId`: User UUID

**Query Parameters**:
- `limit` (optional): Number of recommendations (default: 10, max: 50)
- `excludeRated` (optional): Exclude already rated books (default: true)
- `algorithm` (optional): Recommendation algorithm (default: hybrid)
  - `collaborative`: Collaborative filtering
  - `content-based`: Content-based filtering
  - `hybrid`: Combination of both

**Response** (200 OK):
```json
{
  "userId": "user-123",
  "recommendations": [
    {
      "bookId": "book-201",
      "title": "Dune",
      "author": "Frank Herbert",
      "score": 0.92,
      "reason": "Similar to your highly-rated books",
      "genres": ["Science Fiction"],
      "averageRating": 4.7,
      "predictedRating": 4.8
    },
    {
      "bookId": "book-202",
      "title": "The Name of the Wind",
      "author": "Patrick Rothfuss",
      "score": 0.88,
      "reason": "Popular among users with similar tastes",
      "genres": ["Fantasy"],
      "averageRating": 4.6,
      "predictedRating": 4.5
    }
  ],
  "totalRecommendations": 2,
  "algorithm": "hybrid",
  "basedOn": {
    "ratedBooks": 15,
    "preferredGenres": ["Fantasy", "Science Fiction"],
    "similarUsers": 42
  },
  "metadata": {
    "timestamp": "2026-06-18T14:00:03.000Z",
    "model": "collaborative-filtering-with-embeddings"
  }
}
```

---

#### Get Similar Books

```http
GET /api/v1/books/{bookId}/similar
```

**Path Parameters**:
- `bookId`: Book UUID

**Query Parameters**:
- `limit` (optional): Number of similar books (default: 10, max: 50)
- `minScore` (optional): Minimum similarity score (0.0-1.0)

**Response** (200 OK):
```json
{
  "bookId": "book-101",
  "title": "The Hobbit",
  "similarBooks": [
    {
      "bookId": "book-103",
      "title": "The Lord of the Rings",
      "author": "J.R.R. Tolkien",
      "similarityScore": 0.95,
      "reason": "Same author, similar themes and style",
      "genres": ["Fantasy", "Adventure"],
      "averageRating": 4.9
    },
    {
      "bookId": "book-104",
      "title": "The Chronicles of Narnia",
      "author": "C.S. Lewis",
      "similarityScore": 0.87,
      "reason": "Similar fantasy themes and writing style",
      "genres": ["Fantasy", "Children's Literature"],
      "averageRating": 4.5
    }
  ],
  "totalSimilar": 2,
  "metadata": {
    "timestamp": "2026-06-18T15:00:00.000Z",
    "similarityMethod": "cosine",
    "model": "text-embedding-ada-002"
  }
}
```

---

### Book Content Analysis

#### Get Book Summary

```http
GET /api/v1/books/{bookId}/summary
```

**Path Parameters**:
- `bookId`: Book UUID

**Response** (200 OK):
```json
{
  "bookId": "book-101",
  "title": "The Hobbit",
  "summary": "A hobbit named Bilbo Baggins embarks on an unexpected adventure...",
  "keyThemes": ["Adventure", "Courage", "Friendship", "Good vs Evil"],
  "mainCharacters": ["Bilbo Baggins", "Gandalf", "Thorin Oakenshield"],
  "setting": "Middle-earth",
  "tone": "Whimsical, adventurous",
  "generatedBy": "gpt-4",
  "metadata": {
    "timestamp": "2026-06-18T16:00:00.000Z",
    "confidence": 0.92
  }
}
```

---

#### Extract Book Entities

```http
GET /api/v1/books/{bookId}/entities
```

**Path Parameters**:
- `bookId`: Book UUID

**Response** (200 OK):
```json
{
  "bookId": "book-101",
  "title": "The Hobbit",
  "entities": {
    "characters": [
      {"name": "Bilbo Baggins", "type": "protagonist", "mentions": 342},
      {"name": "Gandalf", "type": "supporting", "mentions": 156},
      {"name": "Smaug", "type": "antagonist", "mentions": 89}
    ],
    "locations": [
      {"name": "The Shire", "type": "region", "mentions": 45},
      {"name": "Lonely Mountain", "type": "landmark", "mentions": 78}
    ],
    "themes": [
      {"name": "Adventure", "confidence": 0.95},
      {"name": "Courage", "confidence": 0.89},
      {"name": "Greed", "confidence": 0.76}
    ]
  },
  "metadata": {
    "timestamp": "2026-06-18T16:30:00.000Z",
    "extractionModel": "spacy-en-core-web-lg"
  }
}
```

---

### Analytics Endpoints

#### Get Book Statistics

```http
GET /api/v1/books/{bookId}/stats
```

**Response** (200 OK):
```json
{
  "bookId": "book-101",
  "title": "The Hobbit",
  "statistics": {
    "totalRatings": 1523,
    "averageRating": 4.8,
    "ratingDistribution": {
      "5": 1205,
      "4": 245,
      "3": 58,
      "2": 12,
      "1": 3
    },
    "totalReviews": 342,
    "readCount": 2456,
    "wishlistCount": 789,
    "popularityScore": 0.94
  },
  "trends": {
    "ratingTrend": "increasing",
    "popularityTrend": "stable"
  },
  "metadata": {
    "timestamp": "2026-06-18T17:00:00.000Z",
    "lastUpdated": "2026-06-18T16:55:00.000Z"
  }
}
```

---

#### Get Trending Books

```http
GET /api/v1/books/trending
```

**Query Parameters**:
- `period` (optional): Time period (day, week, month, year)
- `limit` (optional): Number of results (default: 10)
- `genre` (optional): Filter by genre

**Response** (200 OK):
```json
{
  "period": "week",
  "trendingBooks": [
    {
      "bookId": "book-301",
      "title": "New Release Book",
      "author": "Popular Author",
      "trendScore": 0.98,
      "ratingChange": "+0.3",
      "readCountChange": "+450",
      "averageRating": 4.7,
      "genres": ["Fiction"]
    }
  ],
  "metadata": {
    "timestamp": "2026-06-18T18:00:00.000Z",
    "calculatedAt": "2026-06-18T17:00:00.000Z"
  }
}
```

---

## Rate Limiting

### Rate Limit Headers

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1718712000
```

### Rate Limits by Endpoint Type

| Endpoint Type | Limit | Window |
|---------------|-------|--------|
| Read (GET) | 1000 requests | per hour |
| Write (POST/PUT/DELETE) | 100 requests | per hour |
| Search | 100 requests | per hour |
| Recommendations | 50 requests | per hour |

### Rate Limit Exceeded Response

```json
{
  "timestamp": "2026-06-18T10:30:00.000Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 3600 seconds.",
  "retryAfter": 3600
}
```

---

## API Examples

### Complete Workflow Example

```bash
# 1. Create a book
curl -X POST http://localhost:8081/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "The Hobbit",
    "author": "J.R.R. Tolkien"
  }'

# Response: {"bookId": "book-101", ...}

# 2. Create a user
curl -X POST http://localhost:8081/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Smith",
    "age": 28
  }'

# Response: {"userId": "user-123", ...}

# 3. User rates the book
curl -X POST http://localhost:8081/api/v1/users/user-123/reviews \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "bookId": "book-101",
    "rating": 5
  }'

# 4. Search for similar books (RAG-powered)
curl "http://localhost:8081/api/v1/search/books?q=fantasy+adventure&limit=5"

# 5. Get personalized recommendations
curl "http://localhost:8081/api/v1/recommendations/users/user-123?limit=10"

# 6. Get similar books
curl "http://localhost:8081/api/v1/books/book-101/similar?limit=5"
```

### Error Handling Example

```bash
# Invalid book creation (missing required field)
curl -X POST http://localhost:8081/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": ""
  }'

# Response (400 Bad Request):
# {
#   "timestamp": "2026-06-18T10:30:00.000Z",
#   "status": 400,
#   "error": "Bad Request",
#   "message": "Validation failed",
#   "validationErrors": [
#     {
#       "field": "title",
#       "message": "Title is required",
#       "rejectedValue": ""
#     },
#     {
#       "field": "author",
#       "message": "Author is required",
#       "rejectedValue": null
#     }
#   ]
# }
```

---

## OpenAPI/Swagger Documentation

Interactive API documentation is available at:

```
http://localhost:8081/swagger-ui.html
```

OpenAPI JSON specification:

```
http://localhost:8081/api-docs
```

---

## Deprecation Policy

### Deprecation Notice Format

Deprecated endpoints will include a `Deprecated` header:

```http
Deprecated: true
Sunset: Wed, 31 Dec 2026 23:59:59 GMT
Link: </api/v2/books>; rel="successor-version"
```

### Deprecation Timeline

1. **Announcement**: 6 months before deprecation
2. **Warning Headers**: Added to responses
3. **Documentation Update**: Mark as deprecated
4. **Sunset Date**: Endpoint removed

---

## Best Practices for API Consumers

1. **Use Correlation IDs**: Include `X-Correlation-ID` header for request tracking
2. **Handle Rate Limits**: Implement exponential backoff
3. **Cache Responses**: Cache GET responses when appropriate
4. **Validate Input**: Validate data before sending requests
5. **Handle Errors Gracefully**: Implement proper error handling
6. **Use Pagination**: Don't request all data at once
7. **Monitor API Changes**: Subscribe to API changelog
8. **Use HTTPS**: Always use HTTPS in production
9. **Implement Timeouts**: Set appropriate request timeouts
10. **Log API Calls**: Log requests and responses for debugging

---

## Changelog

### Version 1.0 (Current)
- Initial API release
- Book CRUD operations
- User CRUD operations
- Rating system

### Version 1.1 (Planned)
- Semantic search endpoints
- Recommendation endpoints
- Book analytics
- JWT authentication

### Version 2.0 (Future)
- GraphQL API
- WebSocket support for real-time updates
- Advanced filtering and sorting
- Bulk operations

---

## References

- [REST API Design Best Practices](https://restfulapi.net/)
- [HTTP Status Codes](https://httpstatuses.com/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [API Versioning Strategies](https://www.baeldung.com/rest-versioning)

---

**Document Version**: 1.0  
**Last Updated**: 2026-06-18  
**Author**: Bob (AI Assistant)  
**Status**: Draft