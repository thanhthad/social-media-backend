# 🚀 Social Media & Dating Platform — Backend (Spring Boot 3 + Java 21)

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2+-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6.2+-green.svg?style=flat-square&logo=springsecurity)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-Cache_%26_RateLimit-DC382D.svg?style=flat-square&logo=redis)](https://redis.io/)
[![Flyway](https://img.shields.io/badge/Flyway-Migration-CC0200.svg?style=flat-square&logo=flyway)](https://flywaydb.org/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED.svg?style=flat-square&logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

A high-performance, production-ready enterprise social networking and dating backend platform built with **Java 21**, **Spring Boot 3**, **PostgreSQL 17**, and **Redis**. Designed with modern software engineering principles: **Domain-Driven Design (DDD), Modular Monolith Architecture, STOMP WebSocket real-time pub/sub, JWT Dual-Token Security, Redis Caching & Distributed Rate Limiting**.

---

## 📑 Table of Contents

- [🏛️ System Architecture](#️-system-architecture)
- [✨ Key Features & Modules](#-key-features--modules)
  - [1. Authentication & Security](#1-authentication--security)
  - [2. User Profile & Social Graph](#2-user-profile--social-graph)
  - [3. Posts & Feed Discovery](#3-posts--feed-discovery)
  - [4. Multi-Entity Reaction Engine](#4-multi-entity-reaction-engine)
  - [5. Threaded Comment System](#5-threaded-comment-system)
  - [6. Reels & Video Ranking](#6-reels--video-ranking)
  - [7. Ephemeral Stories & Viewer Tracking](#7-ephemeral-stories--viewer-tracking)
  - [8. Dating & Matchmaking Engine](#8-dating--matchmaking-engine)
  - [9. Real-Time Chat & STOMP Messaging](#9-real-time-chat--stomp-messaging)
  - [10. Push Notification System](#10-push-notification-system)
  - [11. Content Moderation & Administration](#11-content-moderation--administration)
- [⚡ Redis Caching & Rate Limiting](#-redis-caching--rate-limiting)
- [🗄️ Database Design & Migration](#️-database-design--migration)
- [📡 WebSocket & STOMP Infrastructure](#-websocket--stomp-infrastructure)
- [⚙️ Environment & Configuration](#️-environment--configuration)
- [🚀 Quick Start & Installation](#-quick-start--installation)
- [🧪 Testing & Quality Assurance](#-testing--quality-assurance)
- [👨‍💻 Author & License](#-author--license)

---

## 🏛️ System Architecture

The backend adopts a **Modular Monolith** structure where each feature domain is self-contained with its own controllers, services, repositories, entities, and DTOs:

```
media.social
├── common                      # Global exception handlers, response wrappers, rate limiting
│   ├── exception
│   ├── ratelimit
│   └── response
├── infrastructure              # Cross-cutting infrastructure concerns
│   ├── cloudinary              # Cloudinary media upload and storage service
│   ├── redis                   # Redis template, serialization, caching configurations
│   ├── security                # Security filter chain, JWT provider, UserContextHolder
│   └── websocket               # WebSocket/STOMP config, ChannelInterceptor, Handshake handler
└── modules                     # Domain Feature Modules
    ├── auth                    # Login, register, token refresh, email verification, password reset
    ├── user                    # User profile, friendships, follows, blocks, user statistics
    ├── post                    # Feed, explore, saved posts, hashtags, violation reports
    ├── conversation            # 1-on-1 & group chats, message reactions, read receipts
    ├── dating                  # Dating profile, swipe algorithm, match detection, match management
    ├── story                   # 24-hour stories, story views tracking, story reactions
    └── notification            # Real-time event notifications & push delivery
```

---

## ✨ Key Features & Modules

### 1. Authentication & Security
* **JWT Dual-Token Workflow**: Access tokens (short-lived) + Refresh tokens (persisted with rotation).
* **Token Blacklisting**: Logout instantly revokes tokens via Redis blocklist.
* **Email Verification & Password Recovery**: OTP token generation with expiration limits.
* **Role-Based Access Control (RBAC)**: Fine-grained permissions (`ROLE_USER`, `ROLE_ADMIN`).
* **Interactive OpenAPI / Swagger Documentation**:

<img width="1902" height="868" alt="image" src="https://github.com/user-attachments/assets/91a7d0ab-b62a-4da6-abad-c3591117ba43" />

```
Client Request ──► JwtAuthenticationFilter ──► SecurityContext ──► UserContextHolder
```

---

### 2. User Profile & Social Graph
* **Comprehensive Profile Management**: Full name, username, bio, occupation, company, education, location, phone, and external social links.
* **Social Connections**:
  - **Friendships**: Send, Accept, Reject, Cancel request, and Unfriend.
  - **Follows**: Follow/Unfollow any public user.
  - **Blocks**: Two-way blocking prevents messaging, profile visibility, and post exposure.
* **Mutual Friends Computation**: High-performance query to find and count mutual friends between users.
* **User Statistics API (`UserStatsResponse`)**: Aggregates post count, follower count, following count, friend count, and mutual friends count.

---

### 3. Posts & Feed Discovery
* **Multi-Media Support**: Multi-image and video uploads processed via Cloudinary.
* **Visibility Scopes**: `PUBLIC`, `FRIEND`, and `PRIVATE` visibility controls.
* **Feed Algorithms**:
  - **Friend Feed**: Chronological stream of friends and self-published posts.
  - **Explore Feed**: Ranked discovery feed based on engagement hot scores and trending tags.
* **Hashtag System**: Automated `#hashtag` extraction, indexing, and trending hashtag aggregation.
* **Saved Posts**: Bookmark favorite posts with instant save/unsave APIs.

---

### 4. Multi-Entity Reaction Engine
* **Supported Entities**: Reactions on **Posts**, **Comments**, and **Chat Messages**.
* **6 Rich Emotion Types**: `LIKE (👍)`, `LOVE (❤️)`, `HAHA (😆)`, `WOW (😮)`, `SAD (😢)`, `ANGRY (😡)`.
* **Optimized Dynamic Aggregation**: Accurately counts real-time reactions and supports tabbed query filtering (`/api/posts/{id}/reactions?type=LOVE`).
* **Reaction DTO (`UserReactionResponse`)**: Returns user identity, avatar, timestamp, and their specific `type` of reaction.

---

### 5. Threaded Comment System
* **Hierarchical Tree Structure**: Root-level comments and nested reply comments.
* **Post-Author Tagging**: Identifies if a comment was written by the post creator.
* **Reactions on Comments**: Full 6-emotion reaction support on individual comment bubbles.
* **Real-Time Counter Synchronization**: Increment/decrement `commentCount` on the post entity upon creation/deletion.

---

### 6. Reels & Video Ranking
* **Vertical Short Video Engine**: Dedicated storage and metadata for Reels (`PostType.REEL`).
* **Hot-Score Ranking**: Engagement-based algorithmic discovery scoring for explore streams.
* **Interactive Reactions**: Quick one-tap love reaction and comment drawer support.

---

### 7. Ephemeral Stories & Viewer Tracking
* **24-Hour Expiration**: Automatic expiration handling for short-lived photo/video moments.
* **Audience Analytics (`StoryViewResponse`)**: Tracks viewers, view timestamps, and viewer profiles for the story author.
* **Story Reactions**: Quick emotion replies sent directly to the creator.

---

### 8. Dating & Matchmaking Engine
* **Discovery Algorithm**: Geo-distance filtering, age range matching, and gender preference scoring.
* **Swipe Deck**: Record `LIKE` or `PASS` swipes per candidate.
* **Mutual Match Detection**: Returns `isMatch: true` immediately when both users swipe right.
* **Dating Match Management (`DatingMatchController`)**:
  - `GET /api/dating/matches`: Retrieve all active mutual matches with direct chat triggers.
  - `DELETE /api/dating/matches/{matchId}`: Unmatch connection cleanly.

---

### 9. Real-Time Chat & STOMP Messaging
* **Point-to-Point Messaging**: Delivers private messages instantly over `/user/{userId}/queue/messages`.
* **Media & Emoji Reactions**: Attach photos/videos and react to messages with real-time sync over `/user/{userId}/queue/message-reactions`.
* **Read Receipts & Delivery State**: Track `SENT`, `DELIVERED`, and `READ` message statuses.
* **Conversation Optimization**: Pre-loads last message snippet and unread badges.

---

### 10. Push Notification System
* **Real-Time Event Dispatch**: Triggers push alerts over `/user/{userId}/queue/notifications` for:
  - Post reactions & comment mentions
  - Friend requests & acceptances
  - Dating match celebrations
  - Private messages
* **Notification Management**: Mark read, mark all read, and delete notification by ID.

---

### 11. Content Moderation & Administration
* **Violation Reports**: Users can report suspicious posts, comments, or profiles.
* **Admin Review API**: Review reports, Approve violation (auto-hides content), or Reject false reports.

---

## ⚡ Redis Caching & Rate Limiting

Redis provides multi-tier caching and distributed protection:

### 1. Redis Caching Flow
Caches frequently accessed entities (User profiles, post metadata, story views) with TTL:

```
Request ──► Redis Cache Hit? ──► Return Cached Object
                 │
                 ▼ Miss
            PostgreSQL Query ──► Update Redis Cache ──► Return Result
```

### 2. Custom Distributed Rate Limiting
Annotation-based rate limiting protects sensitive endpoints (Login, Register, OTP verification) against brute force:

```java
@RateLimit(
    name = "AUTH_LOGIN",
    limit = 10,
    windowSeconds = 60
)
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) { ... }
```

---

## 🗄️ Database Design & Migration

Database schema migrations are version-controlled using **Flyway**:

```
src/main/resources/db/migration/
├── V1__create_tables.sql            # Core tables (users, posts, comments, reactions, friendships)
├── V2__insert_roles.sql             # Default system roles (ROLE_USER, ROLE_ADMIN)
├── V3__insert_fake_datas.sql        # Demo mock data
├── V4__update_chat_fields.sql       # Messaging schemas and read status
├── V5__create_dating_tables.sql     # Dating profiles, swipes, and matches
├── V6__create_story_tables.sql      # Stories, views, and interactions
└── V10__create_reel_details_table.sql # Reels metadata and engagement metrics
```

### Entity Relationship Diagram (ERD):

<img width="2604" height="1991" alt="DBdiagram" src="https://github.com/user-attachments/assets/098c984a-e939-475d-84c8-447ef2f3681d" />

---

## 📡 WebSocket & STOMP Infrastructure

Configured in `WebSocketConfig.java` with dual-mode support (Native WebSocket + SockJS Fallback):

```
Frontend Client ──(SockJS / WebSocket Handshake)──► /ws
                     │
                     ▼ ChannelInterceptor
             Validates JWT Bearer Token
                     │
                     ▼
         Spring STOMP Message Broker
         ├── /topic/system                 (Broadcasts)
         └── /user/{id}/queue/messages     (Private Chat)
         └── /user/{id}/queue/message-reactions
         └── /user/{id}/queue/notifications (Push Notifications)
```

---

## ⚙️ Environment & Configuration

Configure `application.properties` or `application-local.properties`:

```properties
# Server Port
server.port=8080

# PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/socialdb
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true

# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT Security
jwt.secret=your_super_secret_jwt_key_at_least_256_bits_long
jwt.access-token-expiration=86400000
jwt.refresh-token-expiration=604800000

# Cloudinary Storage
cloudinary.cloud-name=your_cloud_name
cloudinary.api-key=your_api_key
cloudinary.api-secret=your_api_secret

# Mail Service (Optional)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
```

---

## 🚀 Quick Start & Installation

### 1. Prerequisites
* **Java**: OpenJDK 21 or Oracle JDK 21
* **Database**: PostgreSQL 15+ & Redis 6+
* **Build Tool**: Maven 3.8+
* **Docker** (Optional, for instant infrastructure setup)

### 2. Run Infrastructure with Docker Compose
```bash
# Start PostgreSQL and Redis containers
docker compose up -d
```

### 3. Build & Run Application
```bash
# Compile and package application
./mvnw clean compile

# Run Spring Boot application
./mvnw spring-boot:run
```

The server will start at: `http://localhost:8080`

### 4. Explore Interactive API Docs
Open your browser and navigate to:
```
http://localhost:8080/swagger-ui/index.html
```

---

## 🧪 Testing & Quality Assurance

The codebase includes automated unit and integration tests using **JUnit 5** and **Mockito**:

```bash
# Run all unit tests
./mvnw test
```

---

## 👨‍💻 Author & License

* **Developer**: Nguyễn Duy Thành
* **Email**: contact / portfolio
* **License**: Licensed under the **[MIT License](LICENSE)**.
