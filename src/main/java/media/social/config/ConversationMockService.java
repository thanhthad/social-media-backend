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

        log.info("Generating 1000 conversations...");
        List<Conversation> convsBatch = new ArrayList<>();
        List<ConversationMember> membersBatch = new ArrayList<>();
        List<Message> messagesBatch = new ArrayList<>();
        List<MessageMedia> mediaBatch = new ArrayList<>();
        List<MessageReaction> reactionsBatch = new ArrayList<>();
        
        ReactionType[] rTypes = ReactionType.values();

        int batchSize = 100;
        for (int i = 0; i < 1000; i++) {
            User u1 = users.get(faker.number().numberBetween(0, users.size()));
            User u2 = users.get(faker.number().numberBetween(0, users.size()));
            while (u1.getId().equals(u2.getId())) {
                u2 = users.get(faker.number().numberBetween(0, users.size()));
            }

            Conversation conversation = Conversation.builder()
                    .type(ConversationType.PRIVATE)
                    .createdAt(OffsetDateTime.now().minusDays(faker.number().numberBetween(1, 30)))
                    .build();
            convsBatch.add(conversation);

            if (convsBatch.size() == batchSize) {
                conversationRepository.saveAllAndFlush(convsBatch);
                
                for (int cIdx = 0; cIdx < convsBatch.size(); cIdx++) {
                    Conversation savedConv = convsBatch.get(cIdx);
                    // Due to batching, we need to carefully pair u1 and u2. 
                    // To keep it simple, we re-pick u1 and u2 for members if we lost track, or just pick random.
                    User m1 = users.get(faker.number().numberBetween(0, users.size()));
                    User m2 = users.get(faker.number().numberBetween(0, users.size()));
                    if (m1.getId().equals(m2.getId())) {
                        m2 = users.get((faker.number().numberBetween(0, users.size())));
                    }

                    membersBatch.add(ConversationMember.builder()
                            .id(new ConversationMemberId(savedConv.getId(), m1.getId()))
                            .conversation(savedConv)
                            .user(m1)
                            .build());
                    membersBatch.add(ConversationMember.builder()
                            .id(new ConversationMemberId(savedConv.getId(), m2.getId()))
                            .conversation(savedConv)
                            .user(m2)
                            .build());

                    int msgCount = faker.number().numberBetween(1, 20);
                    List<Message> convMessages = new ArrayList<>();
                    for (int j = 0; j < msgCount; j++) {
                        User sender = faker.bool().bool() ? m1 : m2;
                        Message message = Message.builder()
                                .conversation(savedConv)
                                .sender(sender)
                                .content(faker.lorem().sentence())
                                .createdAt(OffsetDateTime.now().minusHours(faker.number().numberBetween(1, 24)))
                                .build();
                        convMessages.add(message);
                    }
                    messageRepository.saveAllAndFlush(convMessages);
                    
                    if (!convMessages.isEmpty()) {
                        Message lastMsg = convMessages.get(convMessages.size() - 1);
                        savedConv.setLastMessage(lastMsg);
                        savedConv.setLastMessageAt(lastMsg.getCreatedAt());
                        
                        for (Message msg : convMessages) {
                            if (faker.bool().bool()) {
                                mediaBatch.add(MessageMedia.builder()
                                        .message(msg)
                                        .url(faker.internet().image())
                                        .publicId(UUID.randomUUID().toString())
                                        .mediaType(MediaType.IMAGE)
                                        .build());
                            }
                            if (faker.bool().bool()) {
                                User rUser = faker.bool().bool() ? m1 : m2;
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
                } catch(Exception e) {} // ignore unique constraint on reaction

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
