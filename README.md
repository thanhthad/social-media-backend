<img width="2604" height="1991" alt="DBdiagram" src="https://github.com/user-attachments/assets/098c984a-e939-475d-84c8-447ef2f3681d" /># Social Media Backend Platform

A production-oriented social media backend system built with **Java Spring Boot**, focusing on scalable architecture, security, real-time communication, caching, and modern backend development practices.

This project provides core social networking features including user management, posts, reactions, comments, follows, messaging, notifications, and media management.

---
# 🚀 Tech Stack
## Backend

* Java 21
* Spring Boot 3
* Spring Security 6
* Spring Data JPA / Hibernate
* Spring Web MVC
* Spring WebSocket (STOMP)
* Maven

## Database

* PostgreSQL 17
* Flyway Database Migration

## Cache & Performance

* Redis
* Spring Cache
* Redis-based Rate Limiting

## Authentication & Security

* JWT Authentication
* Access Token / Refresh Token mechanism
* Role-based Authorization
* BCrypt Password Encryption

## Storage

* Cloudinary Media Storage

## Documentation

* OpenAPI / Swagger UI

## DevOps

* Docker
* Docker Compose

---

# 🏗️ System Architecture

The project follows a modular layered architecture designed for maintainability and scalability.

```
media.social

├── common
│   ├── exception
│   ├── response
│   └── ratelimit
│
├── infrastructure
│   ├── security
│   ├── redis
│   ├── websocket
│   └── cloudinary
|
└── config
|
├── modules
│
│   ├── user
│   ├── post
│   ├── conversation
│   └── notification
│
```

---

# ✨ Main Features

# 🔐 Authentication & Authorization

Implemented secure authentication flow:

* User registration
* User login
* JWT access token generation
* Refresh token mechanism
* Logout with token revocation
* Role-based permission management

Security flow:

```
Client
 |
 | Login
 ↓
Authentication Service
 |
 ↓
JWT Access Token
 |
 ↓
JwtAuthenticationFilter
 |
 ↓
SecurityContext
```

---

# 👤 User Management

Features:

* User profile management
* Avatar update
* Change password
* User search
* Follow / Unfollow users
* Block users
* Role management
* Admin user management

---

# 📝 Post System

Supported features:

* Create post
* Update post
* Delete post
* Upload multiple media files
* Post visibility:

```
PUBLIC
FOLLOWERS_ONLY
PRIVATE
```

* Save post
* Hashtag support
* Trending hashtag
* Report post

---

# 💬 Comment System

Features:

* Create comment
* Reply comment
* Update comment
* Delete comment
* Comment reaction

Relationship:

```
Post
 |
 └── Comment
        |
        └── Reply
```

---

# ❤️ Reaction System

Implemented reaction system for:

* Posts
* Comments
* Messages

Supported:

```
LIKE
LOVE
HAHA
WOW
SAD
ANGRY
```

Includes:

* Add reaction
* Remove reaction
* Count reactions
* Get users reacted

---

# 💾 Saved Post

Users can:

* Save posts
* Remove saved posts
* Check saved status

---

# 💬 Real-time Messaging

Built with:

* Spring WebSocket
* STOMP protocol

Features:

* Private conversation
* Group conversation
* Send messages
* Send media messages
* Message reaction
* Delete message
* Read message tracking

Architecture:

```
Client

   |
 WebSocket

   |
Spring WebSocket Broker

   |
Message Service

   |
PostgreSQL
```

---

# 🔔 Notification System

Real-time notification support:

Examples:

* Someone follows you
* Someone reacts to your post
* Someone comments
* New message

Features:

* Get notifications
* Mark as read
* Mark all as read
* Count unread notifications

---

# ⚡ Redis Usage

Redis is used for:

## 1. Cache

Caching frequently accessed data:

Example:

```
User Profile
Post Information
```

Flow:

```
Request

 ↓

Redis Cache

 ↓ miss

Database

 ↓

Update Cache
```

---

## 2. Rate Limiting

Custom annotation-based rate limit:

Example:

```java
@RateLimit(
    name = "AUTH_LOGIN",
    limit = 10,
    windowSeconds = 60
)
```

Architecture:

```
Controller

 ↓

RateLimit Aspect

 ↓

Redis Counter

 ↓

Allow / Reject Request
```

---

# 🗄️ Database Design

Database migration managed by Flyway.

Migration structure:

```
db/migration

V1__create_tables.sql

V2__insert_roles.sql

V3__insert_fake_datas.sql

V4__update_chat_fields.sql
```

Database contains:

<img width="2604" height="1991" alt="DBdiagram" src="https://github.com/user-attachments/assets/6a21217d-92c1-4e8b-b7cc-fa40f4a5f66f" />


---

# 📦 Docker Support

The project supports containerized deployment.

Services:

```
docker-compose

├── PostgreSQL

├── Redis

└── Spring Boot Application
```

Run:

```bash
docker compose up -d
```

---

# ⚙️ Environment Configuration

Create:

```
application-local.properties
```

Example:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/socialdb

spring.datasource.username=postgres

spring.datasource.password=password


spring.redis.host=localhost

spring.redis.port=6379


cloudinary.cloud-name=xxx
cloudinary.api-key=xxx
cloudinary.api-secret=xxx
```

---

# ▶️ Run Project Locally

## Requirements

Install:

* Java 21
* PostgreSQL
* Redis
* Maven

Clone:

```bash
git clone https://github.com/thanhthad/social-media-backend.git
```

Go to project:

```bash
cd social-media-backend
```

Run:

```bash
./mvnw spring-boot:run
```

Application runs:

```
http://localhost:8080
```

Swagger:

```
http://localhost:8080/swagger-ui/index.html
```

---

# 🧪 Testing

Implemented unit tests for:

* Authentication Service
* User Service
* Post Service

Testing stack:

* JUnit 5
* Mockito

---

# 📌 Future Improvements

Planned improvements:

* CI/CD pipeline with GitHub Actions
* Deploy using Docker on VPS
* Monitoring with Spring Actuator
* Prometheus + Grafana
* Kafka event-driven notification system
* Elasticsearch for advanced search
* Cursor pagination for large datasets
* Distributed WebSocket using Redis Pub/Sub

---

# 👨‍💻 Author

**Nguyễn Duy Thành**

Backend Developer

Main technologies:

* Java
* Spring Boot
* PostgreSQL
* Redis
* WebSocket
* Docker

---

# 📄 License

This project is created for learning, portfolio, and backend engineering practice purposes.
