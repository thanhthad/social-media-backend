package media.social.infrastructure.redis.serializer;

/**
 * Supported serialization formats for Redis storage and caching.
 */
public enum RedisSerializationFormat {

    /**
     * Polymorphic JSON format using Jackson with type metadata support.
     * Ideal for polymorphic objects, collections, and generic DTOs.
     */
    JSON,

    /**
     * Typed JSON format using Jackson for specific class types without type envelope.
     */
    TYPED_JSON,

    /**
     * Plain text / UTF-8 String format.
     * Ideal for strings, enums, numbers, and raw keys.
     */
    STRING,

    /**
     * Standard Java JDK binary serialization.
     * Ideal for any Java Serializable objects.
     */
    JDK_BINARY
}
