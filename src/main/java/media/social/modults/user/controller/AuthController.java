package media.social.modults.user.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modults.user.dto.request.auth.LoginRequest;
import media.social.modults.user.dto.request.auth.RefreshTokenRequest;
import media.social.modults.user.dto.request.auth.RegisterRequest;
import media.social.modults.user.dto.response.auth.AuthResponse;
import media.social.modults.user.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Authentication", description = "Auth APIs")
public class AuthController {

    private final AuthService authService;

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

        AuthResponse authResponse =
                authService.generateAccessToken(
                        request.getRefreshToken()
                );

        return ResponseData.success(
                authResponse,
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
}