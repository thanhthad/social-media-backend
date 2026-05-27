package media.social.modults.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.entity.User;
import media.social.modults.exception.user.UserNotFoundException;
import media.social.modults.repository.UserRepository;
import media.social.modults.service.UserServiceDomain;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class UserServiceDomainImpl  implements UserServiceDomain {
    private final UserRepository userRepository;

    public void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
    }

    @Override
    public User getByUserId(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not exists with id: "+ userId)
        );
    }
}
