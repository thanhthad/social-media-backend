package media.social.modules.auth.service;

import media.social.modules.auth.dto.request.LoginRequest;
import media.social.modules.auth.dto.request.RegisterRequest;
import media.social.modules.auth.dto.response.AuthResponse;

public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse generateAccessToken(String refreshToken);

    void logout(String refreshToken);
}