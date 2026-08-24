package media.social.modules.file.media.upload.policy.impl;

import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.file.media.upload.policy.MediaUploadPolicy;
import media.social.modules.file.media.upload.policy.MediaUploadProperties;
import media.social.modules.file.media.upload.policy.MediaUploadPolicyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MediaUploadPolicyResolver")
class MediaUploadPolicyResolverImplTest {

    private MediaUploadPolicyResolver resolver;

    @BeforeEach
    void setUp() {
        MediaUploadProperties props = new MediaUploadProperties();

        // Image config
        props.getImage().setMaxSizeBytes(5 * 1024 * 1024L);
        props.getImage().setAllowedExtensions(List.of("jpg", "jpeg", "png", "webp"));

        // Video config
        props.getVideo().setAllowedExtensions(List.of("mp4", "mov", "avi", "mkv", "webm"));

        // POST policy
        props.getPolicies().getPost().setMaxVideoSizeBytes(300L * 1024 * 1024);
        props.getPolicies().getPost().setMaxVideoDurationSeconds(600);

        // STORY policy
        props.getPolicies().getStory().setMaxVideoSizeBytes(100L * 1024 * 1024);
        props.getPolicies().getStory().setMaxVideoDurationSeconds(30);

        // MESSAGE policy
        props.getPolicies().getMessage().setMaxVideoSizeBytes(50L * 1024 * 1024);
        props.getPolicies().getMessage().setMaxVideoDurationSeconds(300);

        // PROFILE policy
        props.getPolicies().getProfile().setMaxVideoSizeBytes(300L * 1024 * 1024);
        props.getPolicies().getProfile().setMaxVideoDurationSeconds(0);

        resolver = new MediaUploadPolicyResolverImpl(props);
    }

    @Test
    @DisplayName("POST returns correct policy")
    void shouldReturnCorrectPolicyForPost() {
        MediaUploadPolicy policy = resolver.resolve(MediaUploadContext.POST);

        assertThat(policy).isNotNull();
        assertThat(policy.maxVideoSizeBytes()).isEqualTo(300L * 1024 * 1024);
        assertThat(policy.maxVideoDurationSeconds()).isEqualTo(600L);
        assertThat(policy.maxImageSizeBytes()).isEqualTo(5 * 1024 * 1024L);
        assertThat(policy.allowedImageExtensions()).containsExactlyInAnyOrder("jpg", "jpeg", "png", "webp");
        assertThat(policy.allowedVideoExtensions()).containsExactlyInAnyOrder("mp4", "mov", "avi", "mkv", "webm");
    }

    @Test
    @DisplayName("STORY returns correct policy")
    void shouldReturnCorrectPolicyForStory() {
        MediaUploadPolicy policy = resolver.resolve(MediaUploadContext.STORY);

        assertThat(policy).isNotNull();
        assertThat(policy.maxVideoSizeBytes()).isEqualTo(100L * 1024 * 1024);
        assertThat(policy.maxVideoDurationSeconds()).isEqualTo(30L);
    }

    @Test
    @DisplayName("MESSAGE returns correct policy")
    void shouldReturnCorrectPolicyForMessage() {
        MediaUploadPolicy policy = resolver.resolve(MediaUploadContext.MESSAGE);

        assertThat(policy).isNotNull();
        assertThat(policy.maxVideoSizeBytes()).isEqualTo(50L * 1024 * 1024);
        assertThat(policy.maxVideoDurationSeconds()).isEqualTo(300L);
    }

    @Test
    @DisplayName("PROFILE returns correct policy")
    void shouldReturnCorrectPolicyForProfile() {
        MediaUploadPolicy policy = resolver.resolve(MediaUploadContext.PROFILE);

        assertThat(policy).isNotNull();
        assertThat(policy.maxVideoDurationSeconds()).isEqualTo(0L);
    }

    @Test
    @DisplayName("All contexts resolve to non-null policies")
    void allContextsShouldResolveToNonNullPolicy() {
        for (MediaUploadContext context : MediaUploadContext.values()) {
            assertThat(resolver.resolve(context))
                    .as("Policy for context %s should not be null", context)
                    .isNotNull();
        }
    }
}
