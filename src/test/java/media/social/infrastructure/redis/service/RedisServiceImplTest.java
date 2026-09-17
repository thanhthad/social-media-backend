package media.social.infrastructure.redis.service;

import media.social.infrastructure.redis.serializer.RedisSerializationFormat;
import media.social.infrastructure.redis.service.impl.RedisServiceImpl;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.user.dto.response.cache.UserCacheResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisServiceImpl Unit Tests")
class RedisServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedisTemplate<String, Object> jdkRedisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private ValueOperations<String, String> stringValueOps;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    private RedisServiceImpl redisService;

    @BeforeEach
    void setUp() {
        redisService = new RedisServiceImpl(redisTemplate, stringRedisTemplate, jdkRedisTemplate);
    }

    @Test
    @DisplayName("set and get with default JSON format")
    void testSetAndGetJson() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        UserCacheResponse user = UserCacheResponse.builder()
                .id(1L)
                .username("test")
                .fullname("Test User")
                .build();

        redisService.set("user:1", user, Duration.ofMinutes(10));
        verify(valueOps).set("user:1", user, Duration.ofMinutes(10));

        when(valueOps.get("user:1")).thenReturn(user);
        UserCacheResponse fetched = redisService.get("user:1", UserCacheResponse.class);
        assertThat(fetched).isNotNull();
        assertThat(fetched.getId()).isEqualTo(1L);
        assertThat(fetched.getUsername()).isEqualTo("test");
    }

    @Test
    @DisplayName("set and get with STRING format")
    void testSetAndGetString() {
        when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOps);

        redisService.set("key:str", "LIKE", Duration.ofMinutes(5), RedisSerializationFormat.STRING);
        verify(stringValueOps).set("key:str", "LIKE", Duration.ofMinutes(5));

        when(stringValueOps.get("key:str")).thenReturn("LIKE");
        ReactionType reaction = redisService.get("key:str", ReactionType.class, RedisSerializationFormat.STRING);
        assertThat(reaction).isEqualTo(ReactionType.LIKE);

        String strVal = redisService.get("key:str", String.class, RedisSerializationFormat.STRING);
        assertThat(strVal).isEqualTo("LIKE");
    }

    @Test
    @DisplayName("set and get with JDK_BINARY format")
    void testSetAndGetJdk() {
        when(jdkRedisTemplate.opsForValue()).thenReturn(valueOps);

        UserCacheResponse user = UserCacheResponse.builder().id(2L).username("jdk").build();
        redisService.set("key:jdk", user, Duration.ofMinutes(5), RedisSerializationFormat.JDK_BINARY);
        verify(valueOps).set("key:jdk", user, Duration.ofMinutes(5));

        when(valueOps.get("key:jdk")).thenReturn(user);
        UserCacheResponse fetched = redisService.get("key:jdk", UserCacheResponse.class, RedisSerializationFormat.JDK_BINARY);
        assertThat(fetched).isNotNull();
        assertThat(fetched.getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("delete, hasKey, expire, getExpire")
    void testKeyOperations() {
        when(redisTemplate.delete("key:1")).thenReturn(true);
        assertThat(redisService.delete("key:1")).isTrue();

        when(redisTemplate.hasKey("key:1")).thenReturn(true);
        assertThat(redisService.hasKey("key:1")).isTrue();

        when(redisTemplate.expire("key:1", Duration.ofMinutes(5))).thenReturn(true);
        assertThat(redisService.expire("key:1", Duration.ofMinutes(5))).isTrue();

        when(redisTemplate.getExpire("key:1", TimeUnit.SECONDS)).thenReturn(300L);
        assertThat(redisService.getExpire("key:1")).isEqualTo(300L);

        when(redisTemplate.keys("user:*")).thenReturn(Set.of("user:1", "user:2"));
        assertThat(redisService.keys("user:*")).containsExactlyInAnyOrder("user:1", "user:2");
    }

    @Test
    @DisplayName("hash operations")
    void testHashOperations() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        redisService.hSet("hash:1", "field1", "val1");
        verify(hashOps).put("hash:1", "field1", "val1");

        when(hashOps.get("hash:1", "field1")).thenReturn("val1");
        String val = redisService.hGet("hash:1", "field1", String.class);
        assertThat(val).isEqualTo("val1");
    }
}
