package media.social.common.ratelimit.aspect;

import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.ratelimit.exception.TooManyRequestException;
import media.social.common.ratelimit.resolver.RateLimitKeyResolver;
import media.social.common.ratelimit.service.RateLimitService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;
    private final RateLimitKeyResolver keyResolver;

    @Before("@annotation(rateLimit)")
    public void check(
            RateLimit rateLimit
    ) {
        String key =
                keyResolver.resolve(
                        rateLimit.name()
                );

        boolean allowed =
                rateLimitService.allow(
                        key,
                        rateLimit.limit(),
                        rateLimit.windowSeconds()
                );
        if (!allowed) {
            throw new TooManyRequestException(
                    "Too many requests"
            );
        }
    }
}