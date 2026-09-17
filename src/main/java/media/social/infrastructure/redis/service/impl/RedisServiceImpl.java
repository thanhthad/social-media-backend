package media.social.infrastructure.redis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import media.social.infrastructure.redis.serializer.RedisSerializationFormat;
import media.social.infrastructure.redis.serializer.RedisSerializerFactory;
import media.social.infrastructure.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> jdkRedisTemplate;
    private final ObjectMapper typedObjectMapper;

    public RedisServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("jdkRedisTemplate") RedisTemplate<String, Object> jdkRedisTemplate
    ) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jdkRedisTemplate = jdkRedisTemplate;
        this.typedObjectMapper = RedisSerializerFactory.createTypedObjectMapper();
    }

    @Override
    public <T> void set(String key, T value, Duration ttl) {
        set(key, value, ttl, RedisSerializationFormat.JSON);
    }

    @Override
    public <T> void set(String key, T value, Duration ttl, RedisSerializationFormat format) {
        if (key == null || value == null) {
            return;
        }

        try {
            switch (format) {
                case STRING -> {
                    String strVal = value instanceof String ? (String) value : String.valueOf(value);
                    if (ttl != null) {
                        stringRedisTemplate.opsForValue().set(key, strVal, ttl);
                    } else {
                        stringRedisTemplate.opsForValue().set(key, strVal);
                    }
                }
                case TYPED_JSON -> {
                    String json = typedObjectMapper.writeValueAsString(value);
                    if (ttl != null) {
                        stringRedisTemplate.opsForValue().set(key, json, ttl);
                    } else {
                        stringRedisTemplate.opsForValue().set(key, json);
                    }
                }
                case JDK_BINARY -> {
                    if (ttl != null) {
                        jdkRedisTemplate.opsForValue().set(key, value, ttl);
                    } else {
                        jdkRedisTemplate.opsForValue().set(key, value);
                    }
                }
                case JSON -> {
                    if (ttl != null) {
                        redisTemplate.opsForValue().set(key, value, ttl);
                    } else {
                        redisTemplate.opsForValue().set(key, value);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Failed to set Redis key [{}] with format [{}]: {}", key, format, ex.getMessage(), ex);
            throw new RuntimeException("Redis set operation failed", ex);
        }
    }

    @Override
    public <T> void set(String key, T value) {
        set(key, value, null, RedisSerializationFormat.JSON);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        return get(key, clazz, RedisSerializationFormat.JSON);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz, RedisSerializationFormat format) {
        if (key == null) {
            return null;
        }

        try {
            switch (format) {
                case STRING -> {
                    String val = stringRedisTemplate.opsForValue().get(key);
                    if (val == null) {
                        return null;
                    }
                    if (clazz == String.class) {
                        return (T) val;
                    }
                    if (clazz.isEnum()) {
                        return (T) Enum.valueOf((Class<Enum>) clazz, val);
                    }
                    if (clazz == Long.class) {
                        return (T) Long.valueOf(val);
                    }
                    if (clazz == Integer.class) {
                        return (T) Integer.valueOf(val);
                    }
                    if (clazz == Boolean.class) {
                        return (T) Boolean.valueOf(val);
                    }
                    return typedObjectMapper.readValue(val, clazz);
                }
                case TYPED_JSON -> {
                    String json = stringRedisTemplate.opsForValue().get(key);
                    if (json == null) {
                        return null;
                    }
                    return typedObjectMapper.readValue(json, clazz);
                }
                case JDK_BINARY -> {
                    Object val = jdkRedisTemplate.opsForValue().get(key);
                    return clazz.isInstance(val) ? (T) val : null;
                }
                case JSON -> {
                    Object val = redisTemplate.opsForValue().get(key);
                    if (val == null) {
                        return null;
                    }
                    if (clazz.isInstance(val)) {
                        return (T) val;
                    }
                    // Handle conversion if object was deserialized into a Map / LinkedHashMap
                    return typedObjectMapper.convertValue(val, clazz);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to get Redis key [{}] with format [{}]: {}", key, format, ex.getMessage(), ex);
            return null;
        }
        return null;
    }

    @Override
    public Boolean delete(String key) {
        if (key == null) {
            return false;
        }
        return redisTemplate.delete(key);
    }

    @Override
    public Long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        return redisTemplate.delete(keys);
    }

    @Override
    public Boolean hasKey(String key) {
        if (key == null) {
            return false;
        }
        return redisTemplate.hasKey(key);
    }

    @Override
    public Boolean expire(String key, Duration ttl) {
        if (key == null || ttl == null) {
            return false;
        }
        return redisTemplate.expire(key, ttl);
    }

    @Override
    public Long getExpire(String key) {
        if (key == null) {
            return -1L;
        }
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    @Override
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    @Override
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    @Override
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String hashKey, Class<T> clazz) {
        Object val = redisTemplate.opsForHash().get(key, hashKey);
        if (val == null) {
            return null;
        }
        if (clazz.isInstance(val)) {
            return (T) val;
        }
        return typedObjectMapper.convertValue(val, clazz);
    }

    @Override
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    @Override
    public Long hDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    @Override
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }
}
