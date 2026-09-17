package media.social.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.conversation.entity.*;
import media.social.modules.conversation.enums.ConversationType;
import media.social.modules.conversation.repository.*;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.user.entity.User;
import media.social.modules.user.repository.UserRepository;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMockService {
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageMediaRepository messageMediaRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final Faker faker = new Faker(new Locale("vi"));

    @Transactional
    public void init() {
        if (conversationRepository.count() >= 1000) {
            log.info("Conversations already initialized");
            return;
        }

        List<User> users = userRepository.findAll();
        if (users.size() < 2) return;

        log.info("Generating 1000 conversations (Private & Groups with Cloudinary media)...");
        List<Conversation> convsBatch = new ArrayList<>();
        List<ConversationMember> membersBatch = new ArrayList<>();
        List<MessageMedia> mediaBatch = new ArrayList<>();
        List<MessageReaction> reactionsBatch = new ArrayList<>();

        ReactionType[] rTypes = ReactionType.values();

        int batchSize = 100;
        for (int i = 0; i < 1000; i++) {
            boolean isGroup = faker.number().numberBetween(1, 10) <= 3; // 30% groups

            User owner = users.get(faker.number().numberBetween(0, users.size()));
            String groupName = isGroup ? MockDataConstants.GROUP_NAMES.get(faker.number().numberBetween(0, MockDataConstants.GROUP_NAMES.size())) : null;
            String avatarUrl = isGroup ? MockDataConstants.getRandomImageUrl() : null;

            Conversation conversation = Conversation.builder()
                    .type(isGroup ? ConversationType.GROUP : ConversationType.PRIVATE)
                    .name(groupName)
                    .avatarUrl(avatarUrl)
                    .avatarPublicId(isGroup ? "" : null)
                    .owner(isGroup ? owner : null)
                    .createdAt(OffsetDateTime.now().minusDays(faker.number().numberBetween(1, 60)))
                    .build();
            convsBatch.add(conversation);

            if (convsBatch.size() == batchSize) {
                conversationRepository.saveAllAndFlush(convsBatch);

                for (Conversation savedConv : convsBatch) {
                    List<User> convMembers = new ArrayList<>();

                    if (savedConv.getType() == ConversationType.GROUP) {
                        int groupSize = faker.number().numberBetween(3, 8);
                        Set<Long> memberIds = new HashSet<>();
                        if (savedConv.getOwner() != null) {
                            convMembers.add(savedConv.getOwner());
                            memberIds.add(savedConv.getOwner().getId());
                        }
                        while (convMembers.size() < groupSize) {
                            User candidate = users.get(faker.number().numberBetween(0, users.size()));
                            if (memberIds.add(candidate.getId())) {
                                convMembers.add(candidate);
                            }
                        }
                    } else {
                        User u1 = users.get(faker.number().numberBetween(0, users.size()));
                        User u2 = users.get(faker.number().numberBetween(0, users.size()));
                        while (u1.getId().equals(u2.getId())) {
                            u2 = users.get(faker.number().numberBetween(0, users.size()));
                        }
                        convMembers.add(u1);
                        convMembers.add(u2);
                    }

                    for (User m : convMembers) {
                        membersBatch.add(ConversationMember.builder()
                                .id(new ConversationMemberId(savedConv.getId(), m.getId()))
                                .conversation(savedConv)
                                .user(m)
                                .build());
                    }

                    int msgCount = faker.number().numberBetween(2, 15);
                    List<Message> convMessages = new ArrayList<>();
                    for (int j = 0; j < msgCount; j++) {
                        User sender = convMembers.get(faker.number().numberBetween(0, convMembers.size()));
                        String msgContent = MockDataConstants.VIETNAMESE_COMMENTS.get(
                                faker.number().numberBetween(0, MockDataConstants.VIETNAMESE_COMMENTS.size())
                        );

                        Message message = Message.builder()
                                .conversation(savedConv)
                                .sender(sender)
                                .content(msgContent)
                                .createdAt(OffsetDateTime.now().minusHours(faker.number().numberBetween(1, 48)))
                                .build();
                        convMessages.add(message);
                    }
                    messageRepository.saveAllAndFlush(convMessages);

                    if (!convMessages.isEmpty()) {
                        Message lastMsg = convMessages.get(convMessages.size() - 1);
                        savedConv.setLastMessage(lastMsg);
                        savedConv.setLastMessageAt(lastMsg.getCreatedAt());

                        for (Message msg : convMessages) {
                            // 30% messages have image attachment
                            if (faker.number().numberBetween(1, 10) <= 3) {
                                mediaBatch.add(MessageMedia.builder()
                                        .message(msg)
                                        .url(MockDataConstants.getRandomImageUrl())
                                        .publicId("")
                                        .mediaType(MediaType.IMAGE)
                                        .build());
                            }
                            // 30% messages have reaction
                            if (faker.number().numberBetween(1, 10) <= 3) {
                                User rUser = convMembers.get(faker.number().numberBetween(0, convMembers.size()));
                                reactionsBatch.add(MessageReaction.builder()
                                        .message(msg)
                                        .user(rUser)
                                        .type(rTypes[faker.number().numberBetween(0, rTypes.length)])
                                        .build());
                            }
                        }
                    }
                }

                conversationRepository.saveAll(convsBatch);
                conversationMemberRepository.saveAll(membersBatch);
                messageMediaRepository.saveAll(mediaBatch);

                try {
                    messageReactionRepository.saveAll(reactionsBatch);
                } catch (Exception e) {}

                convsBatch.clear();
                membersBatch.clear();
                mediaBatch.clear();
                reactionsBatch.clear();
                log.info("Saved batch of {} conversations", batchSize);
            }
        }

        log.info("Conversation generation complete");
    }
}
