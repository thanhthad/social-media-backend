package media.social.modules.dating.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.dating.dto.request.interest.UpdateDatingInterestRequest;
import media.social.modules.dating.dto.response.interest.DatingInterestResponse;
import media.social.modules.dating.entity.DatingInterest;
import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.dating.entity.DatingProfileInterest;
import media.social.modules.dating.entity.DatingProfileInterestId;
import media.social.modules.dating.exception.profile.BadRequestException;
import media.social.modules.dating.repository.DatingInterestRepository;
import media.social.modules.dating.repository.DatingProfileInterestRepository;
import media.social.modules.dating.repository.DatingProfileRepository;
import media.social.modules.dating.service.DatingInterestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatingInterestServiceImpl implements DatingInterestService {

    private final DatingInterestRepository datingInterestRepository;

    private final DatingProfileInterestRepository datingProfileInterestRepository;

    private final DatingProfileRepository datingProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DatingInterestResponse> getAllInterest() {

        return datingInterestRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatingInterestResponse> getMyInterest() {

        Long userId = UserContextHolder.getUserId();

        DatingProfile profile = getDatingProfile(userId);

        return datingProfileInterestRepository
                .findByDatingProfileDatingProfileId(profile.getId())
                .stream()
                .map(item -> mapToResponse(item.getInterest()))
                .toList();
    }


    @Override
    @Transactional
    public List<DatingInterestResponse> updateMyInterest(UpdateDatingInterestRequest request) {

        Long userId = UserContextHolder.getUserId();

        DatingProfile profile = getDatingProfile(userId);

        datingProfileInterestRepository
                .deleteByDatingProfileDatingProfileId(profile.getId());


        List<DatingInterest> interests =
                datingInterestRepository.findAllByIdIn(request.getInterestIds());


        if(interests.size() != request.getInterestIds().size()){
            throw new BadRequestException("Some interests do not exist");
        }


        List<DatingProfileInterest> mappings = interests.stream()
                .map(interest -> DatingProfileInterest.builder()
                        .id(new DatingProfileInterestId(
                                profile.getId(),
                                interest.getId()
                        ))
                        .datingProfile(profile)
                        .interest(interest)
                        .createdAt(OffsetDateTime.now())
                        .build())
                .toList();


        datingProfileInterestRepository.saveAll(mappings);


        return interests.stream()
                .map(this::mapToResponse)
                .toList();
    }


    private DatingProfile getDatingProfile(Long userId){

        return datingProfileRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new BadRequestException("Dating profile not found")
                );
    }


    private DatingInterestResponse mapToResponse(DatingInterest interest){

        return DatingInterestResponse.builder()
                .id(interest.getId())
                .name(interest.getName())
                .build();
    }
}