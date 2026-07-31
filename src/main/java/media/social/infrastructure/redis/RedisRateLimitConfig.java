package media.social.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class RedisRateLimitConfig {

    @Bean
    public RedisTemplate<String, String> rateLimitRedisTemplate(
            RedisConnectionFactory factory
    ){

        RedisTemplate<String,String> template =
                new RedisTemplate<>();

        template.setConnectionFactory(factory);

        template.setKeySerializer(
                new StringRedisSerializer()
        );

        template.setValueSerializer(
                new StringRedisSerializer()
        );

        return template;
    }
}