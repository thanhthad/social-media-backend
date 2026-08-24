package media.social.modules.file.media.upload.policy.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.file.media.upload.policy.MediaUploadPolicy;
import media.social.modules.file.media.upload.policy.MediaUploadPolicyResolver;
import media.social.modules.file.media.upload.policy.MediaUploadProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Resolves MediaUploadPolicy from configuration using a Map lookup.
 * To add a new context (e.g. REEL, COMMENT): add it to MediaUploadContext enum,
 * add its config in application.properties, and add the mapping here.
 * CloudinaryStorageService and MediaValidator do NOT need to change.
 */
@Component
@RequiredArgsConstructor
public class MediaUploadPolicyResolverImpl implements MediaUploadPolicyResolver {

    private final MediaUploadProperties props;

    @Override
    public MediaUploadPolicy resolve(MediaUploadContext context) {
        return policyMap().get(context);
    }

    private Map<MediaUploadContext, MediaUploadPolicy> policyMap() {

        Map<MediaUploadContext, MediaUploadPolicy> map = new EnumMap<>(MediaUploadContext.class);

        MediaUploadProperties.ImageProperties img   = props.getImage();
        MediaUploadProperties.VideoProperties video = props.getVideo();
        MediaUploadProperties.PoliciesProperties pol = props.getPolicies();

        map.put(MediaUploadContext.POST, new MediaUploadPolicy(
                img.getMaxSizeBytes(),
                pol.getPost().getMaxVideoSizeBytes(),
                pol.getPost().getMaxVideoDurationSeconds(),
                img.getAllowedExtensions(),
                video.getAllowedExtensions()
        ));

        map.put(MediaUploadContext.STORY, new MediaUploadPolicy(
                img.getMaxSizeBytes(),
                pol.getStory().getMaxVideoSizeBytes(),
                pol.getStory().getMaxVideoDurationSeconds(),
                img.getAllowedExtensions(),
                video.getAllowedExtensions()
        ));

        map.put(MediaUploadContext.MESSAGE, new MediaUploadPolicy(
                img.getMaxSizeBytes(),
                pol.getMessage().getMaxVideoSizeBytes(),
                pol.getMessage().getMaxVideoDurationSeconds(),
                img.getAllowedExtensions(),
                video.getAllowedExtensions()
        ));

        map.put(MediaUploadContext.PROFILE, new MediaUploadPolicy(
                img.getMaxSizeBytes(),
                pol.getProfile().getMaxVideoSizeBytes(),
                pol.getProfile().getMaxVideoDurationSeconds(),
                img.getAllowedExtensions(),
                video.getAllowedExtensions()
        ));

        return map;
    }
}
