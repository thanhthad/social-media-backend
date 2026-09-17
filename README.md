# 🚀 Social Media & Dating Platform — Backend (Spring Boot 3 + Java 21)

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2+-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6.2+-green.svg?style=flat-square&logo=springsecurity)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-Cache_%26_RateLimit-DC382D.svg?style=flat-square&logo=redis)](https://redis.io/)
[![Flyway](https://img.shields.io/badge/Flyway-Migration-CC0200.svg?style=flat-square&logo=flyway)](https://flywaydb.org/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED.svg?style=flat-square&logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

Hệ thống Backend mạng xã hội & hẹn hò (Social Media & Matchmaking Dating Platform) hiệu năng cao, xây dựng trên nền tảng **Java 21**, **Spring Boot 3**, **PostgreSQL 17**, và **Redis**. Áp dụng các nguyên lý kỹ thuật phần mềm hiện đại: **Domain-Driven Design (DDD)**, **Modular Monolith Architecture**, **STOMP WebSocket Real-Time Pub/Sub**, **JWT Dual-Token Security**, và **Redis Distributed Rate Limiting**.

---

## 📑 Mục Lục

- [🏛️ Kiến Trúc Hệ Thống (System Architecture)](#️-kiến-trúc-hệ-thống-system-architecture)
- [📖 Tài Liệu API & Swagger UI](#-tài-liệu-api--swagger-ui)
- [🧭 Danh Mục 30 Controllers & 159 Endpoints](#-danh-mục-30-controllers--159-endpoints)
- [🛡️ Xác Thực & Chế Độ Khách (Security & Guest Mode)](#️-xác-thực--chế-độ-khách-security--guest-mode)
- [⚡ Redis Caching & Rate Limiting Phân Tán](#-redis-caching--rate-limiting-phân-tán)
- [🗄️ Thiết Kế Cơ Sở Dữ Liệu & ERD](#️-thiết-kế-cơ-sở-dữ-liệu--erd)
- [📡 Hạ Tầng Real-Time STOMP WebSocket](#-hạ-tầng-real-time-stomp-websocket)
- [⚙️ Cấu Hình Môi Trường (Environment Config)](#️-cấu-hình-môi-trường-environment-config)
- [🚀 Hướng Dẫn Cài Đặt & Chạy Ứng Dụng](#-hướng-dẫn-cài-đặt--chạy-ứng-dụng)
- [🧪 Kiểm Thử & Đảm Bảo Chất Lượng](#-kiểm-thử--đảm-bảo-chất-lượng)

---

## 🏛️ Kiến Trúc Hệ Thống (System Architecture)

Backend được tổ chức theo mô hình **Modular Monolith**, phân chia rõ ràng giữa tầng dùng chung (common), hạ tầng (infrastructure) và các miền nghiệp vụ (modules):

```
media.social
├── common                      # Xử lý lỗi toàn cục, response wrapper, rate limiting
│   ├── exception               # GlobalExceptionHandler, AppExceptions
│   ├── ratelimit               # AOP Annotation @RateLimit backed by Redis
│   └── response                # Standardized ResponseData<T> wrapper
├── infrastructure              # Cấu hình kỹ thuật nền tảng & tích hợp bên thứ ba
│   ├── cloudinary              # Lưu trữ & xử lý media (ảnh, video)
│   ├── redis                   # RedisTemplate, CacheManager, Key Generators
│   ├── security                # SecurityFilterChain, JWT Token Provider, UserContextHolder
│   └── websocket               # STOMP WebSocket, ChannelInterceptor, Handshake Handler
└── modules                     # 7 Domain Nghiệp Vụ Chính
    ├── auth                    # Đăng nhập, đăng ký, refresh token, OTP xác thực
    ├── user                    # Hồ sơ người dùng, quan hệ bạn bè, follow, chặn, thống kê
    ├── post                    # Bảng tin, bài viết, media, hashtags, báo cáo vi phạm
    ├── conversation            # Chat 1-1, chat nhóm, cảm xúc tin nhắn, đã đọc
    ├── dating                  # Hồ sơ hẹn hò, quẹt match, sở thích, bộ lọc cự ly/tuổi
    ├── story                   # Tin 24h, theo dõi người xem, thả tim story
    └── notification            # Thông báo thời gian thực & STOMP push
```

---

## 📖 Tài Liệu API & Swagger UI

Toàn bộ hệ thống cung cấp **159 REST Endpoints** được tài liệu hóa chi tiết qua SpringDoc OpenAPI 3:

<img width="1902" height="868" alt="image" src="https://github.com/user-attachments/assets/91a7d0ab-b62a-4da6-abad-c3591117ba43" />

* **Swagger UI Trực Quan**: `http://localhost:8080/swagger-ui/index.html`
* **OpenAPI Schema (JSON)**: `http://localhost:8080/v3/api-docs`

---

## 🧭 Danh Mục 30 Controllers & 159 Endpoints

| Miền Nghiệp Vụ | Controller | Base Path | Mô Tả & Chức Năng Cốt Lõi |
| :--- | :--- | :--- | :--- |
| **Xác thực** | `AuthController` | `/api/auth` | Đăng ký, đăng nhập, refresh token, đăng xuất, gửi OTP xác thực email, đổi mật khẩu quên |
| **Người dùng** | `UserController` | `/api/users` | CRUD thông tin cá nhân, cập nhật thông tin cơ bản/liên hệ/nghề nghiệp, đổi avatar/ảnh bìa, đổi username/password |
| | `FriendshipController` | `/api/friends` | Gửi/chấp nhận/từ chối lời mời kết bạn, hủy kết bạn, danh sách lời mời chờ, đếm bạn chung |
| | `FollowController` | `/api/follows` | Theo dõi, bỏ theo dõi người dùng, danh sách followers và following |
| | `BlockController` | `/api/blocks` | Chặn, bỏ chặn, kiểm tra trạng thái chặn 2 chiều, danh sách bị chặn |
| **Bài viết** | `PostController` | `/api/posts` | Bảng tin cá nhân, khám phá, bảng tin công khai cho khách, CRUD bài viết, thêm/xóa media |
| | `PostReactionController` | `/api/posts` | 6 loại cảm xúc (`LIKE`, `LOVE`, `HAHA`, `WOW`, `SAD`, `ANGRY`), đếm lượt & danh sách người thả tim |
| | `CommentController` | `/api/comments` | Bình luận gốc, phản hồi lồng nhau (tree), chỉnh sửa bình luận (`PATCH /{id}`), xóa bình luận |
| | `CommentReactionController` | `/api/comments` | Thả cảm xúc trên bình luận, gỡ cảm xúc, danh sách người thả cảm xúc |
| | `SavedPostController` | `/api/posts` | Lưu bài viết vào bộ sưu tập, bỏ lưu, kiểm tra trạng thái đã lưu |
| | `HashtagController` | `/api/hashtags` | Xu hướng hashtag thịnh hành, tìm kiếm bài viết theo hashtag |
| | `ReportController` | `/api/reports` | Báo cáo bài viết vi phạm tiêu chuẩn cộng đồng, Admin kiểm duyệt & ẩn bài |
| **Reels Video** | `ReelController` | `/api/reels` | Feed Reels theo dõi/khám phá/công khai/của tôi, tải lên video dọc, sửa caption & quyền riêng tư (`PATCH /{id}`), tracking tiến độ xem & replay |
| **Tin 24h** | `StoryController` | `/api/stories` | Đăng tin ảnh/video 24h, feed tin bạn bè/công khai, xem ai đã xem tin, thả tim tin, cập nhật quyền riêng tư (`PATCH /{id}/visibility`), xóa tin |
| **Trò chuyện** | `ConversationController` | `/api/conversations` | Hội thoại 1-1, chat nhóm, đổi ảnh đại diện nhóm (`PUT`), đổi tên nhóm (`PUT`), xóa cuộc trò chuyện |
| | `ConversationMemberController` | `/api/conversation-members` | Quản lý thành viên nhóm chat, thêm/xóa thành viên, cập nhật con trỏ tin nhắn đã đọc (`PUT`) |
| | `MessageController` | `/api/messages` | Gửi tin nhắn văn bản & media, tải lịch sử hội thoại, xóa tin nhắn |
| | `MessageReactionController` | `/api/message-reactions` | Thả cảm xúc tin nhắn thời gian thực, gỡ cảm xúc, danh sách người thả |
| **Hẹn hò** | `DatingProfileController` | `/api/dating` | Tạo/xem/cập nhật hồ sơ hẹn hò, cập nhật tọa độ GPS, sửa từng trường riêng lẻ (`PATCH /profile/field`) |
| | `DatingDiscoveryController` | `/api/dating` | Thuật toán khám phá đối tượng theo bán kính GPS (km), độ tuổi và giới tính |
| | `DatingSwipeController` | `/api/dating` | Quẹt `LIKE` / `DISLIKE`, kiểm tra match tức thì, hoàn tác quẹt trước |
| | `DatingMatchController` | `/api/dating` | Danh sách các cặp đã tương hợp (match), hủy tương hợp (unmatch) |
| | `DatingPreferenceController` | `/api/dating` | Cài đặt tiêu chí: độ tuổi min-max, khoảng cách tối đa, giới tính ưu tiên (`PUT /preferences`) |
| | `DatingInterestController` | `/api/dating` | Danh mục sở thích chuẩn, cập nhật sở thích người dùng (`PUT /interests`) |
| | `DatingProfilePhotoController` | `/api/dating` | Tải lên bộ sưu tập ảnh hẹn hò, đặt ảnh đại diện chính (`PATCH /photos/{id}/primary`), xóa ảnh |
| | `DatingReportController` | `/api/dating/reports` | Báo cáo hồ sơ hẹn hò vi phạm, kiểm duyệt & xử lý |
| **Thông báo** | `NotificationController` | `/api/notifications` | Danh sách thông báo, lọc chưa đọc, đánh dấu đã đọc (`PATCH /{id}/read`), đọc tất cả (`PATCH /read-all`), xóa |
| **Quản trị** | `AdminUserController` | `/api/admin/users` | Quản lý tài khoản, tìm kiếm, khóa/mở khóa/kích hoạt trạng thái (`PATCH /{id}/status`) |
| | `AdminUserRoleController` | `/api/admin/users` | Phân quyền vai trò (`ROLE_ADMIN`, `ROLE_MODERATOR`, `ROLE_USER`), gỡ quyền |

---

## 🛡️ Xác Thực & Chế Độ Khách (Security & Guest Mode)

* **Cơ Chế Token Kép (JWT Dual-Token)**:
  * Access Token: thời hạn ngắn (24h), ký bảo mật HMAC-SHA256.
  * Refresh Token: thời hạn 7 ngày, hỗ trợ cơ chế luân chuyển (token rotation).
* **Thu Hồi Token Tức Thì (Token Blacklist)**: Khi đăng xuất, token được đưa vào Redis blocklist với TTL tự hủy.
* **Chế Độ Khách Công Khai (Guest Public Access - `SecurityConfig.java`)**:
  * Cho phép người dùng chưa đăng nhập tự do xem:
    * `GET /api/posts/public/**` (Bảng tin bài viết công khai)
    * `GET /api/reels/public/**` (Bảng tin video ngắn Reels công khai)
    * `GET /api/hashtags/**` (Các hashtag đang thịnh hành)
    * `GET /api/comments/post/**` & `GET /api/comments/*/replies` (Đọc bình luận công khai không bị lỗi 401)
* **Ngữ Cảnh Luồng (Thread Context)**: `UserContextHolder` trích xuất an toàn `userId` và quyền hạn từ `SecurityContext`.

---

## ⚡ Redis Caching & Rate Limiting Phân Tán

### 1. Cơ Chế Bộ Nhớ Đệm (Caching)
* Lưu trữ tạm các dữ liệu truy xuất tần suất cao: Thông tin User, chỉ số tương tác bài viết, danh sách người xem Story, điểm hot score Reels.
* Tự động xóa cache tương ứng khi có thao tác ghi hoặc chỉnh sửa.

### 2. Giới Hạn Tần Suất Gọi Phân Tán (`@RateLimit`)
Bảo vệ các endpoint nhạy cảm chống spam và tấn công brute-force bằng thuật toán Token Bucket lưu trên Redis:

```java
@RateLimit(name = "AUTH_LOGIN", limit = 10, windowSeconds = 60)
@PostMapping("/login")
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) { ... }
```

---

## 🗄️ Thiết Kế Cơ Sở Dữ Liệu & ERD

Hệ thống quản lý phiên bản database bằng **Flyway Database Migration**:

```
src/main/resources/db/migration/
├── V1__create_tables.sql              # Bảng cốt lõi: users, posts, comments, reactions, friendships
├── V2__insert_roles.sql               # Quyền mặc định: ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR
├── V3__insert_fake_datas.sql          # Dữ liệu mẫu ban đầu
├── V4__update_chat_fields.sql         # Hội thoại chat, tin nhắn, con trỏ đã đọc
├── V5__create_dating_tables.sql       # Hồ sơ hẹn hò, swipes, matches, preferences
├── V6__create_story_tables.sql        # Tin 24h, người xem, phản hồi story
└── V10__create_reel_details_table.sql # Metadata video Reel, lượt xem, xếp hạng hot score
```

### Sơ Đồ Thực Thể Quan Hệ (ERD):

<img width="2604" height="1991" alt="DBdiagram" src="https://github.com/user-attachments/assets/098c984a-e939-475d-84c8-447ef2f3681d" />

---

## 📡 Hạ Tầng Real-Time STOMP WebSocket

Được cấu hình trong `WebSocketConfig.java` với endpoint kết nối `/ws` (hỗ trợ SockJS fallback):

* **Bắt tay kết nối**: Xác thực JWT token từ connection headers trước khi cho phép vào kênh.
* **Kênh gửi nhận tin nhắn (Destinations)**:
  * `/user/{userId}/queue/messages` — Nhận tin nhắn chat riêng tư & chat nhóm tức thì
  * `/user/{userId}/queue/message-reactions` — Đồng bộ thả/gỡ cảm xúc trên tin nhắn
  * `/user/{userId}/queue/notifications` — Nhận thông báo đẩy (thả tim, bình luận, kết bạn, match hẹn hò)
  * `/topic/system` — Thông báo phát sóng toàn hệ thống

---

## ⚙️ Cấu Hình Môi Trường (Environment Config)

Thiết lập trong `application.properties` hoặc biến môi trường hệ thống:

```properties
server.port=8080

# PostgreSQL Database
spring.datasource.url=jdbc:postgresql://localhost:5432/socialdb
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true

# Redis Cache & Rate Limiting
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT Security (Secret tối thiểu 256 bits)
jwt.secret=your_super_secret_jwt_key_at_least_256_bits_long
jwt.access-token-expiration=86400000
jwt.refresh-token-expiration=604800000

# Cloudinary Multi-Media Storage
cloudinary.cloud-name=your_cloud_name
cloudinary.api-key=your_api_key
cloudinary.api-secret=your_api_secret
```

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy Ứng Dụng

### 1. Yêu Cầu Môi Trường
* **Java**: OpenJDK 21 hoặc Oracle JDK 21
* **Database**: PostgreSQL 15+ & Redis 6+
* **Build Tool**: Maven 3.8+ (hoặc dùng sẵn wrapper `./mvnw`)

### 2. Khởi Động Database & Redis bằng Docker Compose
```bash
docker compose up -d
```

### 3. Biên Dịch & Khởi Động Server
```bash
# Biên dịch mã nguồn
./mvnw clean compile

# Chạy ứng dụng Spring Boot
./mvnw spring-boot:run
```

Server sẽ khởi chạy tại: `http://localhost:8080`.

---

## 🧪 Kiểm Thử & Đảm Bảo Chất Lượng

Chạy toàn bộ unit tests và integration tests:

```bash
./mvnw test
```

---

## 👨‍💻 Tác Giả & Bản Quyền

* **Tác giả**: Nguyễn Duy Thành
* **Bản quyền**: Được phát hành theo giấy phép **[MIT License](LICENSE)**.
