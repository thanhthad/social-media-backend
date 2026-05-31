package media.social.modults.user.security.context;

import media.social.modults.user.exception.user.UnauthorizedException;
import media.social.modults.user.security.userdetails.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserContextHolder {

    public static Long getUserId() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null ||
                !auth.isAuthenticated() ||
                "anonymousUser".equals(auth.getPrincipal()) ||
                !(auth.getPrincipal() instanceof CustomUserDetails)) {

            throw new UnauthorizedException(
                    "Please login first"
            );
        }

        return ((CustomUserDetails) auth.getPrincipal()).getId();
    }

    public static boolean isAuthenticated() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        return auth != null
                && auth.isAuthenticated()
                && auth.getPrincipal() instanceof CustomUserDetails;
    }
}