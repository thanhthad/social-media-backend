package media.social.modults.user.service;

import media.social.modults.user.dto.request.auth.LoginRequest;
import media.social.modults.user.dto.request.auth.RegisterRequest;
import media.social.modults.user.dto.response.auth.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse generateAccessToken(String refreshToken);

    void logout(String refreshToken);
}