package media.social.modules.user.service;

import media.social.modules.user.dto.request.auth.LoginRequest;
import media.social.modules.user.dto.request.auth.RegisterRequest;
import media.social.modules.user.dto.response.auth.AuthResponse;

public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse generateAccessToken(String refreshToken);

    void logout(String refreshToken);
}