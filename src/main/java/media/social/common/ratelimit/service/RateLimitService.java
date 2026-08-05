package media.social.common.ratelimit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redis;

    public boolean allow(
            String key,
            int limit,
            long windowSeconds
    ) {
        try {
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
        } catch (RedisConnectionFailureException ex) {
            log.warn(
                    "Redis unavailable, bypass rate limit. key={}",
                    key
            );
            // Redis chết -> bỏ qua rate limit
            return true;
        }
    }
}