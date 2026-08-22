package media.social.infrastructure.redis;

import media.social.infrastructure.redis.serializer.RedisSerializerFactory;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    private static final String KEY_PREFIX_DELIMITER = ":";
    private static final String APP_PREFIX = "social:cache:";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisSerializer<String> stringSerializer = RedisSerializerFactory.createStringSerializer();
        RedisSerializer<Object> jsonSerializer = RedisSerializerFactory.createGenericJsonSerializer();

        // Default Cache Configuration (30m TTL, JSON serializer, prefixed keys)
        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> APP_PREFIX + cacheName + KEY_PREFIX_DELIMITER)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        // Tailored Cache Configurations based on cache services
        Map<String, RedisCacheConfiguration> initialCacheConfigurations = new HashMap<>();

        // 1. User basic info cache (30m TTL)
        initialCacheConfigurations.put(
                RedisCacheNames.USERS,
                defaultConfiguration.entryTtl(Duration.ofMinutes(30))
        );

        // 2. User full profile cache (60m TTL)
        initialCacheConfigurations.put(
                RedisCacheNames.USER_PROFILE,
                defaultConfiguration.entryTtl(Duration.ofMinutes(60))
        );

        // 3. Friendship count cache (10m TTL - counts change frequently)
        initialCacheConfigurations.put(
                RedisCacheNames.FRIENDSHIP_COUNT,
                defaultConfiguration.entryTtl(Duration.ofMinutes(10))
        );

        // 4. Post detail cache (15m TTL)
        initialCacheConfigurations.put(
                RedisCacheNames.POSTS,
                defaultConfiguration.entryTtl(Duration.ofMinutes(15))
        );

        // 5. User reaction cache (30m TTL)
        initialCacheConfigurations.put(
                RedisCacheNames.USER_REACTIONS,
                defaultConfiguration.entryTtl(Duration.ofMinutes(30))
        );

        // 6. User stories cache (5m TTL - stories are transient)
        initialCacheConfigurations.put(
                RedisCacheNames.USER_STORIES,
                defaultConfiguration.entryTtl(Duration.ofMinutes(5))
        );

        // 7. Dating profile cache (30m TTL)
        initialCacheConfigurations.put(
                RedisCacheNames.DATING_PROFILE,
                defaultConfiguration.entryTtl(Duration.ofMinutes(30))
        );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(initialCacheConfigurations)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CustomCacheErrorHandler();
    }
}