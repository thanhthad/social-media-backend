package media.social.modules.conversation.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.Status;
import media.social.modules.conversation.dto.projection.ConversationListProjection;
import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.entity.ConversationMember;
import media.social.modules.conversation.entity.ConversationMemberId;
import media.social.modules.conversation.entity.Message;
import media.social.modules.conversation.enums.ConversationType;
import media.social.modules.user.entity.Profile;
import media.social.modules.user.entity.User;
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
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration & Query Integration Test cho {@link ConversationRepository} sử dụng PostgreSQL Testcontainers.
 *
 * <p>Kiểm thử:
 * <ul>
 *     <li>Migration test (Flyway V1: conversations, conversation_members, messages)</li>
 *     <li>findByIdWithOwner (LEFT JOIN FETCH owner và lastMessage)</li>
 *     <li>findConversationList (Query phức tạp với CASE WHEN, COALESCE tên/avatar của người chat cùng, COUNT media preview, GROUP BY, ORDER BY lastMessageAt DESC NULLS LAST)</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ConversationRepository – Integration Tests với PostgreSQL Container")
class ConversationRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:16-3.4-alpine").asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("social_test_conv_db")
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

    @Autowired private ConversationRepository conversationRepository;
    @Autowired private TestEntityManager em;

    private User alice;
    private User bob;
    private User charlie;
    private Conversation privateConv;
    private Conversation groupConv;
    private Message lastMsg;

    @BeforeEach
    void setUp() {
        alice = createUser("alice", "alice@example.com");
        bob = createUser("bob", "bob@example.com");
        charlie = createUser("charlie", "charlie@example.com");

        createProfile(alice, "Alice Nguyen", "https://avatar.com/alice.png");
        createProfile(bob, "Bob Tran", "https://avatar.com/bob.png");
        createProfile(charlie, "Charlie Le", "https://avatar.com/charlie.png");

        // 1. Private Conversation giữa alice và bob
        privateConv = em.persist(Conversation.builder()
                .type(ConversationType.PRIVATE)
                .owner(alice)
                .lastMessageAt(OffsetDateTime.now().minusMinutes(10))
                .build());

        addMember(privateConv, alice);
        addMember(privateConv, bob);

        lastMsg = em.persist(Message.builder()
                .conversation(privateConv)
                .sender(bob)
                .content("Hey Alice!")
                .build());

        privateConv.setLastMessage(lastMsg);
        em.persist(privateConv);

        // 2. Group Conversation
        groupConv = em.persist(Conversation.builder()
                .type(ConversationType.GROUP)
                .owner(alice)
                .name("Dev Team Group")
                .avatarUrl("https://avatar.com/group.png")
                .lastMessageAt(OffsetDateTime.now().minusMinutes(2))
                .build());

        addMember(groupConv, alice);
        addMember(groupConv, bob);
        addMember(groupConv, charlie);

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 1. MIGRATION & FETCH TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Migration & Fetch Tests")
    class MigrationAndFetchTests {

        @Test
        @DisplayName("Flyway V1: Khởi tạo bảng conversations thành công")
        void flyway_conversationsTableReady() {
            assertThat(conversationRepository.count()).isGreaterThanOrEqualTo(2L);
        }

        @Test
        @DisplayName("findByIdWithOwner: LEFT JOIN FETCH nạp sẵn owner và lastMessage")
        void findByIdWithOwner_eagerlyLoadsEntities() {
            Optional<Conversation> opt = conversationRepository.findByIdWithOwner(privateConv.getId());

            assertThat(opt).isPresent();
            Conversation conv = opt.get();
            assertThat(conv.getOwner()).isNotNull();
            assertThat(conv.getOwner().getUsername()).isEqualTo("alice");
            assertThat(conv.getLastMessage()).isNotNull();
            assertThat(conv.getLastMessage().getContent()).isEqualTo("Hey Alice!");
        }
    }

    // =========================================================================
    // 2. CONVERSATION LIST QUERY TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. findConversationList Query Tests")
    class ConversationListTests {

        @Test
        @DisplayName("findConversationList: Hiển thị đúng tên đối phương trong Private chat và tên nhóm trong Group chat")
        void findConversationList_displaysCorrectNames() {
            // Alice xem danh sách cuộc trò chuyện của mình
            List<ConversationListProjection> list = conversationRepository.findConversationList(
                    alice.getId(),
                    ConversationType.PRIVATE,
                    ConversationType.DATING // Loại trừ dating
            );

            assertThat(list).hasSize(2);

            // Do ORDER BY lastMessageAt DESC: groupConv (2 mins ago) trước, privateConv (10 mins ago) sau
            ConversationListProjection firstItem = list.get(0);
            assertThat(firstItem.getDisplayName()).isEqualTo("Dev Team Group");
            assertThat(firstItem.getType()).isEqualTo(ConversationType.GROUP);

            ConversationListProjection secondItem = list.get(1);
            // Trong Private chat của Alice: tên hiển thị phải là đối phương (Bob Tran)
            assertThat(secondItem.getDisplayName()).isEqualTo("Bob Tran");
            assertThat(secondItem.getAvatarUrl()).isEqualTo("https://avatar.com/bob.png");
            assertThat(secondItem.getPreview()).isEqualTo("Hey Alice!");
        }

        @Test
        @DisplayName("findConversationList: Loại trừ hoàn toàn loại cuộc trò chuyện excludedType (DATING)")
        void findConversationList_excludesType() {
            // Tạo cuộc trò chuyện DATING
            Conversation datingConv = em.persist(Conversation.builder()
                    .type(ConversationType.DATING)
                    .owner(alice)
                    .lastMessageAt(OffsetDateTime.now())
                    .build());
            addMember(datingConv, alice);
            em.flush();
            em.clear();

            List<ConversationListProjection> list = conversationRepository.findConversationList(
                    alice.getId(),
                    ConversationType.PRIVATE,
                    ConversationType.DATING
            );

            List<ConversationType> types = list.stream().map(ConversationListProjection::getType).toList();
            assertThat(types).doesNotContain(ConversationType.DATING);
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private User createUser(String username, String email) {
        return em.persist(User.builder()
                .username(username)
                .email(email)
                .passwordHash("$2a$10$dummyHash")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .status(Status.ACTIVE)
                .build());
    }

    private void createProfile(User user, String fullName, String avatarUrl) {
        em.persist(Profile.builder()
                .user(user)
                .fullName(fullName)
                .avatarUrl(avatarUrl)
                .dateOfBirth(LocalDate.of(1996, 1, 1))
                .build());
    }

    private void addMember(Conversation conversation, User user) {
        em.persist(ConversationMember.builder()
                .id(new ConversationMemberId(conversation.getId(), user.getId()))
                .conversation(conversation)
                .user(user)
                .build());
    }
}
