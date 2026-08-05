package media.social.modules.auth.security.oauth;

import lombok.RequiredArgsConstructor;
import media.social.modules.auth.Enum.AuthProvider;
import media.social.modules.user.entity.Profile;
import media.social.modules.user.entity.User;
import media.social.modules.user.repository.ProfileRepository;
import media.social.modules.user.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request)
            throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(request);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String avatarUrl = oAuth2User.getAttribute("picture");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .provider(AuthProvider.GOOGLE)
                            .emailVerified(true)
                            .build();

                    User saved =  userRepository.save(newUser);

                    Profile profile = Profile.builder()
                            .user(saved)
                            .fullName(name)
                            .avatarUrl(avatarUrl)
                            .build();

                    profileRepository.save(profile);
                    return saved;
                });

        return oAuth2User;
    }
}