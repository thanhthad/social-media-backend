package media.social.modules.auth.service;

import media.social.modules.auth.dto.request.*;
import media.social.modules.auth.dto.response.AuthResponse;

public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse generateAccessToken(String refreshToken);

    void logout(String refreshToken);

    String forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void resendVerification(
            ResendVerificationRequest request
    );
}