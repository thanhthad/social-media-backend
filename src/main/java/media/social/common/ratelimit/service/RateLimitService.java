package media.social.common.ratelimit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redis;

    public boolean allow(
            String key,
            int limit,
            long windowSeconds
    ) {
        Long count =
                redis.opsForValue()
                        .increment(key);
        if (count == 1) {
            redis.expire(
                    key,
                    Duration.ofSeconds(windowSeconds)
            );
        }
        return count <= limit;
    }
}