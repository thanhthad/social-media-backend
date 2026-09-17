package media.social.modules.dating.service.impl;

import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.entity.ConversationMember;
import media.social.modules.conversation.entity.ConversationMemberId;
import media.social.modules.conversation.enums.ConversationType;
import media.social.modules.conversation.repository.ConversationMemberRepository;
import media.social.modules.conversation.repository.ConversationRepository;
import media.social.modules.dating.entity.DatingMatch;
import media.social.modules.dating.repository.DatingMatchRepository;
import media.social.modules.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatingMatchServiceImplTest {

    @Mock
    private DatingMatchRepository datingMatchRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @InjectMocks
    private DatingMatchServiceImpl datingMatchService;

    private User userOne;
    private User userTwo;

    @BeforeEach
    void setUp() {
        userOne = User.builder().id(1L).username("alice").build();
        userTwo = User.builder().id(2L).username("bob").build();
    }

    // -------------------------------------------------------------------------
    // 1. Happy path: userOne has the smaller id
    // -------------------------------------------------------------------------
    @Test
    void createMatch_success_userOneHasSmallerId_createsMatchAndConversationAndMembers() {
        // userOne.id=1 < userTwo.id=2, so firstUser=userOne, secondUser=userTwo
        when(datingMatchRepository.existsByUserOneIdAndUserTwoId(1L, 2L)).thenReturn(false);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(10L);
            return conv;
        });

        datingMatchService.createMatch(userOne, userTwo);

        // Verify match is saved with correct user order (smaller id first)
        ArgumentCaptor<DatingMatch> matchCaptor = ArgumentCaptor.forClass(DatingMatch.class);
        verify(datingMatchRepository).save(matchCaptor.capture());
        DatingMatch savedMatch = matchCaptor.getValue();
        assertThat(savedMatch.getUserOne()).isEqualTo(userOne);
        assertThat(savedMatch.getUserTwo()).isEqualTo(userTwo);
    }

    // -------------------------------------------------------------------------
    // 2. Happy path: userTwo has the smaller id — order must be swapped
    // -------------------------------------------------------------------------
    @Test
    void createMatch_success_userTwoHasSmallerId_createsMatchWithCorrectOrder() {
        // Swap: userOne.id=5, userTwo.id=3 => firstUser should be userTwo (id=3)
        User bigUser   = User.builder().id(5L).username("charlie").build();
        User smallUser = User.builder().id(3L).username("diana").build();

        when(datingMatchRepository.existsByUserOneIdAndUserTwoId(3L, 5L)).thenReturn(false);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(20L);
            return conv;
        });

        datingMatchService.createMatch(bigUser, smallUser);

        ArgumentCaptor<DatingMatch> matchCaptor = ArgumentCaptor.forClass(DatingMatch.class);
        verify(datingMatchRepository).save(matchCaptor.capture());
        DatingMatch savedMatch = matchCaptor.getValue();

        // firstUser must be the one with smaller id (smallUser, id=3)
        assertThat(savedMatch.getUserOne().getId()).isEqualTo(3L);
        assertThat(savedMatch.getUserTwo().getId()).isEqualTo(5L);
    }

    // -------------------------------------------------------------------------
    // 3. Same user — must throw IllegalArgumentException
    // -------------------------------------------------------------------------
    @Test
    void createMatch_sameUser_throwsIllegalArgumentException() {
        User sameUser = User.builder().id(7L).username("eve").build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> datingMatchService.createMatch(sameUser, sameUser)
        );

        assertThat(ex.getMessage()).isEqualTo("Users cannot match with themselves");

        // No repository interactions should occur
        verifyNoInteractions(datingMatchRepository, conversationRepository, conversationMemberRepository);
    }

    // -------------------------------------------------------------------------
    // 4. Match already exists — returns early, nothing is created
    // -------------------------------------------------------------------------
    @Test
    void createMatch_alreadyMatched_doesNothing() {
        when(datingMatchRepository.existsByUserOneIdAndUserTwoId(1L, 2L)).thenReturn(true);

        datingMatchService.createMatch(userOne, userTwo);

        verify(datingMatchRepository).existsByUserOneIdAndUserTwoId(1L, 2L);
        verify(datingMatchRepository, never()).save(any());
        verifyNoInteractions(conversationRepository, conversationMemberRepository);
    }

    // -------------------------------------------------------------------------
    // 5. Conversation is created with ConversationType.DATING
    // -------------------------------------------------------------------------
    @Test
    void createMatch_createsConversationWithDatingType() {
        when(datingMatchRepository.existsByUserOneIdAndUserTwoId(1L, 2L)).thenReturn(false);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(30L);
            return conv;
        });

        datingMatchService.createMatch(userOne, userTwo);

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(conversationCaptor.capture());
        assertThat(conversationCaptor.getValue().getType()).isEqualTo(ConversationType.DATING);
    }

    // -------------------------------------------------------------------------
    // 6. All repositories are called exactly once with correct entities
    // -------------------------------------------------------------------------
    @Test
    void createMatch_savesAllEntities_allRepositoriesCalledOnce() {
        when(datingMatchRepository.existsByUserOneIdAndUserTwoId(1L, 2L)).thenReturn(false);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(40L);
            return conv;
        });

        datingMatchService.createMatch(userOne, userTwo);

        // conversationRepository saved once
        verify(conversationRepository, times(1)).save(any(Conversation.class));

        // conversationMemberRepository saved twice (one per member)
        ArgumentCaptor<ConversationMember> memberCaptor = ArgumentCaptor.forClass(ConversationMember.class);
        verify(conversationMemberRepository, times(2)).save(memberCaptor.capture());

        ConversationMember member1 = memberCaptor.getAllValues().get(0);
        ConversationMember member2 = memberCaptor.getAllValues().get(1);

        // First member belongs to userOne (smaller id)
        assertThat(member1.getUser()).isEqualTo(userOne);
        assertThat(member1.getConversation().getId()).isEqualTo(40L);

        // Second member belongs to userTwo (larger id)
        assertThat(member2.getUser()).isEqualTo(userTwo);
        assertThat(member2.getConversation().getId()).isEqualTo(40L);

        // datingMatchRepository saved once
        verify(datingMatchRepository, times(1)).save(any(DatingMatch.class));
    }

    // -------------------------------------------------------------------------
    // 7. Conversation member IDs are constructed with correct conversationId/userId
    // -------------------------------------------------------------------------
    @Test
    void createMatch_conversationMemberIds_areSetCorrectly() {
        when(datingMatchRepository.existsByUserOneIdAndUserTwoId(1L, 2L)).thenReturn(false);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(50L);
            return conv;
        });

        datingMatchService.createMatch(userOne, userTwo);

        ArgumentCaptor<ConversationMember> memberCaptor = ArgumentCaptor.forClass(ConversationMember.class);
        verify(conversationMemberRepository, times(2)).save(memberCaptor.capture());

        ConversationMember member1 = memberCaptor.getAllValues().get(0);
        ConversationMember member2 = memberCaptor.getAllValues().get(1);

        // Member IDs use the saved conversation's id
        assertThat(member1.getId()).isEqualTo(new ConversationMemberId(50L, 1L));
        assertThat(member2.getId()).isEqualTo(new ConversationMemberId(50L, 2L));
    }

    // -------------------------------------------------------------------------
    // 8. existsByUserOneIdAndUserTwoId is always called with (min, max) regardless of input order
    // -------------------------------------------------------------------------
    @Test
    void createMatch_existsCheck_alwaysUsesMinMaxOrdering() {
        User bigUser   = User.builder().id(9L).username("frank").build();
        User smallUser = User.builder().id(4L).username("grace").build();

        when(datingMatchRepository.existsByUserOneIdAndUserTwoId(4L, 9L)).thenReturn(false);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(60L);
            return conv;
        });

        // Pass bigUser first intentionally
        datingMatchService.createMatch(bigUser, smallUser);

        // Must query with (4, 9) — min first, max second
        verify(datingMatchRepository).existsByUserOneIdAndUserTwoId(4L, 9L);
        verify(datingMatchRepository, never()).existsByUserOneIdAndUserTwoId(9L, 4L);
    }
}
