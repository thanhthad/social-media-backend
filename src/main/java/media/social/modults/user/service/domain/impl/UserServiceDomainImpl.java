package media.social.modults.user.service.domain.impl;


import lombok.AllArgsConstructor;
import media.social.modults.user.entity.User;
import media.social.modults.user.exception.user.UserNotFoundException;
import media.social.modults.user.repository.UserRepository;
import media.social.modults.user.service.domain.UserServiceDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
