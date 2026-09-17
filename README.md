# 🚀 Social Media & Dating Platform — Enterprise Backend (Spring Boot 3 + Java 21)

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2+-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6.2+-green.svg?style=flat-square&logo=springsecurity)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-Cache_%26_RateLimit-DC382D.svg?style=flat-square&logo=redis)](https://redis.io/)
[![Flyway](https://img.shields.io/badge/Flyway-Migration-CC0200.svg?style=flat-square&logo=flyway)](https://flywaydb.org/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED.svg?style=flat-square&logo=docker)](https://www.docker.com/)
[![Swagger](https://img.shields.io/badge/OpenAPI_3-Swagger_UI-85EA2D.svg?style=flat-square&logo=swagger)](https://swagger.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

An enterprise-grade, high-throughput backend platform engineered for modern social media, short-video streaming, real-time communication, and matchmaking dating services. Built with **Java 21**, **Spring Boot 3**, **PostgreSQL 17**, and **Redis**, the system adheres to **Domain-Driven Design (DDD)**, **Modular Monolith Architecture**, **STOMP WebSocket Real-Time Pub/Sub**, **JWT Dual-Token Security**, and **Redis Distributed Rate Limiting**.

---

## 📑 Table of Contents

- [🏛️ System Architecture](#️-system-architecture)
- [📖 Interactive API Documentation (Swagger)](#-interactive-api-documentation-swagger)
- [🧭 30 Controllers & Domain Breakdown](#-30-controllers--domain-breakdown)
- [🛡️ Security Architecture & Guest Access](#️-security-architecture--guest-access)
- [⚡ Redis Caching & Distributed Rate Limiting](#-redis-caching--distributed-rate-limiting)
- [🗄️ Database Schema & Flyway Migrations](#️-database-schema--flyway-migrations)
- [📡 Real-Time WebSocket & STOMP Infrastructure](#-real-time-websocket--stomp-infrastructure)
- [⚙️ Environment Configuration](#️-environment-configuration)
- [🚀 Quick Start & Deployment](#-quick-start--deployment)
- [🧪 Testing & Quality Assurance](#-testing--quality-assurance)
- [👨‍💻 Author & License](#-author--license)

---

## 🏛️ System Architecture

The codebase is architected as a **Modular Monolith**, ensuring strict boundary separation across business domains while minimizing operational complexity:

```
media.social
├── common                      # Cross-cutting foundational utilities
│   ├── exception               # GlobalExceptionHandler, Custom Runtime Exceptions
│   ├── ratelimit               # Redis-backed @RateLimit AOP Aspect
│   └── response                # Unified ResponseData<T> envelope wrapper
├── infrastructure              # External systems, hardware & framework integrations
│   ├── cloudinary              # Multi-media storage pipeline (images, vertical videos)
│   ├── redis                   # RedisTemplate, CacheManager, and Serialization
│   ├── security                # SecurityFilterChain, JWT Token Provider, UserContextHolder
│   └── websocket               # STOMP broker, ChannelInterceptor, Handshake Handler
└── modules                     # 7 Domain Business Modules (DDD)
    ├── auth                    # Identity, JWT lifecycle, email verification OTP, password reset
    ├── user                    # User profile, bidirectional social graph, follows, blocks, stats
    ├── post                    # Multi-stream feeds, post CRUD, media attachments, hashtags, reports
    ├── conversation            # Direct & group chats, member management, read cursor, reactions
    ├── dating                  # Dating profiles, GPS discovery, swipe engine, match verification
    ├── story                   # 24h ephemeral stories, viewer auditing, interactive reactions
    └── notification            # Multi-channel push notification broker
```

---

## 📖 Interactive API Documentation (Swagger)

The backend provides **159 REST Endpoints** fully documented and tested via SpringDoc OpenAPI 3:

<img width="1902" height="868" alt="image" src="https://github.com/user-attachments/assets/91a7d0ab-b62a-4da6-abad-c3591117ba43" />

* **Swagger UI Web Console**: `http://localhost:8080/swagger-ui/index.html`
* **OpenAPI 3.0 Spec (JSON)**: `http://localhost:8080/v3/api-docs`

---

## 🧭 30 Controllers & Domain Breakdown

The 159 endpoints are organized into 30 dedicated REST controllers across 8 core business domains:

| Domain | Controller | Base Path | Key Capabilities & Endpoints |
| :--- | :--- | :--- | :--- |
| **Authentication** | `AuthController` | `/api/auth` | User registration, login, JWT refresh token rotation, logout revocation, OTP email verification, password reset. |
| **User & Graph** | `UserController` | `/api/users` | Profile retrieval & full/partial updates, avatar/cover uploads, username/password change, personal statistics. |
| | `FriendshipController` | `/api/friends` | Friend request lifecycle (send, accept, reject, cancel, unfriend), pending requests, mutual friends count. |
| | `FollowController` | `/api/follows` | Unidirectional follow/unfollow system, followers and following paginated lists. |
| | `BlockController` | `/api/blocks` | Two-way blocking mechanism, block status verification, blocked users directory. |
| **Feed & Posts** | `PostController` | `/api/posts` | Multi-stream feeds (Friend, Explore, Public Guest, User), Post CRUD, media attachment deletion & appending. |
| | `PostReactionController` | `/api/posts` | 6 emotion types (`LIKE`, `LOVE`, `HAHA`, `WOW`, `SAD`, `ANGRY`), real-time reaction counters, reacted user breakdowns. |
| | `CommentController` | `/api/comments` | Hierarchical threaded comments (root & nested replies), inline content edit (`PATCH /{id}`), deletion. |
| | `CommentReactionController` | `/api/comments` | Emotional reactions on comments, reaction removal, reacted user details. |
| | `SavedPostController` | `/api/posts` | Bookmark posts to private collections, unsave, check saved status. |
| | `HashtagController` | `/api/hashtags` | Automated hashtag extraction, trending hashtags aggregation, hashtag post search. |
| | `ReportController` | `/api/reports` | Community violation reporting for posts, administrative review, and automatic hiding. |
| **Reels Studio** | `ReelController` | `/api/reels` | Dedicated vertical video streams (feed, explore, public, my reels), video upload, caption & visibility editing (`PATCH /{id}`), watch duration & replay tracking. |
| **Stories (24h)** | `StoryController` | `/api/stories` | Ephemeral 24h photo/video moments, viewer analytics auditing (`StoryViewResponse`), story reactions, privacy modification (`PATCH /{id}/visibility`). |
| **Real-Time Chat** | `ConversationController` | `/api/conversations` | 1-on-1 private chats, group chat rooms, group avatar modification (`PUT`), group name updates (`PUT`), room deletion. |
| | `ConversationMemberController` | `/api/conversation-members` | Group membership roster, adding/removing members, last read message cursor updates (`PUT`). |
| | `MessageController` | `/api/messages` | Message transmission (rich text, media attachments), chat history retrieval, message deletion. |
| | `MessageReactionController` | `/api/message-reactions` | Real-time message emotion reactions, reaction removals, reacted participant lists. |
| **Dating & Match** | `DatingProfileController` | `/api/dating` | Dating profile onboarding, GPS coordinate updates, granular single-field modifications (`PATCH /profile/field`). |
| | `DatingDiscoveryController` | `/api/dating` | Matchmaking recommendation algorithm based on geo-distance (km), age range, and gender preference. |
| | `DatingSwipeController` | `/api/dating` | Swipe deck engine (`LIKE` / `DISLIKE`), instant mutual match detection, swipe history undo. |
| | `DatingMatchController` | `/api/dating` | Active mutual matches directory, direct chat channel initiation, unmatching. |
| | `DatingPreferenceController` | `/api/dating` | User matching criteria: min/max age range, search radius (km), preferred gender (`PUT /preferences`). |
| | `DatingInterestController` | `/api/dating` | Master interest catalog, user lifestyle interests assignment (`PUT /interests`). |
| | `DatingProfilePhotoController` | `/api/dating` | Photo gallery upload, primary photo assignment (`PATCH /photos/{id}/primary`), photo deletion. |
| | `DatingReportController` | `/api/dating/reports` | Abusive dating profile reporting, administrative moderation review. |
| **Notifications** | `NotificationController` | `/api/notifications` | Notification feeds, unread filtering, mark single as read (`PATCH`), mark all read (`PATCH /read-all`), deletion. |
| **Administration** | `AdminUserController` | `/api/admin/users` | Global user management, account status changes (`ACTIVE`, `LOCKED`, `BANNED` via `PATCH`). |
| | `AdminUserRoleController` | `/api/admin/users` | Authority assignments (`ROLE_ADMIN`, `ROLE_MODERATOR`, `ROLE_USER`), role revocation. |

---

## 🛡️ Security Architecture & Guest Access

### 1. JWT Dual-Token Lifecycle
* **Access Token**: Short-lived (24h), HMAC-SHA256 signed stateless JWT containing subject identity and role claims.
* **Refresh Token**: Long-lived (7 days), persisted with one-time rotation to mitigate token theft.
* **Instant Token Revocation**: Logout operations inject access tokens into a Redis blocklist with an automatic Time-To-Live (TTL) matching the token expiration.

### 2. Public Guest Access (`SecurityConfig.java`)
Configured to deliver a frictionless experience for unauthenticated prospective users:
* `GET /api/posts/public/**` — Public social feeds and individual public posts.
* `GET /api/reels/public/**` — Public vertical short-video reels.
* `GET /api/hashtags/**` — Trending hashtags and tagged posts.
* `GET /api/comments/post/**` & `GET /api/comments/*/replies` — Unrestricted public comment reading without 401 exceptions.

### 3. Thread-Safe Context
`UserContextHolder` leverages `ThreadLocal` bound to the Spring SecurityContext to ensure thread-safe retrieval of current user identity and permissions across all service layers.

---

## ⚡ Redis Caching & Distributed Rate Limiting

### 1. High-Performance Entity Caching
* **Target Entities**: User profile projections, post reaction aggregates, story viewer counts, and hot-score rankings.
* **Consistency Model**: Cache-aside pattern with automatic invalidation on mutating operations (create, update, delete).

### 2. Distributed Rate Limiting (`@RateLimit`)
Custom Spring AOP annotation powered by Redis Token Bucket algorithm protects mission-critical endpoints against brute-force attacks and resource exhaustion:

```java
@RateLimit(
    name = "AUTH_LOGIN",
    limit = 10,
    windowSeconds = 60
)
@PostMapping("/login")
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    return ResponseData.success(authService.login(request), "Login successful", HttpStatus.OK);
}
```

---

## 🗄️ Database Schema & Flyway Migrations

Database evolution is strictly version-controlled using **Flyway**:

```
src/main/resources/db/migration/
├── V1__create_tables.sql              # Core schema: users, posts, comments, reactions, friendships
├── V2__insert_roles.sql               # Default security authorities (ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR)
├── V3__insert_fake_datas.sql          # Development fixture datasets
├── V4__update_chat_fields.sql         # Conversations, messages, read receipts
├── V5__create_dating_tables.sql       # Dating profiles, swipes, matches, preferences
├── V6__create_story_tables.sql        # Ephemeral stories, viewer tracking, reactions
└── V10__create_reel_details_table.sql # Reels video metadata, watch analytics, hot-score ranking
```

### Entity Relationship Diagram (ERD):

<img width="2604" height="1991" alt="DBdiagram" src="https://github.com/user-attachments/assets/098c984a-e939-475d-84c8-447ef2f3681d" />

---

## 📡 Real-Time WebSocket & STOMP Infrastructure

Configured in `WebSocketConfig.java` via STOMP over SockJS at `/ws`:

* **Inbound Handshake**: Intercepts and validates JWT Bearer tokens from STOMP connection headers.
* **Pub/Sub Broker Destinations**:
  * `/user/{userId}/queue/messages` — Point-to-point private and group chat message delivery.
  * `/user/{userId}/queue/message-reactions` — Instant message reaction state updates.
  * `/user/{userId}/queue/notifications` — Real-time event notifications (likes, comments, friend requests, dating matches).
  * `/topic/system` — Broadcast administrative announcements.

---

## ⚙️ Environment Configuration

Configure properties in `src/main/resources/application.properties` or pass via environment variables:

```properties
server.port=8080

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/socialdb
spring.datasource.username=postgres
spring.datasource.password=your_secure_password
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true

# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT Security (Minimum 256-bit secret key)
jwt.secret=your_super_secret_jwt_key_at_least_256_bits_long
jwt.access-token-expiration=86400000
jwt.refresh-token-expiration=604800000

# Cloudinary Storage Credentials
cloudinary.cloud-name=your_cloud_name
cloudinary.api-key=your_api_key
cloudinary.api-secret=your_api_secret

# Spring Mail (Optional - for Email Verification OTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
```

---

## 🚀 Quick Start & Deployment

### 1. Prerequisites
* **Java**: OpenJDK 21 or Oracle JDK 21
* **Database**: PostgreSQL 15+
* **Cache**: Redis 6+
* **Build Tool**: Apache Maven 3.8+ (or bundled `./mvnw`)
* **Docker**: Docker Engine 20+ & Docker Compose (optional)

### 2. Launch Infrastructure via Docker Compose
```bash
# Start PostgreSQL and Redis containers
docker compose up -d
```

### 3. Build & Run Application
```bash
# Compile and package application
./mvnw clean compile

# Execute Spring Boot application
./mvnw spring-boot:run
```

The server will initialize at: `http://localhost:8080`.

### 4. Explore Interactive API Console
Open your browser and navigate to:
```
http://localhost:8080/swagger-ui/index.html
```

---

## 🧪 Testing & Quality Assurance

Execute the automated test suite powered by **JUnit 5** and **Mockito**:

```bash
# Run unit and integration tests
./mvnw test
```

---

## 👨‍💻 Author & License

* **Lead Developer**: Nguyễn Duy Thành
* **Repository**: [thanhthad/social-media-backend](https://github.com/thanhthad/social-media-backend)
* **License**: Licensed under the **[MIT License](LICENSE)**.
