package media.social.modules.dating.service.impl;

import lombok.RequiredArgsConstructor;
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

    @Override
    @Transactional
    public void createMatch(User userOne, User userTwo) {

        if (userOne.getId().equals(userTwo.getId())) {
            throw new IllegalArgumentException(
                    "Users cannot match with themselves"
            );
        }

        Long firstUserId =
                Math.min(
                        userOne.getId(),
                        userTwo.getId()
                );

        Long secondUserId =
                Math.max(
                        userOne.getId(),
                        userTwo.getId()
                );

        if (datingMatchRepository.existsByUserOneUserIdAndUserTwoUserId(
                firstUserId,
                secondUserId
        )) {
            return;
        }

        User firstUser =
                userOne.getId().equals(firstUserId)
                        ? userOne
                        : userTwo;

        User secondUser =
                userOne.getId().equals(firstUserId)
                        ? userTwo
                        : userOne;

        DatingMatch match = DatingMatch.builder()
                .userOne(firstUser)
                .userTwo(secondUser)
                .build();

        datingMatchRepository.save(match);
    }
}
