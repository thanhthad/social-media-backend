package media.social.modules.file.media.upload.policy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Binds media upload configuration from application.properties.
 * Prefix: media.upload
 *
 * Example:
 *   media.upload.image.max-size-bytes=5242880
 *   media.upload.image.allowed-extensions=jpg,jpeg,png,webp
 *   media.upload.video.allowed-extensions=mp4,mov,avi,mkv,webm
 *   media.upload.policies.post.max-video-size-bytes=314572800
 *   media.upload.policies.post.max-video-duration-seconds=600
 */
@Component
@ConfigurationProperties(prefix = "media.upload")
@Getter
@Setter
public class MediaUploadProperties {

    private ImageProperties image = new ImageProperties();
    private VideoProperties video = new VideoProperties();
    private PoliciesProperties policies = new PoliciesProperties();

    @Getter
    @Setter
    public static class ImageProperties {
        private long maxSizeBytes = 5 * 1024 * 1024L;   // 5MB default
        private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "webp");
    }

    @Getter
    @Setter
    public static class VideoProperties {
        private List<String> allowedExtensions = List.of("mp4", "mov", "avi", "mkv", "webm");
    }

    @Getter
    @Setter
    public static class PoliciesProperties {
        private ContextPolicyProperties post    = new ContextPolicyProperties(300L * 1024 * 1024, 600);
        private ContextPolicyProperties story   = new ContextPolicyProperties(100L * 1024 * 1024, 30);
        private ContextPolicyProperties message = new ContextPolicyProperties(50L * 1024 * 1024, 300);
        private ContextPolicyProperties profile = new ContextPolicyProperties(300L * 1024 * 1024, 0);
    }

    @Getter
    @Setter
    public static class ContextPolicyProperties {
        private long maxVideoSizeBytes;
        private long maxVideoDurationSeconds;

        public ContextPolicyProperties() {
        }

        public ContextPolicyProperties(long maxVideoSizeBytes, long maxVideoDurationSeconds) {
            this.maxVideoSizeBytes = maxVideoSizeBytes;
            this.maxVideoDurationSeconds = maxVideoDurationSeconds;
        }
    }
}
