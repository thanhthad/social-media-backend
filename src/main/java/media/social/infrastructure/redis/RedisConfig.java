package media.social.infrastructure.redis;

import media.social.infrastructure.redis.serializer.RedisSerializerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * Primary RedisTemplate with JSON serialization (Polymorphic Jackson).
     * Ideal for DTOs, complex domain objects, and collections.
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        RedisSerializer<String> stringSerializer = RedisSerializerFactory.createStringSerializer();
        RedisSerializer<Object> jsonSerializer = RedisSerializerFactory.createGenericJsonSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedisTemplate with JDK Binary serialization.
     * Ideal for Java Serializable objects and binary structures.
     */
    @Bean(name = "jdkRedisTemplate")
    public RedisTemplate<String, Object> jdkRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        RedisSerializer<String> stringSerializer = RedisSerializerFactory.createStringSerializer();
        RedisSerializer<Object> jdkSerializer = RedisSerializerFactory.createJdkSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jdkSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jdkSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * RedisTemplate for raw byte array operations.
     */
    @Bean(name = "byteRedisTemplate")
    public RedisTemplate<String, byte[]> byteRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        RedisSerializer<String> stringSerializer = RedisSerializerFactory.createStringSerializer();
        RedisSerializer<byte[]> byteSerializer = RedisSerializer.byteArray();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(byteSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(byteSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * StringRedisTemplate for string/text/token operations.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}