package media.social.modules.dating.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.entity.ConversationMember;
import media.social.modules.conversation.entity.ConversationMemberId;
import media.social.modules.conversation.enums.ConversationType;
import media.social.modules.conversation.repository.ConversationMemberRepository;
import media.social.modules.conversation.repository.ConversationRepository;
import media.social.modules.dating.entity.DatingMatch;
import media.social.modules.dating.repository.DatingMatchRepository;
import media.social.modules.dating.service.DatingMatchService;
import media.social.modules.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DatingMatchServiceImpl implements DatingMatchService {

    private final DatingMatchRepository datingMatchRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;

    @Override
    @Transactional
    public void createMatch(
            User userOne,
            User userTwo
    ) {

        if (userOne.getId().equals(userTwo.getId())) {
            throw new IllegalArgumentException(
                    "Users cannot match with themselves"
            );
        }

        Long firstUserId = Math.min(
                userOne.getId(),
                userTwo.getId()
        );

        Long secondUserId = Math.max(
                userOne.getId(),
                userTwo.getId()
        );

        if (datingMatchRepository
                .existsByUserOneIdAndUserTwoId(
                        firstUserId,
                        secondUserId
                )) {
            return;
        }

        User firstUser = userOne.getId().equals(firstUserId)
                ? userOne
                : userTwo;

        User secondUser = userOne.getId().equals(firstUserId)
                ? userTwo
                : userOne;

        Conversation conversation = Conversation.builder()
                .type(ConversationType.PRIVATE)
                .build();

        conversationRepository.save(conversation);

        ConversationMember member1 =
                ConversationMember.builder()
                        .id(
                                new ConversationMemberId(
                                        conversation.getId(),
                                        firstUser.getId()
                                )
                        )
                        .conversation(conversation)
                        .user(firstUser)
                        .build();

        ConversationMember member2 =
                ConversationMember.builder()
                        .id(
                                new ConversationMemberId(
                                        conversation.getId(),
                                        secondUser.getId()
                                )
                        )
                        .conversation(conversation)
                        .user(secondUser)
                        .build();

        conversationMemberRepository.save(member1);
        conversationMemberRepository.save(member2);

        DatingMatch match = DatingMatch.builder()
                .userOne(firstUser)
                .userTwo(secondUser)
                .conversation(conversation)
                .build();

        datingMatchRepository.save(match);
    }
}