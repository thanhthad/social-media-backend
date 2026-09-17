package media.social.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

@DisplayName("RedisCacheConfig Tests")
class RedisCacheConfigTest {

    @Test
    @DisplayName("cacheManager bean builds with all initial cache configurations")
    void testCacheManagerConfiguration() {
        RedisCacheConfig config = new RedisCacheConfig();
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);

        RedisCacheManager cacheManager = config.cacheManager(factory);
        assertThat(cacheManager).isNotNull();

        CacheErrorHandler errorHandler = config.errorHandler();
        assertThat(errorHandler).isNotNull();
        assertThat(errorHandler).isInstanceOf(CustomCacheErrorHandler.class);
    }

    @Test
    @DisplayName("CustomCacheErrorHandler methods execute without throwing")
    void testCustomCacheErrorHandler() {
        CustomCacheErrorHandler handler = new CustomCacheErrorHandler();
        RuntimeException ex = new RuntimeException("Redis connection refused");

        assertDoesNotThrow(() -> handler.handleCacheGetError(ex, null, "test-key"));
        assertDoesNotThrow(() -> handler.handleCachePutError(ex, null, "test-key", "test-val"));
        assertDoesNotThrow(() -> handler.handleCacheEvictError(ex, null, "test-key"));
        assertDoesNotThrow(() -> handler.handleCacheClearError(ex, null));
    }
}
