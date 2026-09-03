package media.social.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Master Migration Integration Test kiểm thử toàn diện quy trình database migrations
 * từ V1 đến V11 với PostgreSQL Testcontainers (PostGIS).
 *
 * <p>Kiểm thử:
 * <ul>
 *     <li>Flyway execution: toàn bộ 11 script migrations chạy thành công 100%</li>
 *     <li>PostgreSQL extensions: pg_trgm, pg_stat_statements, postgis đều được cài đặt</li>
 *     <li>Database tables: Kiểm tra sự tồn tại của hơn 30 bảng nghiệp vụ</li>
 *     <li>GIN indexes: Kiểm tra index hỗ trợ fuzzy search idx_users_username_trgm và idx_posts_content_trgm</li>
 *     <li>Seeded master data: Kiểm tra dữ liệu khởi tạo của roles (V2) và dating_interests (V8)</li>
 *     <li>Data types & Constraints: Cột JSONB, check constraints, unique constraints</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Database Migration Master Integration Test – Kiểm thử toàn diện Schema & Flyway V1..V11")
class DatabaseMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgis/postgis:16-3.4-alpine")
                    .withDatabaseName("social_master_migration_db")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // =========================================================================
    // 1. FLYWAY MIGRATION STATUS TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Flyway Migration Execution Tests")
    class FlywayExecutionTests {

        @Test
        @DisplayName("Tất cả 11 script migration (V1 -> V11) đã được thực thi và có trạng thái SUCCESS")
        void allMigrationsAppliedSuccessfully() {
            MigrationInfo[] appliedMigrations = flyway.info().applied();

            assertThat(appliedMigrations).hasSize(11);

            for (MigrationInfo info : appliedMigrations) {
                assertThat(info.getState().isApplied()).isTrue();
                assertThat(info.getState().isFailed()).isFalse();
            }

            // Kiểm tra các script cốt lõi
            List<String> scriptDescriptions = Arrays.stream(appliedMigrations)
                    .map(MigrationInfo::getDescription)
                    .toList();

            assertThat(scriptDescriptions).contains(
                    "create tables",
                    "insert roles",
                    "enable pg extensions",
                    "create trgm indexes",
                    "create dating tables",
                    "seed dating interests",
                    "create story tables",
                    "create reel details table",
                    "alter reel details thumbnail public id nullable"
            );
        }
    }

    // =========================================================================
    // 2. EXTENSIONS VERIFICATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. PostgreSQL Extensions Verification (V5 & V8)")
    class ExtensionVerificationTests {

        @Test
        @DisplayName("Extensions pg_trgm, pg_stat_statements và postgis đã được kích hoạt thành công")
        void requiredExtensionsInstalled() {
            List<String> installedExtensions = jdbcTemplate.queryForList(
                    "SELECT extname FROM pg_extension", String.class);

            assertThat(installedExtensions).contains(
                    "pg_trgm",
                    "pg_stat_statements",
                    "postgis"
            );
        }
    }

    // =========================================================================
    // 3. TABLE EXISTENCE VERIFICATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. Database Tables Verification (Hơn 30 bảng nghiệp vụ)")
    class TableVerificationTests {

        @Test
        @DisplayName("Tất cả các bảng trong hệ thống tồn tại đầy đủ trên database")
        void allTablesExistInSchema() {
            List<String> tables = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                    String.class
            );

            assertThat(tables).contains(
                    // User Module
                    "users", "roles", "user_roles", "profiles", "friendships", "blocks",
                    // Post Module
                    "posts", "post_media", "hashtags", "post_hashtags", "saved_posts",
                    "comments", "reactions", "comment_reactions", "reports",
                    // Conversation Module
                    "conversations", "conversation_members", "messages", "message_media", "message_reactions",
                    // Auth Module
                    "email_verification_tokens", "password_reset_tokens", "refresh_tokens",
                    // Notification Module
                    "notifications",
                    // Dating Module
                    "dating_profiles", "dating_preferences", "dating_matches", "dating_swipes",
                    "dating_reports", "dating_interests", "dating_profile_interests", "dating_profile_photos",
                    // Story Module
                    "stories", "story_media", "story_views", "story_reactions",
                    // Reel Module
                    "reel_details", "reel_views"
            );
        }
    }

    // =========================================================================
    // 4. GIN INDEXES VERIFICATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("4. GIN Trigram Indexes Verification (V6)")
    class GinIndexVerificationTests {

        @Test
        @DisplayName("GIN index idx_users_username_trgm và idx_posts_content_trgm tồn tại để tối ưu tìm kiếm mờ")
        void ginIndexesExist() {
            List<String> indexNames = jdbcTemplate.queryForList(
                    "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'",
                    String.class
            );

            assertThat(indexNames).contains(
                    "idx_users_username_trgm",
                    "idx_posts_content_trgm"
            );
        }
    }

    // =========================================================================
    // 5. SEEDED DATA VERIFICATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("5. Seeded Data Verification (V2 Roles & V8 Dating Interests)")
    class SeededDataTests {

        @Test
        @DisplayName("Flyway V2: Đã seed đầy đủ các Role hệ thống")
        void systemRolesSeeded() {
            List<String> roleNames = jdbcTemplate.queryForList(
                    "SELECT name FROM roles", String.class);

            assertThat(roleNames).contains("USER", "ADMIN", "MODERATOR");
        }

        @Test
        @DisplayName("Flyway V8: Đã seed các sở thích Dating cơ bản")
        void datingInterestsSeeded() {
            List<String> interestNames = jdbcTemplate.queryForList(
                    "SELECT name FROM dating_interests", String.class);

            assertThat(interestNames).contains(
                    "Du lịch",
                    "Âm nhạc",
                    "Đọc sách",
                    "Nấu ăn",
                    "Gym / Thể thao",
                    "Chơi game"
            );
        }
    }

    // =========================================================================
    // 6. COLUMN TYPES & CONSTRAINTS TESTS
    // =========================================================================

    @Nested
    @DisplayName("6. Column Types & Constraints Verification")
    class ColumnTypeAndConstraintTests {

        @Test
        @DisplayName("Cột social_links trong bảng profiles có kiểu dữ liệu jsonb")
        void socialLinksIsJsonb() {
            String dataType = jdbcTemplate.queryForObject(
                    """
                    SELECT data_type
                    FROM information_schema.columns
                    WHERE table_name = 'profiles'
                      AND column_name = 'social_links'
                    """,
                    String.class
            );

            assertThat(dataType).isEqualTo("jsonb");
        }

        @Test
        @DisplayName("Các ràng buộc quan trọng uk_friendship_pair, chk_posts_post_type tồn tại")
        void databaseConstraintsExist() {
            List<String> constraints = jdbcTemplate.queryForList(
                    """
                    SELECT constraint_name
                    FROM information_schema.table_constraints
                    WHERE table_schema = 'public'
                    """,
                    String.class
            );

            assertThat(constraints).contains(
                    "uk_friendship_pair",
                    "chk_friendship_different_users",
                    "chk_posts_post_type",
                    "chk_posts_visibility",
                    "uk_report_user_post",
                    "uk_match_pair"
            );
        }
    }
}
