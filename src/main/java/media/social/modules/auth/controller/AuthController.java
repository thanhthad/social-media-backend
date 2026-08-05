package media.social.modules.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.auth.dto.request.*;
import media.social.modules.auth.dto.response.AuthResponse;
import media.social.modules.auth.service.AuthService;
import media.social.modules.auth.service.EmailVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "Auth APIs"
)
public class AuthController {

    private final AuthService authService;

    private final EmailVerificationService emailVerificationService;

    // ================= LOGIN =================
    @PostMapping("/login")
    @RateLimit(
            name = "AUTH_LOGIN",
            limit = 10,
            windowSeconds = 60
    )
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request
    ) {

        AuthResponse response =
                authService.login(request);

        return ResponseData.success(
                response,
                "Login successfully",
                HttpStatus.OK
        );
    }

    // ================= REGISTER =================
    @PostMapping("/register")
    @RateLimit(
            name = "AUTH_REGISTER",
            limit = 5,
            windowSeconds = 60
    )
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        authService.register(request);

        return ResponseData.success(
                null,
                "Register successfully",
                HttpStatus.OK
        );
    }

    // ================= VERIFY EMAIL =================
    @GetMapping("/verify-email")
    @RateLimit(
            name = "AUTH_VERIFY_EMAIL",
            limit = 20,
            windowSeconds = 60
    )
    public ResponseEntity<?> verifyEmail(
            @RequestParam String token
    ) {
        emailVerificationService.verify(token);

        return ResponseData.success(
                null,
                "Email verified successfully",
                HttpStatus.OK
        );
    }

    // ================= RESEND VERIFICATION =================
    @PostMapping("/resend-verification")
    @RateLimit(
            name = "AUTH_RESEND_VERIFICATION",
            limit = 3,
            windowSeconds = 300
    )
    public ResponseEntity<?> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {

        authService.resendVerification(request);

        return ResponseData.success(
                null,
                "Verification email has been sent",
                HttpStatus.OK
        );
    }

    // ================= REFRESH TOKEN =================
    @PostMapping("/refresh")
    @RateLimit(
            name = "AUTH_REFRESH_TOKEN",
            limit = 30,
            windowSeconds = 60
    )
    public ResponseEntity<?> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResponse response =
                authService.generateAccessToken(
                        request.getRefreshToken()
                );

        return ResponseData.success(
                response,
                "Create new access token successfully",
                HttpStatus.OK
        );
    }

    // ================= LOGOUT =================
    @PostMapping("/logout")
    @RateLimit(
            name = "AUTH_LOGOUT",
            limit = 30,
            windowSeconds = 60
    )
    public ResponseEntity<?> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        authService.logout(
                request.getRefreshToken()
        );

        return ResponseData.success(
                null,
                "Logout successfully",
                HttpStatus.OK
        );
    }

    // ================= FORGOT PASSWORD =================
    @PostMapping("/forgot-password")
    @RateLimit(
            name = "AUTH_FORGOT_PASSWORD",
            limit = 5,
            windowSeconds = 300
    )
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ){

        String token =
                authService.forgotPassword(request);

        return ResponseData.success(
                token,
                "Password reset email sent successfully",
                HttpStatus.OK
        );
    }

    // ================= RESET PASSWORD =================

    @PostMapping("/reset-password")
    @RateLimit(
            name = "AUTH_RESET_PASSWORD",
            limit = 10,
            windowSeconds = 60
    )
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ){
        authService.resetPassword(request);


        return ResponseData.success(
                null,
                "Password reset successfully",
                HttpStatus.OK
        );
    }
}