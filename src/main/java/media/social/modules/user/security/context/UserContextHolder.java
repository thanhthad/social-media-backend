package media.social.modules.user.security.context;

import media.social.modules.user.exception.user.UnauthorizedException;
import media.social.modules.user.security.userdetails.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


public class UserContextHolder {

    public static CustomUserDetails getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Please login first");
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new UnauthorizedException("Please login first");
        }

        return user;
    }

    public static boolean isAuthenticated() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails;
    }

    public static Long getUserId() {
        return getCurrentUser().getId();
    }

    public static String getEmail() {
        return getCurrentUser().getEmail();
    }

    public static String getUsername() {
        return getCurrentUser().getUsername();
    }
}