package media.social.modules.auth.security.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import media.social.modules.auth.dto.response.AuthResponse;
import media.social.modules.auth.security.jwt.JwtUtil;
import media.social.modules.auth.security.userdetails.CustomUserDetails;
import media.social.modules.auth.security.userdetails.CustomUserDetailsService;
import media.social.modules.user.service.RefreshTokenService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final CustomUserDetailsService customUserDetailsService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User =
                (OAuth2User) authentication.getPrincipal();

        String email =
                oAuth2User.getAttribute("email");

        CustomUserDetails user = (CustomUserDetails)
                customUserDetailsService.loadUserByUsername(email);

        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAuthorities()
        );

        String refreshToken = refreshTokenService.create(user.getId()).getToken();

        boolean hasUsername = user.getUsername() != null && !user.getUsername().isBlank();

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");

        AuthResponse responseBody = AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .hasUsername(hasUsername)
                .build();

        objectMapper.writeValue(
                response.getWriter(),
                responseBody
        );

    }
}