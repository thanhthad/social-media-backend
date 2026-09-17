package media.social.infrastructure.redis.service;

import media.social.infrastructure.redis.serializer.RedisSerializationFormat;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Service interface for direct Redis operations supporting multiple serialization formats.
 */
public interface RedisService {

    /**
     * Store a key-value pair with default JSON serialization and TTL.
     */
    <T> void set(String key, T value, Duration ttl);

    /**
     * Store a key-value pair with a specific serialization format and TTL.
     */
    <T> void set(String key, T value, Duration ttl, RedisSerializationFormat format);

    /**
     * Store a key-value pair without expiration.
     */
    <T> void set(String key, T value);

    /**
     * Get a value with default JSON deserialization.
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * Get a value with a specific serialization format.
     */
    <T> T get(String key, Class<T> clazz, RedisSerializationFormat format);

    /**
     * Delete a key.
     */
    Boolean delete(String key);

    /**
     * Delete multiple keys.
     */
    Long delete(Collection<String> keys);

    /**
     * Check if a key exists.
     */
    Boolean hasKey(String key);

    /**
     * Set expiration time for a key.
     */
    Boolean expire(String key, Duration ttl);

    /**
     * Get remaining time to live for a key in seconds.
     */
    Long getExpire(String key);

    /**
     * Increment an integer value stored in Redis.
     */
    Long increment(String key, long delta);

    /**
     * Decrement an integer value stored in Redis.
     */
    Long decrement(String key, long delta);

    /**
     * Set a hash field value.
     */
    void hSet(String key, String hashKey, Object value);

    /**
     * Get a hash field value.
     */
    <T> T hGet(String key, String hashKey, Class<T> clazz);

    /**
     * Get all key-value pairs in a hash.
     */
    Map<Object, Object> hGetAll(String key);

    /**
     * Delete one or more hash fields.
     */
    Long hDelete(String key, Object... hashKeys);

    /**
     * Find keys matching a pattern.
     */
    Set<String> keys(String pattern);
}
