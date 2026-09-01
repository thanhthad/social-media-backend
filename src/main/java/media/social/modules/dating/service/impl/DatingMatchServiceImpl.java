package media.social.modules.dating.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.entity.ConversationMember;
import media.social.modules.conversation.entity.ConversationMemberId;
import media.social.modules.conversation.enums.ConversationType;
import media.social.modules.conversation.repository.ConversationMemberRepository;
import media.social.modules.conversation.repository.ConversationRepository;
import media.social.modules.dating.dto.response.match.DatingMatchResponse;
import media.social.modules.dating.entity.DatingMatch;
import media.social.modules.dating.repository.DatingMatchRepository;
import media.social.modules.dating.service.DatingMatchService;
import media.social.modules.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        if (datingMatchRepository.existsByUserOneIdAndUserTwoId(
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
                .type(ConversationType.DATING)
                .build();

        conversationRepository.save(conversation);

        ConversationMember conversationMember1 =
                ConversationMember.builder()
                        .id(new ConversationMemberId(
                                conversation.getId(),
                                firstUser.getId()
                        ))
                        .conversation(conversation)
                        .user(firstUser)
                        .build();

        ConversationMember conversationMember2 =
                ConversationMember.builder()
                        .id(new ConversationMemberId(
                                conversation.getId(),
                                secondUser.getId()
                        ))
                        .conversation(conversation)
                        .user(secondUser)
                        .build();

        conversationMemberRepository.save(conversationMember1);
        conversationMemberRepository.save(conversationMember2);

        DatingMatch match = DatingMatch.builder()
                .userOne(firstUser)
                .userTwo(secondUser)
                .build();

        datingMatchRepository.save(match);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatingMatchResponse> getMyMatches() {
        Long currentUserId = media.social.modules.auth.security.context.UserContextHolder.getUserId();
        List<DatingMatch> matches = datingMatchRepository.findActiveMatchesByUserId(currentUserId);

        return matches.stream().map(m -> {
            User other = m.getUserOne().getId().equals(currentUserId) ? m.getUserTwo() : m.getUserOne();
            var profile = other.getProfile();
            return DatingMatchResponse.builder()
                    .matchId(m.getId())
                    .matchedUserId(other.getId())
                    .username(other.getUsername())
                    .name(profile != null && profile.getFullName() != null ? profile.getFullName() : other.getUsername())
                    .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                    .bio(profile != null ? profile.getBio() : null)
                    .matchedAt(m.getMatchedAt())
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public void unmatch(Long matchId) {
        Long currentUserId = media.social.modules.auth.security.context.UserContextHolder.getUserId();
        DatingMatch match = datingMatchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tương hợp"));

        if (!match.getUserOne().getId().equals(currentUserId) && !match.getUserTwo().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Bạn không có quyền huỷ tương hợp này");
        }

        match.setStatus(media.social.modules.dating.enums.DatingMatchStatus.UNMATCHED);
        datingMatchRepository.save(match);
    }
}