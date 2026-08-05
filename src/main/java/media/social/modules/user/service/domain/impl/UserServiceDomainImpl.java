package media.social.modules.user.service.domain.impl;


import lombok.AllArgsConstructor;
import media.social.modules.user.entity.User;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@AllArgsConstructor
@Service
public class UserServiceDomainImpl  implements UserServiceDomain {
    private final UserRepository userRepository;

    @Override
    public void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

    }

    @Override
    @Transactional(readOnly = true)
    public User getByUserId(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not exists with id: "+ userId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public User getByEmail(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new UserNotFoundException(
                                "User not exists with email: "
                                        + email
                        )
                );
    }

    @Override
    @Transactional
    public void increaseFailedAttempt(String email) {

        User user =
                getByEmail(email);

        int attempts =
                user.getFailedAttempt() + 1;

        user.setFailedAttempt(attempts);

        if(attempts >= 5){
            user.setLockUntil(
                    LocalDateTime.now()
                            .plusHours(2)
            );

            user.setFailedAttempt(0);
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resetFailedAttempt(Long userId) {
        User user =
                getByUserId(userId);

        user.setFailedAttempt(0);

        user.setLockUntil(null);

        userRepository.save(user);
    }

    @Override
    public boolean isAccountLocked(User user) {

        return user.getLockUntil() != null
                &&
                user.getLockUntil()
                        .isAfter(
                                LocalDateTime.now()
                        );
    }
}
