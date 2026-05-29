package media.social.modults.user.service;

import media.social.modults.user.dto.request.LoginRequest;
import media.social.modults.user.dto.request.RegisterRequest;
import media.social.modults.user.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String refreshToken);
}