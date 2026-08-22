package media.social.infrastructure.redis.serializer;

import media.social.modules.post.enums.ReactionType;
import media.social.modules.user.dto.response.cache.UserCacheResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisSerializerFactory Tests")
class RedisSerializerFactoryTest {

    @Test
    @DisplayName("createStringSerializer - correctly serializes and deserializes String")
    void testStringSerializer() {
        StringRedisSerializer serializer = RedisSerializerFactory.createStringSerializer();
        String original = "hello-world-redis";
        byte[] bytes = serializer.serialize(original);

        assertThat(bytes).isNotEmpty();
        String restored = serializer.deserialize(bytes);
        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("createGenericJsonSerializer - correctly serializes and deserializes UserCacheResponse")
    void testGenericJsonSerializer() {
        RedisSerializer<Object> serializer = RedisSerializerFactory.createGenericJsonSerializer();

        UserCacheResponse user = UserCacheResponse.builder()
                .id(123L)
                .username("testuser")
                .fullname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .build();

        byte[] bytes = serializer.serialize(user);
        assertThat(bytes).isNotEmpty();

        Object deserialized = serializer.deserialize(bytes);
        assertThat(deserialized).isInstanceOf(UserCacheResponse.class);

        UserCacheResponse restoredUser = (UserCacheResponse) deserialized;
        assertThat(restoredUser.getId()).isEqualTo(123L);
        assertThat(restoredUser.getUsername()).isEqualTo("testuser");
        assertThat(restoredUser.getFullname()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("createGenericJsonSerializer - correctly deserializes empty array []")
    void testGenericJsonSerializerEmptyArray() {
        RedisSerializer<Object> serializer = RedisSerializerFactory.createGenericJsonSerializer();
        byte[] emptyArrayBytes = "[]".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        Object result = serializer.deserialize(emptyArrayBytes);
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(java.util.List.class);
        assertThat((java.util.List<?>) result).isEmpty();
    }

    @Test
    @DisplayName("createGenericJsonSerializer - correctly deserializes array with @class objects")
    void testGenericJsonSerializerPolymorphicArray() {
        RedisSerializer<Object> serializer = RedisSerializerFactory.createGenericJsonSerializer();
        String json = "[{\"@class\":\"media.social.modules.story.dto.response.UserStoryResponse\",\"storyId\":500,\"userId\":1236,\"username\":\"jordan.kautzer4722\",\"content\":\"Hành trình\"}]";
        byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        Object result = serializer.deserialize(bytes);
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(java.util.List.class);

        java.util.List<?> list = (java.util.List<?>) result;
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).isInstanceOf(media.social.modules.story.dto.response.UserStoryResponse.class);
        media.social.modules.story.dto.response.UserStoryResponse story = (media.social.modules.story.dto.response.UserStoryResponse) list.get(0);
        assertThat(story.getStoryId()).isEqualTo(500L);
        assertThat(story.getUserId()).isEqualTo(1236L);
        assertThat(story.getContent()).isEqualTo("Hành trình");
    }

    @Test
    @DisplayName("createTypedJsonSerializer - correctly serializes and deserializes target type")
    void testTypedJsonSerializer() {
        RedisSerializer<UserCacheResponse> serializer = RedisSerializerFactory.createTypedJsonSerializer(UserCacheResponse.class);

        UserCacheResponse user = UserCacheResponse.builder()
                .id(456L)
                .username("typeduser")
                .fullname("Typed User")
                .build();

        byte[] bytes = serializer.serialize(user);
        assertThat(bytes).isNotEmpty();

        UserCacheResponse restored = serializer.deserialize(bytes);
        assertThat(restored).isNotNull();
        assertThat(restored.getId()).isEqualTo(456L);
        assertThat(restored.getUsername()).isEqualTo("typeduser");
    }

    @Test
    @DisplayName("createJdkSerializer - correctly serializes Serializable object")
    void testJdkSerializer() {
        RedisSerializer<Object> serializer = RedisSerializerFactory.createJdkSerializer();

        UserCacheResponse user = UserCacheResponse.builder()
                .id(789L)
                .username("jdkuser")
                .fullname("JDK User")
                .build();

        byte[] bytes = serializer.serialize(user);
        assertThat(bytes).isNotEmpty();

        Object deserialized = serializer.deserialize(bytes);
        assertThat(deserialized).isInstanceOf(UserCacheResponse.class);
        UserCacheResponse restored = (UserCacheResponse) deserialized;
        assertThat(restored.getId()).isEqualTo(789L);
    }

    @Test
    @DisplayName("createSerializer with format enum returns corresponding serializer")
    void testCreateSerializerByFormat() {
        RedisSerializer<?> jsonSerializer = RedisSerializerFactory.createSerializer(RedisSerializationFormat.JSON, null);
        assertThat(jsonSerializer).isNotNull();

        RedisSerializer<?> stringSerializer = RedisSerializerFactory.createSerializer(RedisSerializationFormat.STRING, null);
        assertThat(stringSerializer).isNotNull();

        RedisSerializer<?> typedSerializer = RedisSerializerFactory.createSerializer(RedisSerializationFormat.TYPED_JSON, UserCacheResponse.class);
        assertThat(typedSerializer).isNotNull();

        RedisSerializer<?> jdkSerializer = RedisSerializerFactory.createSerializer(RedisSerializationFormat.JDK_BINARY, null);
        assertThat(jdkSerializer).isNotNull();
    }
}
