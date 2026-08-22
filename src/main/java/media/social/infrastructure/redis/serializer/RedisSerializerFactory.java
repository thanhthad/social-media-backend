package media.social.infrastructure.redis.serializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

/**
 * Factory for creating configured Redis serializers supporting different serialization formats.
 */
public final class RedisSerializerFactory {

    private RedisSerializerFactory() {
        // Prevent instantiation
    }

    private static BasicPolymorphicTypeValidator createTypeValidator() {
        return BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("media.social")
                .allowIfSubType("java.util")
                .allowIfSubType("java.time")
                .allowIfSubType("java.lang")
                .build();
    }

    /**
     * Creates a customized ObjectMapper configured for Redis JSON serialization with polymorphic typing.
     */
    public static ObjectMapper createRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mapper.activateDefaultTyping(
                createTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }

    /**
     * Creates an ObjectMapper tailored for deserializing polymorphic collections / arrays
     * without expecting an outer type wrapper on the array root itself.
     */
    public static ObjectMapper createCollectionObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mapper.activateDefaultTyping(
                createTypeValidator(),
                ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT,
                JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }

    /**
     * Creates a customized ObjectMapper for typed JSON serialization (no polymorphic metadata envelope).
     */
    public static ObjectMapper createTypedObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Generic JSON serializer with polymorphic typing support for DTOs and collections.
     * Adaptively handles single objects, empty arrays ([]), and arrays of polymorphic elements ([{"@class": ...}]).
     */
    public static RedisSerializer<Object> createGenericJsonSerializer() {
        return new AdaptiveJsonRedisSerializer();
    }

    /**
     * Typed JSON serializer for a specific target class.
     */
    public static <T> RedisSerializer<T> createTypedJsonSerializer(Class<T> targetClass) {
        return new Jackson2JsonRedisSerializer<>(createTypedObjectMapper(), targetClass);
    }

    /**
     * String serializer for plain text, UTF-8 strings, and enums.
     */
    public static StringRedisSerializer createStringSerializer() {
        return StringRedisSerializer.UTF_8;
    }

    /**
     * Standard JDK binary serializer for Java Serializable objects.
     */
    public static RedisSerializer<Object> createJdkSerializer() {
        return new JdkSerializationRedisSerializer();
    }

    /**
     * Create RedisSerializer dynamically based on format and optional target class.
     */
    @SuppressWarnings("unchecked")
    public static <T> RedisSerializer<?> createSerializer(RedisSerializationFormat format, Class<T> targetClass) {
        return switch (format) {
            case JSON -> createGenericJsonSerializer();
            case TYPED_JSON -> targetClass != null ? createTypedJsonSerializer(targetClass) : createGenericJsonSerializer();
            case STRING -> createStringSerializer();
            case JDK_BINARY -> createJdkSerializer();
        };
    }

    /**
     * Adaptive JSON serializer that seamlessly handles standard JSON objects as well as raw JSON arrays
     * ([] and [{"@class": ...}]) without throwing MismatchedInputException.
     */
    private static class AdaptiveJsonRedisSerializer implements RedisSerializer<Object> {
        private final GenericJackson2JsonRedisSerializer delegate;
        private final ObjectMapper collectionMapper;
        private final JavaType listType;

        public AdaptiveJsonRedisSerializer() {
            ObjectMapper objectTypeMapper = createRedisObjectMapper();
            this.delegate = new GenericJackson2JsonRedisSerializer(objectTypeMapper);
            this.collectionMapper = createCollectionObjectMapper();
            this.listType = this.collectionMapper.getTypeFactory().constructCollectionType(List.class, Object.class);
        }

        @Override
        public byte[] serialize(Object t) throws SerializationException {
            return delegate.serialize(t);
        }

        @Override
        public Object deserialize(byte[] bytes) throws SerializationException {
            if (bytes == null || bytes.length == 0) {
                return null;
            }

            int i = 0;
            while (i < bytes.length && Character.isWhitespace((char) bytes[i])) {
                i++;
            }

            if (i < bytes.length && bytes[i] == '[') {
                try {
                    return collectionMapper.readValue(bytes, listType);
                } catch (Exception e) {
                    // Fall back to delegate if needed
                }
            }

            return delegate.deserialize(bytes);
        }
    }
}
