package media.social.modules.auth.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.Status;
import media.social.modules.auth.entity.EmailVerificationToken;
import media.social.modules.auth.entity.PasswordResetToken;
import media.social.modules.auth.entity.RefreshToken;
import media.social.modules.user.entity.User;
import media.social.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migration & Query Integration Test cho các Repository của Auth module:
 * {@link EmailVerificationTokenRepository}, {@link PasswordResetTokenRepository}, {@link RefreshTokenRepository}
 * sử dụng PostgreSQL Testcontainers.
 *
 * <p>Kiểm thử:
 * <ul>
 *     <li>Migration test (Flyway V1: email_verification_tokens, password_reset_tokens, refresh_tokens, ON DELETE CASCADE)</li>
 *     <li>EmailVerificationToken: findByToken, deleteByUser</li>
 *     <li>PasswordResetToken: findByTokenWithUser (JOIN FETCH prt.user), deleteByUser</li>
 *     <li>RefreshToken: findFirstByUserIdAndRevokedFalseAndExpiredAtAfterOrderByExpiredAtDesc (complex derived query)</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Auth Token Repositories – Integration Tests với PostgreSQL Container")
class AuthTokenRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("social_test_auth_db")
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

    @Autowired private EmailVerificationTokenRepository emailTokenRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TestEntityManager em;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = em.persist(User.builder()
                .username("alice")
                .email("alice@example.com")
                .passwordHash("$2a$10$hash")
                .provider(AuthProvider.LOCAL)
                .emailVerified(false)
                .status(Status.ACTIVE)
                .build());

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 1. MIGRATION & CASCADE DELETE TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Migration Test (Flyway V1 Schema, Unique Token, ON DELETE CASCADE)")
    class MigrationTests {

        @Test
        @DisplayName("Flyway V1: Token là UNIQUE, không được trùng lặp trong email_verification_tokens")
        void flyway_uniqueTokenConstraint_failsOnDuplicate() {
            EmailVerificationToken token1 = EmailVerificationToken.builder()
                    .user(alice)
                    .token("unique_token_123")
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .used(false)
                    .build();
            em.persist(token1);
            em.flush();

            EmailVerificationToken token2 = EmailVerificationToken.builder()
                    .user(alice)
                    .token("unique_token_123") // Trùng token
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .used(false)
                    .build();

            assertThatThrownBy(() -> {
                em.persist(token2);
                em.flush();
            }).isNotNull();
        }

        @Test
        @DisplayName("Flyway V1: ON DELETE CASCADE tự động dọn dẹp tokens khi User bị xóa")
        void flyway_cascadeDelete_removesAllTokens() {
            createEmailToken(alice, "email_token_abc");
            createPasswordToken(alice, "pwd_token_abc");
            createRefreshToken(alice, "refresh_token_abc", LocalDateTime.now().plusDays(7), false);
            em.flush();
            em.clear();

            // Xóa alice
            userRepository.deleteById(alice.getId());
            em.flush();
            em.clear();

            assertThat(emailTokenRepository.findByToken("email_token_abc")).isEmpty();
            assertThat(passwordResetTokenRepository.findByTokenWithUser("pwd_token_abc")).isEmpty();
            assertThat(refreshTokenRepository.findByToken("refresh_token_abc")).isEmpty();
        }
    }

    // =========================================================================
    // 2. EMAIL VERIFICATION TOKEN TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. EmailVerificationTokenRepository Tests")
    class EmailTokenTests {

        @Test
        @DisplayName("findByToken & deleteByUser hoạt động đúng")
        void emailToken_lifecycle() {
            EmailVerificationToken token = createEmailToken(alice, "verify_token_1");
            em.flush();
            em.clear();

            Optional<EmailVerificationToken> found = emailTokenRepository.findByToken("verify_token_1");
            assertThat(found).isPresent();
            assertThat(found.get().getUser().getId()).isEqualTo(alice.getId());

            emailTokenRepository.deleteByUser(alice);
            em.flush();
            em.clear();

            assertThat(emailTokenRepository.findByToken("verify_token_1")).isEmpty();
        }
    }

    // =========================================================================
    // 3. PASSWORD RESET TOKEN TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. PasswordResetTokenRepository Tests")
    class PasswordResetTokenTests {

        @Test
        @DisplayName("findByTokenWithUser: JOIN FETCH nạp sẵn thực thể User")
        void findByTokenWithUser_eagerUser() {
            createPasswordToken(alice, "reset_token_xyz");
            em.flush();
            em.clear();

            Optional<PasswordResetToken> opt = passwordResetTokenRepository.findByTokenWithUser("reset_token_xyz");
            assertThat(opt).isPresent();
            assertThat(opt.get().getUser()).isNotNull();
            assertThat(opt.get().getUser().getUsername()).isEqualTo("alice");

            passwordResetTokenRepository.deleteByUser(alice);
            em.flush();
            em.clear();

            assertThat(passwordResetTokenRepository.findByTokenWithUser("reset_token_xyz")).isEmpty();
        }
    }

    // =========================================================================
    // 4. REFRESH TOKEN TESTS
    // =========================================================================

    @Nested
    @DisplayName("4. RefreshTokenRepository Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("findFirstByUserIdAndRevokedFalseAndExpiredAtAfterOrderByExpiredAtDesc: Lấy token hợp lệ mới nhất")
        void refreshToken_findValidToken() {
            LocalDateTime now = LocalDateTime.now();

            // Token 1: Đã hết hạn
            createRefreshToken(alice, "token_expired", now.minusHours(1), false);

            // Token 2: Bị thu hồi (revoked = true)
            createRefreshToken(alice, "token_revoked", now.plusDays(1), true);

            // Token 3: Hợp lệ, hết hạn sau 2 ngày
            createRefreshToken(alice, "token_valid_short", now.plusDays(2), false);

            // Token 4: Hợp lệ, hết hạn sau 7 ngày (mới nhất)
            createRefreshToken(alice, "token_valid_latest", now.plusDays(7), false);

            em.flush();
            em.clear();

            Optional<RefreshToken> activeToken = refreshTokenRepository
                    .findFirstByUserIdAndRevokedFalseAndExpiredAtAfterOrderByExpiredAtDesc(alice.getId(), now);

            assertThat(activeToken).isPresent();
            assertThat(activeToken.get().getToken()).isEqualTo("token_valid_latest");
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private EmailVerificationToken createEmailToken(User user, String token) {
        return em.persist(EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build());
    }

    private PasswordResetToken createPasswordToken(User user, String token) {
        return em.persist(PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(2))
                .used(false)
                .build());
    }

    private RefreshToken createRefreshToken(User user, String token, LocalDateTime expiredAt, boolean revoked) {
        return em.persist(RefreshToken.builder()
                .user(user)
                .token(token)
                .expiredAt(expiredAt)
                .revoked(revoked)
                .build());
    }
}
