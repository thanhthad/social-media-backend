package media.social.modults.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.dto.request.LoginRequest;
import media.social.modults.dto.request.RefreshTokenRequest;
import media.social.modults.dto.request.RegisterRequest;
import media.social.modults.dto.response.LoginResponse;
import media.social.modults.dto.response.UserResponse;
import media.social.modults.service.RefreshTokenService;
import media.social.modults.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Authentication", description = "Auth APIs")
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    // ================= LOGIN =================
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request
    ) {

        LoginResponse response = userService.login(request);

        return ResponseData.success(
                response,
                "Login successfully",
                HttpStatus.OK
        );
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        UserResponse response = userService.register(request);

        return ResponseData.success(
                response,
                "Login successfully",
                HttpStatus.OK
        );
    }

    // ================= REFRESH TOKEN =================
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        String newAccessToken =
                refreshTokenService.generateAccessToken(request.getRefreshToken());

        return ResponseData.success(
                newAccessToken,
                "Create new access token successfully",
                HttpStatus.OK
        );
    }

    // ================= LOGOUT =================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        refreshTokenService.revoke(request.getRefreshToken());

        return ResponseData.success(
                null,
                "Logout successfully",
                HttpStatus.OK
        );
    }
}