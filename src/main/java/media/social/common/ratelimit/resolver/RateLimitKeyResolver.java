package media.social.common.ratelimit.resolver;

import media.social.modules.user.security.context.UserContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RateLimitKeyResolver {

    public String resolve(String name) {

        if (UserContextHolder.isAuthenticated()) {
            Long userId =
                    UserContextHolder.getUserId();

            return "rl:"
                    + name
                    + ":user:"
                    + userId;
        }
        return "rl:"
                + name
                + ":ip:"
                + getClientIp();
    }

    private String getClientIp() {

        ServletRequestAttributes attributes =
                (ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        return attributes
                .getRequest()
                .getRemoteAddr();
    }
}