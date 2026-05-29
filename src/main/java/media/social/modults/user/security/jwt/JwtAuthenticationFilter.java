package media.social.modults.user.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import media.social.modults.user.security.userdetails.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");

        // no token
        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        try {

            String token = authHeader.substring(7);

            jwtUtil.validateToken(token);

            CustomUserDetails userDetails =
                    jwtUtil.getUserDetails(token);

            // create authentication object
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // set authentication
            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {

            sendErrorResponse(
                    response,
                    "JWT token has expired"
            );

        } catch (SignatureException ex) {

            sendErrorResponse(
                    response,
                    "Invalid JWT signature"
            );

        } catch (MalformedJwtException ex) {

            sendErrorResponse(
                    response,
                    "Malformed JWT token"
            );

        } catch (UnsupportedJwtException ex) {

            sendErrorResponse(
                    response,
                    "Unsupported JWT token"
            );

        } catch (IllegalArgumentException ex) {

            sendErrorResponse(
                    response,
                    "JWT token is empty"
            );

        } catch (Exception ex) {

            sendErrorResponse(
                    response,
                    "Authentication failed"
            );
        }
    }

    private void sendErrorResponse(
            HttpServletResponse response,
            String message
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType("application/json");

        response.getWriter().write(
                """
                {
                    "success": false,
                    "message": "%s"
                }
                """.formatted(message)
        );
    }
}