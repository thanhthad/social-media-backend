package media.social.modults.user.security.userdetails;

import lombok.RequiredArgsConstructor;
import media.social.modults.user.Enum.Status;
import media.social.modults.user.entity.User;
import media.social.modults.user.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {

        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        if (user.getStatus() == Status.BANNED) {
            throw new DisabledException(
                    "Your account has been banned"
            );
        }

        return CustomUserDetails.fromUser(user);
    }
}