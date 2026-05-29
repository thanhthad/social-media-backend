package media.social.modults.user.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.user.exception.UserAlreadyExistsException;
import media.social.modults.repository.UserRepository;
import media.social.modults.user.dto.request.LoginRequest;
import media.social.modults.user.dto.request.RegisterRequest;
import media.social.modults.user.dto.response.AuthResponse;
import media.social.modults.user.entity.User;
import media.social.modults.user.service.AuthService;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;

    @Override
    public AuthResponse register(RegisterRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                () -> new UserAlreadyExistsException("User already exists with email: "+ request.getEmail())
        );

    }

    @Override
    public AuthResponse login(LoginRequest request) {
        return null;
    }

    @Override
    public void logout(String refreshToken) {

    }
}
