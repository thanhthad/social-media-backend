package media.social.modults.user.security.context;

import media.social.modults.user.security.userdetails.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserContextHolder {

    public static Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null ||
                !auth.isAuthenticated() ||
                auth.getPrincipal().equals("anonymousUser") ||
                !(auth.getPrincipal() instanceof CustomUserDetails)) {
            return null;
        }

        return ((CustomUserDetails) auth.getPrincipal()).getId();
    }

    public static boolean isAuthenticated() {
        return getUserId() != null;
    }
}