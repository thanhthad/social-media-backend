package media.social.modules.dating.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.dating.dto.request.interest.UpdateDatingInterestRequest;
import media.social.modules.dating.dto.response.interest.DatingInterestResponse;
import media.social.modules.dating.entity.DatingInterest;
import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.dating.entity.DatingProfileInterest;
import media.social.modules.dating.exception.profile.BadRequestException;
import media.social.modules.dating.repository.DatingInterestRepository;
import media.social.modules.dating.repository.DatingProfileInterestRepository;
import media.social.modules.dating.repository.DatingProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatingInterestServiceImplTest {

    @Mock
    private DatingInterestRepository datingInterestRepository;

    @Mock
    private DatingProfileInterestRepository datingProfileInterestRepository;

    @Mock
    private DatingProfileRepository datingProfileRepository;

    @InjectMocks
    private DatingInterestServiceImpl datingInterestService;

    // -----------------------------------------------------------------------
    // getAllInterest
    // -----------------------------------------------------------------------

    @Test
    void getAllInterest_success_returnsList() {
        DatingInterest interest1 = DatingInterest.builder().id(1L).name("Music").build();
        DatingInterest interest2 = DatingInterest.builder().id(2L).name("Travel").build();
        when(datingInterestRepository.findAll()).thenReturn(List.of(interest1, interest2));

        List<DatingInterestResponse> result = datingInterestService.getAllInterest();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("Music");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getName()).isEqualTo("Travel");
        verify(datingInterestRepository).findAll();
    }

    @Test
    void getAllInterest_empty_returnsEmptyList() {
        when(datingInterestRepository.findAll()).thenReturn(Collections.emptyList());

        List<DatingInterestResponse> result = datingInterestService.getAllInterest();

        assertThat(result).isEmpty();
        verify(datingInterestRepository).findAll();
    }

    // -----------------------------------------------------------------------
    // getMyInterest
    // -----------------------------------------------------------------------

    @Test
    void getMyInterest_success() {
        Long userId = 10L;
        DatingProfile profile = DatingProfile.builder().id(100L).build();
        DatingInterest interest = DatingInterest.builder().id(1L).name("Cooking").build();
        DatingProfileInterest profileInterest = DatingProfileInterest.builder()
                .interest(interest)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);
            when(datingProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(datingProfileInterestRepository.findByDatingProfileId(profile.getId()))
                    .thenReturn(List.of(profileInterest));

            List<DatingInterestResponse> result = datingInterestService.getMyInterest();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(0).getName()).isEqualTo("Cooking");
            verify(datingProfileRepository).findByUserId(userId);
            verify(datingProfileInterestRepository).findByDatingProfileId(profile.getId());
        }
    }

    @Test
    void getMyInterest_profileNotFound_throwsBadRequestException() {
        Long userId = 10L;

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);
            when(datingProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> datingInterestService.getMyInterest());

            assertThat(exception.getMessage()).isEqualTo("Dating profile not found");
            verify(datingProfileRepository).findByUserId(userId);
            verifyNoInteractions(datingProfileInterestRepository);
        }
    }

    // -----------------------------------------------------------------------
    // updateMyInterest
    // -----------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void updateMyInterest_success_deletesOldAndSavesNew() {
        Long userId = 10L;
        DatingProfile profile = DatingProfile.builder().id(100L).build();
        DatingInterest interest1 = DatingInterest.builder().id(1L).name("Gaming").build();
        DatingInterest interest2 = DatingInterest.builder().id(2L).name("Hiking").build();

        UpdateDatingInterestRequest request = new UpdateDatingInterestRequest();
        request.setInterestIds(List.of(1L, 2L));

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);
            when(datingProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(datingInterestRepository.findAllByIdIn(List.of(1L, 2L)))
                    .thenReturn(List.of(interest1, interest2));
            when(datingProfileInterestRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<DatingInterestResponse> result = datingInterestService.updateMyInterest(request);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(0).getName()).isEqualTo("Gaming");
            assertThat(result.get(1).getId()).isEqualTo(2L);
            assertThat(result.get(1).getName()).isEqualTo("Hiking");

            verify(datingProfileInterestRepository).deleteByDatingProfileId(profile.getId());

            ArgumentCaptor<List<DatingProfileInterest>> captor = ArgumentCaptor.forClass(List.class);
            verify(datingProfileInterestRepository).saveAll(captor.capture());
            List<DatingProfileInterest> saved = captor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved.get(0).getInterest()).isEqualTo(interest1);
            assertThat(saved.get(0).getDatingProfile()).isEqualTo(profile);
            assertThat(saved.get(1).getInterest()).isEqualTo(interest2);
            assertThat(saved.get(1).getDatingProfile()).isEqualTo(profile);
        }
    }

    @Test
    void updateMyInterest_someInterestsNotFound_throwsBadRequestException() {
        Long userId = 10L;
        DatingProfile profile = DatingProfile.builder().id(100L).build();
        DatingInterest interest1 = DatingInterest.builder().id(1L).name("Gaming").build();

        UpdateDatingInterestRequest request = new UpdateDatingInterestRequest();
        request.setInterestIds(List.of(1L, 2L)); // 2 requested, only 1 found

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);
            when(datingProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(datingInterestRepository.findAllByIdIn(List.of(1L, 2L)))
                    .thenReturn(List.of(interest1));

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> datingInterestService.updateMyInterest(request));

            assertThat(exception.getMessage()).isEqualTo("Some interests do not exist");
            verify(datingProfileInterestRepository).deleteByDatingProfileId(profile.getId());
            verify(datingProfileInterestRepository, never()).saveAll(anyList());
        }
    }

    @Test
    void updateMyInterest_emptyList_success() {
        Long userId = 10L;
        DatingProfile profile = DatingProfile.builder().id(100L).build();

        UpdateDatingInterestRequest request = new UpdateDatingInterestRequest();
        request.setInterestIds(Collections.emptyList());

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);
            when(datingProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(datingInterestRepository.findAllByIdIn(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());
            when(datingProfileInterestRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

            List<DatingInterestResponse> result = datingInterestService.updateMyInterest(request);

            assertThat(result).isEmpty();
            verify(datingProfileInterestRepository).deleteByDatingProfileId(profile.getId());
            verify(datingProfileInterestRepository).saveAll(Collections.emptyList());
        }
    }

    @Test
    void updateMyInterest_profileNotFound_throwsBadRequestException() {
        Long userId = 10L;

        UpdateDatingInterestRequest request = new UpdateDatingInterestRequest();
        request.setInterestIds(List.of(1L));

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);
            when(datingProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> datingInterestService.updateMyInterest(request));

            assertThat(exception.getMessage()).isEqualTo("Dating profile not found");
            verify(datingProfileRepository).findByUserId(userId);
            verifyNoInteractions(datingInterestRepository);
            verify(datingProfileInterestRepository, never()).deleteByDatingProfileId(anyLong());
            verify(datingProfileInterestRepository, never()).saveAll(anyList());
        }
    }
}
