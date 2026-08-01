package media.social.common.ratelimit.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String name();

    int limit();

    long windowSeconds();
}