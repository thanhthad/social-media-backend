package media.social.modules.conversation.repository;

import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.auth.Enum.Status;
import media.social.modules.conversation.dto.projection.MessageProjection;
import media.social.modules.conversation.dto.projection.MessageReactionUserProjection;
import media.social.modules.conversation.dto.projection.UnreadCountProjection;
import media.social.modules.conversation.entity.*;
import media.social.modules.conversation.enums.ConversationType;
import media.social.modules.post.enums.ReactionType;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migration & Query Integration Test cho {@link ConversationMemberRepository},
 * {@link MessageRepository}, và {@link MessageReactionRepository} sử dụng PostgreSQL Testcontainers.
 *
 * <p>Kiểm thử:
 * <ul>
 *     <li>Migration test (Flyway V1: uk_user_message_reaction, conversation_members composite PK)</li>
 *     <li>findPrivateConversation (Truy vấn tìm cuộc trò chuyện riêng tư chính xác 2 thành viên)</li>
 *     <li>countUnreadMessages & countUnreadMessagesByConversationIds (Tính số tin nhắn chưa đọc dựa trên lastReadMessageId)</li>
 *     <li>findMessages (Phân trang danh sách tin nhắn theo thời gian giảm dần)</li>
 *     <li>MessageReaction (Thả cảm xúc tin nhắn, uk_user_message_reaction ngăn trùng lặp)</li>
 * </ul>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Message & Member – Integration Tests với PostgreSQL Container")
class MessageAndMemberRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("social_test_msg_member_db")
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

    @Autowired private ConversationMemberRepository memberRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private MessageReactionRepository messageReactionRepository;
    @Autowired private TestEntityManager em;

    private User alice;
    private User bob;
    private Conversation conv;
    private Message msg1;
    private Message msg2;

    @BeforeEach
    void setUp() {
        alice = createUser("alice", "alice@example.com");
        bob = createUser("bob", "bob@example.com");

        createProfile(alice, "Alice Nguyen");
        createProfile(bob, "Bob Tran");

        conv = em.persist(Conversation.builder()
                .type(ConversationType.PRIVATE)
                .owner(alice)
                .lastMessageAt(OffsetDateTime.now())
                .build());

        msg1 = em.persist(Message.builder()
                .conversation(conv)
                .sender(bob)
                .content("Message 1 from Bob")
                .deleted(false)
                .build());

        msg2 = em.persist(Message.builder()
                .conversation(conv)
                .sender(bob)
                .content("Message 2 from Bob")
                .deleted(false)
                .build());

        // Alice has read up to msg1
        em.persist(ConversationMember.builder()
                .id(new ConversationMemberId(conv.getId(), alice.getId()))
                .conversation(conv)
                .user(alice)
                .lastReadMessage(msg1)
                .build());

        // Bob has read up to msg2
        em.persist(ConversationMember.builder()
                .id(new ConversationMemberId(conv.getId(), bob.getId()))
                .conversation(conv)
                .user(bob)
                .lastReadMessage(msg2)
                .build());

        // Reaction on msg1 by alice
        em.persist(MessageReaction.builder()
                .message(msg1)
                .user(alice)
                .type(ReactionType.LOVE)
                .build());

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 1. MIGRATION & REACTION TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Migration Test (uk_user_message_reaction)")
    class MigrationTests {

        @Test
        @DisplayName("Flyway V1: uk_user_message_reaction ngăn 1 user react 2 lần trên 1 tin nhắn")
        void flyway_uniqueReactionOnMessage() {
            MessageReaction duplicate = MessageReaction.builder()
                    .message(msg1)
                    .user(alice)
                    .type(ReactionType.LIKE)
                    .build();

            assertThatThrownBy(() -> {
                em.persist(duplicate);
                em.flush();
            }).hasMessageContaining("uk_user_message_reaction");
        }
    }

    // =========================================================================
    // 2. CONVERSATION MEMBER TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. ConversationMemberRepository Queries")
    class MemberQueriesTests {

        @Test
        @DisplayName("findPrivateConversation: Tìm chính xác cuộc trò chuyện riêng tư chỉ gồm 2 thành viên")
        void findPrivateConversation_exactMatch() {
            Optional<Conversation> found = memberRepository.findPrivateConversation(alice.getId(), bob.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(conv.getId());

            // Đảo ngược thứ tự tham số vẫn tìm được
            Optional<Conversation> reverseFound = memberRepository.findPrivateConversation(bob.getId(), alice.getId());
            assertThat(reverseFound).isPresent();
        }

        @Test
        @DisplayName("countUnreadMessagesByConversationIds: GROUP BY conversation tính số tin nhắn chưa đọc")
        void countUnreadMessagesByConversationIds_accurate() {
            // Bob đã gửi msg1 và msg2. Alice mới đọc msg1 -> còn 1 tin nhắn chưa đọc (msg2)
            List<UnreadCountProjection> unreadList = memberRepository.countUnreadMessagesByConversationIds(
                    alice.getId(), List.of(conv.getId()));

            assertThat(unreadList).hasSize(1);
            assertThat(unreadList.get(0).getConversationId()).isEqualTo(conv.getId());
            assertThat(unreadList.get(0).getUnreadCount()).isEqualTo(1L);
        }
    }

    // =========================================================================
    // 3. MESSAGE REPOSITORY TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. MessageRepository Queries")
    class MessageQueriesTests {

        @Test
        @DisplayName("countUnreadMessages: Đếm số lượng tin nhắn chưa đọc của user trong 1 phòng chat")
        void countUnreadMessages_singleRoom() {
            long unread = messageRepository.countUnreadMessages(conv.getId(), alice.getId());
            assertThat(unread).isEqualTo(1L);

            // Bob đã đọc hết -> 0
            long bobUnread = messageRepository.countUnreadMessages(conv.getId(), bob.getId());
            assertThat(bobUnread).isEqualTo(0L);
        }

        @Test
        @DisplayName("findMessages: Phân trang danh sách tin nhắn giảm dần theo thời gian (createdAt DESC)")
        void findMessages_pagination() {
            Page<MessageProjection> page = messageRepository.findMessages(conv.getId(), PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(2);
            // msg2 mới hơn msg1
            assertThat(page.getContent().get(0).getContent()).isEqualTo("Message 2 from Bob");
            assertThat(page.getContent().get(1).getContent()).isEqualTo("Message 1 from Bob");
        }

        @Test
        @DisplayName("MessageReaction: findUsersReacted lấy danh sách người thả cảm xúc trên tin nhắn")
        void messageReaction_findUsersReacted() {
            List<MessageReactionUserProjection> reactions = messageReactionRepository.findUsersReacted(msg1.getId());

            assertThat(reactions).hasSize(1);
            assertThat(reactions.get(0).getUserName()).isEqualTo("alice");
            assertThat(reactions.get(0).getReactionType()).isEqualTo(ReactionType.LOVE);
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

    private void createProfile(User user, String fullName) {
        em.persist(Profile.builder()
                .user(user)
                .fullName(fullName)
                .avatarUrl("https://avatar.com/" + user.getUsername() + ".png")
                .dateOfBirth(LocalDate.of(1996, 1, 1))
                .build());
    }
}
