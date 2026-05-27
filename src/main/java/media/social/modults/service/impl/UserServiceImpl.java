package media.social.modults.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import media.social.modults.dto.request.LoginRequest;
import media.social.modults.dto.request.RegisterRequest;
import media.social.modults.dto.request.UserRequest;
import media.social.modults.dto.response.LoginResponse;
import media.social.modults.dto.response.UploadImageResponse;
import media.social.modults.dto.response.UserResponse;
import media.social.modults.entity.RefreshToken;
import media.social.modults.entity.User;
import media.social.modults.exception.user.UserAlreadyExistsException;
import media.social.modults.exception.user.UserNotFoundException;
import media.social.modults.mapper.UserMapper;
import media.social.modults.repository.UserRepository;
import media.social.modults.security.userdetails.CustomUserDetails;
import media.social.modults.service.CloudinaryService;
import media.social.modults.service.RefreshTokenService;
import media.social.modults.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
@Log4j2
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final CloudinaryService cloudinaryService;

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }

        return ((CustomUserDetails) auth.getPrincipal()).getId();
    }

    // =========================================================
    // 1. CREATE USER
    // =========================================================
    @Override
    @Transactional
    public UserResponse create(UserRequest request) {

        Long actorUserId = getUserId();

        log.info("USER_EVENT | action=CREATE_USER | actorUserId={} | email={} | status=START",
                actorUserId, request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {

            log.warn("USER_EVENT | action=CREATE_USER | actorUserId={} | email={} | status=FAIL | reason=EMAIL_ALREADY_EXISTS",
                    actorUserId, request.getEmail());

            throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

        log.info("USER_EVENT | action=CREATE_USER | actorUserId={} | targetUserId={} | email={} | status=SUCCESS",
                actorUserId, savedUser.getId(), savedUser.getEmail());

        return userMapper.toResponse(savedUser);
    }

    // =========================================================
    // 2. UPDATE USER
    // =========================================================
    @Override
    @Transactional
    public UserResponse updateById(Long id, UserRequest request) {

        Long actorUserId = getUserId();

        log.info("USER_EVENT | action=UPDATE_USER | actorUserId={} | targetUserId={} | status=START",
                actorUserId, id);

        User user = userRepository.findById(id).orElseThrow(() -> {

            log.warn("USER_EVENT | action=UPDATE_USER | actorUserId={} | targetUserId={} | status=FAIL | reason=NOT_FOUND",
                    actorUserId, id);

            return new UserNotFoundException("User not found with id: " + id);
        });

        userMapper.updateUserFromRequest(request, user);

        User savedUser = userRepository.save(user);

        log.info("USER_EVENT | action=UPDATE_USER | actorUserId={} | targetUserId={} | status=SUCCESS",
                actorUserId, id);

        return userMapper.toResponse(savedUser);
    }

    // =========================================================
    // 3. DELETE USER
    // =========================================================
    @Override
    @Transactional
    public void deleteById(Long id) {

        Long actorUserId = getUserId();

        log.info("USER_EVENT | action=DELETE_USER | actorUserId={} | targetUserId={} | status=START",
                actorUserId, id);

        User user = userRepository.findById(id).orElseThrow(() -> {

            log.warn("USER_EVENT | action=DELETE_USER | actorUserId={} | targetUserId={} | status=FAIL | reason=NOT_FOUND",
                    actorUserId, id);

            return new UserNotFoundException("User not found with id: " + id);
        });
        if (user.getAvatarPublicId() != null && !user.getAvatarPublicId().isBlank()) {
            try {
                cloudinaryService.deleteImage(user.getAvatarPublicId());
            } catch (Exception e) {
                log.warn("Failed to delete avatar | publicId={}", user.getAvatarPublicId(), e);
            }
        }
        userRepository.delete(user);

        log.info("USER_EVENT | action=DELETE_USER | actorUserId={} | targetUserId={} | status=SUCCESS",
                actorUserId, id);
    }

    @Override
    @Transactional
    public void deleteByEmail(String email) {
        Long actorUserId = getUserId();

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User not found with email: " + email)
        );
        if (user.getAvatarPublicId() != null && !user.getAvatarPublicId().isBlank()) {
            try {
                cloudinaryService.deleteImage(user.getAvatarPublicId());
            } catch (Exception e) {
                log.warn("Failed to delete avatar | publicId={}", user.getAvatarPublicId(), e);
            }
        }
        userRepository.delete(user);
        log.info("USER_EVENT | action=DELETE_USER | actorUserId={} | targetUserEmail={} | status=SUCCESS",
                actorUserId, email);

    }

    // =========================================================
    // 4. SEARCH USER BY USERNAME
    // =========================================================
    @Override
    public Page<UserResponse> getByUsername(String username, Pageable pageable) {

        Long actorUserId = getUserId();

        log.info("USER_EVENT | action=SEARCH_USER | actorUserId={} | keyword={} | page={} | size={}",
                actorUserId, username, pageable.getPageNumber(), pageable.getPageSize());

        Page<User> userPage = userRepository.findByUsernameContainingIgnoreCase(username, pageable);

        log.info("USER_EVENT | action=SEARCH_USER | actorUserId={} | status=SUCCESS | total={}",
                actorUserId, userPage.getTotalElements());

        return userPage.map(userMapper::toResponse);
    }

    // =========================================================
    // UPLOAD AVATAR
    // =========================================================
    @Override
    @Transactional
    public UserResponse uploadAvatar(Long userId, MultipartFile file) {

        Long actorUserId = getUserId();

        log.info(
                "USER_EVENT | action=UPLOAD_AVATAR | actorUserId={} | targetUserId={} | status=START",
                actorUserId,
                userId
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {

                    log.warn(
                            "USER_EVENT | action=UPLOAD_AVATAR | actorUserId={} | targetUserId={} | status=FAIL | reason=NOT_FOUND",
                            actorUserId,
                            userId
                    );

                    return new UserNotFoundException(
                            "User not found with id: " + userId
                    );
                });
        if (user.getAvatarPublicId() != null && !user.getAvatarPublicId().isBlank()) {
            try {
                cloudinaryService.deleteImage(user.getAvatarPublicId());
            } catch (Exception e) {
                log.warn("Cannot delete old avatar | publicId={}", user.getAvatarPublicId());
            }
        }

        UploadImageResponse imageUrl = cloudinaryService.uploadImage(file, "avatars");
        user.setAvatarUrl(imageUrl.getImageUrl());
        user.setAvatarPublicId(imageUrl.getPublicId());
        User savedUser = userRepository.save(user);

        log.info(
                "USER_EVENT | action=UPLOAD_AVATAR | actorUserId={} | targetUserId={} | status=SUCCESS | avatarUrl={}",
                actorUserId,
                userId,
                imageUrl.getImageUrl()
        );

        return userMapper.toResponse(savedUser);
    }

    // =========================================================
    // 5. GET USER BY ID
    // =========================================================
    @Override
    public UserResponse getById(Long id) {

        Long actorUserId = getUserId();

        log.info("USER_EVENT | action=GET_USER | actorUserId={} | targetUserId={} | status=START",
                actorUserId, id);

        User user = userRepository.findById(id).orElseThrow(() -> {

            log.warn("USER_EVENT | action=GET_USER | actorUserId={} | targetUserId={} | status=FAIL | reason=NOT_FOUND",
                    actorUserId, id);

            return new UserNotFoundException("User not found with id: " + id);
        });

        return userMapper.toResponse(user);
    }

    // =========================================================
    // 6. GET USER BY EMAIL
    // =========================================================
    @Override
    public UserResponse getByEmail(String email) {

        Long actorUserId = getUserId();

        log.info("USER_EVENT | action=GET_USER_BY_EMAIL | actorUserId={} | email={} | status=START",
                actorUserId, email);

        User user = userRepository.findByEmail(email).orElseThrow(() -> {

            log.warn("USER_EVENT | action=GET_USER_BY_EMAIL | actorUserId={} | email={} | status=FAIL | reason=NOT_FOUND",
                    actorUserId, email);

            return new UserNotFoundException("User not found with email: " + email);
        });

        return userMapper.toResponse(user);
    }

    // =========================================================
    // 7. GET ALL USERS
    // =========================================================
    @Override
    public Page<UserResponse> getAll(Pageable pageable) {

        Long actorUserId = getUserId();

        log.info("USER_EVENT | action=GET_ALL_USERS | actorUserId={} | page={} | size={}",
                actorUserId, pageable.getPageNumber(), pageable.getPageSize());

        Page<User> userPage = userRepository.findAll(pageable);

        log.info("USER_EVENT | action=GET_ALL_USERS | actorUserId={} | status=SUCCESS | total={}",
                actorUserId, userPage.getTotalElements());

        return userPage.map(userMapper::toResponse);
    }

    // =========================================================
    // 8. LOGIN
    // =========================================================
    @Override
    public LoginResponse login(LoginRequest loginRequest) {

        log.info("USER_EVENT | action=LOGIN | email={} | status=START",
                loginRequest.getEmail());

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {

                    log.warn("USER_EVENT | action=LOGIN | email={} | status=FAIL | reason=USER_NOT_FOUND",
                            loginRequest.getEmail());

                    return new UserNotFoundException("User not found with email: " + loginRequest.getEmail());
                });

        RefreshToken refreshToken = refreshTokenService.findValidByUser(user.getId());
        String accessToken = refreshTokenService.generateAccessToken(refreshToken.getToken());

        log.info("USER_EVENT | action=LOGIN | userId={} | email={} | status=SUCCESS",
                user.getId(), user.getEmail());

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken.getToken());

        return response;
    }

    // =========================================================
    // 9. REGISTER
    // =========================================================
    @Override
    public UserResponse register(RegisterRequest registerRequest) {

        log.info("USER_EVENT | action=REGISTER | email={} | status=START",
                registerRequest.getEmail());

        if (userRepository.existsByEmail(registerRequest.getEmail())) {

            log.warn("USER_EVENT | action=REGISTER | email={} | status=FAIL | reason=EMAIL_ALREADY_EXISTS",
                    registerRequest.getEmail());

            throw new UserAlreadyExistsException("User already exists with email: " + registerRequest.getEmail());
        }

        User user = User.builder()
                .email(registerRequest.getEmail())
                .username(registerRequest.getUsername())
                .build();

        User saved = userRepository.save(user);

        log.info("USER_EVENT | action=REGISTER | userId={} | email={} | status=SUCCESS",
                saved.getId(), saved.getEmail());

        return userMapper.toResponse(saved);
    }
}