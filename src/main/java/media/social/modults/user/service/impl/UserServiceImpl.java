package media.social.modults.user.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import media.social.modults.file.image.dto.response.UploadImageResponse;
import media.social.modults.file.image.service.CloudinaryService;
import media.social.modults.user.dto.request.self.ChangePasswordRequest;
import media.social.modults.user.dto.request.self.UpdateAvatarRequest;
import media.social.modults.user.dto.request.self.UpdateProfileRequest;
import media.social.modults.user.dto.response.UserResponse;
import media.social.modults.user.dto.response.pub.PublicUserProfileResponse;
import media.social.modults.user.dto.response.self.ProfileResponse;
import media.social.modults.user.dto.response.self.UserProfileResponse;
import media.social.modults.user.entity.Profile;
import media.social.modults.user.entity.User;
import media.social.modults.user.exception.profile.ProfileNotFoundException;
import media.social.modults.user.exception.user.UserNotFoundException;
import media.social.modults.user.mapper.ProfileMapper;
import media.social.modults.user.mapper.UserMapper;
import media.social.modults.user.repository.ProfileRepository;
import media.social.modults.user.repository.UserRepository;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final CloudinaryService cloudinaryService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileResponse getMe() {
        Long userId = UserContextHolder.getUserId();
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found with id: "+userId)
        );
        Profile profile = profileRepository.findByUserId(userId).orElseThrow(
                () -> new ProfileNotFoundException("Profile not found with userId: " + userId)
        );
        UserProfileResponse userProfileResponse = userMapper.toUserProfileResponse(user,profile);

        return userProfileResponse;
    }

    @Override
    @Transactional
    public UserProfileResponse updateMe(UpdateProfileRequest request) {
        Long userId = UserContextHolder.getUserId();
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found with id: "+userId)
        );
        Profile profile = profileRepository.findByUserId(userId).orElseThrow(
                () -> new ProfileNotFoundException("Profile not found with userId: " + userId)
        );

        profileMapper.updateProfileFromRequest(request,profile);
        profileRepository.save(profile);

        UserProfileResponse userProfileResponse = userMapper.toUserProfileResponse(user,profile);

        return userProfileResponse;
    }

    @Override
    public UserProfileResponse updateAvatar(UpdateAvatarRequest request) {
        Long userId = UserContextHolder.getUserId();

        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found with id: "+userId)
        );

        Profile profile = profileRepository.findByUserId(userId).orElseThrow(
                () -> new ProfileNotFoundException("Profile not found with userId: " + userId)
        );
        cloudinaryService.validateImage(request.getFile());

        UploadImageResponse uploadImageResponse = cloudinaryService.uploadImage(request.getFile(),"avatars");

        if(profile.getAvatarUrl() != null){
            cloudinaryService.deleteImage(profile.getAvatarPublicId());
        }

        profile.setAvatarPublicId(uploadImageResponse.getPublicId());

        profile.setAvatarUrl(uploadImageResponse.getImageUrl());

        profileRepository.save(profile);

        UserProfileResponse userProfileResponse = userMapper.toUserProfileResponse(user,profile);
        return userProfileResponse;
    }

    @Override
    public void updatePassword(ChangePasswordRequest request) {
        Long userId = UserContextHolder.getUserId();

        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found with id: "+userId)
        );

        if(!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())){
            throw new BadCredentialsException("Invalid password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public PublicUserProfileResponse getUserById(Long id) {
        
    }

    @Override
    public Page<UserResponse> findUsersByName(String username, Pageable pageable) {
        return null;
    }
}
