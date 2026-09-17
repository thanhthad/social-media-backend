package media.social.common.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import media.social.common.ratelimit.exception.TooManyRequestException;
import media.social.common.response.ApiResponse;
import media.social.common.response.ResponseData;
import media.social.modules.auth.exception.password.AccountAlreadyLockedException;
import media.social.modules.auth.exception.password.PasswordResetTokenExpiredException;
import media.social.modules.auth.exception.password.PasswordResetTokenInvalidException;
import media.social.modules.auth.exception.password.PasswordResetTokenUsedException;
import media.social.modules.auth.exception.verification.*;
import media.social.modules.conversation.exception.*;
import media.social.modules.dating.exception.photo.*;
import media.social.modules.dating.exception.preference.PreferenceNotFoundException;
import media.social.modules.dating.exception.profile.BadRequestException;
import media.social.modules.dating.exception.profile.CoordinatesNotFoundException;
import media.social.modules.dating.exception.profile.DatingProfileNotFoundException;
import media.social.modules.dating.exception.report.CannotReportOwnProfileException;
import media.social.modules.dating.exception.report.DatingReportAlreadyExistsException;
import media.social.modules.dating.exception.report.DatingReportAlreadyReviewedException;
import media.social.modules.dating.exception.report.DatingReportNotFoundException;
import media.social.modules.file.exception.CloudinaryDeleteException;
import media.social.modules.file.exception.CloudinaryUploadException;
import media.social.modules.file.exception.InvalidMediaException;
import media.social.modules.notification.exception.NotificationAlreadyExistsException;
import media.social.modules.notification.exception.NotificationNotFoundException;
import media.social.modules.post.exception.comment.CanNotCommentYourself;
import media.social.modules.post.exception.comment.CommentAlreadyExistsException;
import media.social.modules.post.exception.comment.CommentNotFoundException;
import media.social.modules.post.exception.post.*;
import media.social.modules.post.exception.post_media.InvalidImageException;
import media.social.modules.post.exception.post_media.MediaNotFoundException;
import media.social.modules.post.exception.reaction.ReactionNotFoundException;
import media.social.modules.post.exception.report.CannotReportOwnPostException;
import media.social.modules.post.exception.report.ReportAlreadyExistsException;
import media.social.modules.post.exception.report.ReportAlreadyReviewedException;
import media.social.modules.post.exception.report.ReportNotFoundException;
import media.social.modules.post.exception.saved_post.PostFriendsOnlyException;
import media.social.modules.post.exception.saved_post.SavedPostAlreadyExistsException;
import media.social.modules.post.exception.saved_post.SavedPostNotFoundException;
import media.social.modules.story.exception.reaction.StoryReactionNotFoundException;
import media.social.modules.story.exception.story.StoryAccessDeniedException;
import media.social.modules.story.exception.story.StoryForbiddenException;
import media.social.modules.story.exception.story.StoryNotFoundException;
import media.social.modules.user.exception.block.BlockAlreadyExistsException;
import media.social.modules.user.exception.block.BlockNotFoundException;
import media.social.modules.user.exception.block.UserBlockedException;
import media.social.modules.user.exception.profile.ProfileNotFoundException;
import media.social.modules.auth.exception.refreshtoken.InvalidRefreshTokenException;
import media.social.modules.auth.exception.refreshtoken.RefreshTokenExpiredException;
import media.social.modules.auth.exception.refreshtoken.RefreshTokenRevokedException;
import media.social.modules.user.exception.role.RoleNotFoundException;
import media.social.modules.user.exception.role.UserRoleAlreadyExistsException;
import media.social.modules.user.exception.role.UserRoleNotFoundException;
import media.social.modules.user.exception.user.UnauthorizedException;
import media.social.modules.user.exception.user.UserAlreadyExistsException;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.auth.security.context.UserContextHolder;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import media.social.modules.user.exception.user.ForbiddenException;
import media.social.modules.reel.exception.ReelNotFoundException;
import media.social.modules.reel.exception.ReelForbiddenException;
import media.social.modules.reel.exception.ReelViewNotFoundException;

import java.util.HashMap;
import java.util.Map;
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private Long getUserId(){
        try {
            if (UserContextHolder.isAuthenticated()) {
                return UserContextHolder.getUserId();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ======================= DATING PROFILE PHOTO =======================

    @ExceptionHandler(DatingProfilePhotoAlreadyPrimaryException.class)
    public ResponseEntity<ApiResponse<Object>> handleDatingProfilePhotoAlreadyPrimary(
            DatingProfilePhotoAlreadyPrimaryException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(DatingProfilePhotoLimitExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleDatingProfilePhotoLimitExceeded(
            DatingProfilePhotoLimitExceededException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(DatingProfilePhotoNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleDatingProfilePhotoNotFound(
            DatingProfilePhotoNotFoundException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(InvalidDatingProfilePhotoException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidDatingProfilePhoto(
            InvalidDatingProfilePhotoException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(UnauthorizedDatingProfilePhotoException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedDatingProfilePhoto(
            UnauthorizedDatingProfilePhotoException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    // ======================= STORY & REACTION =======================

    @ExceptionHandler(StoryReactionNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleStoryReactionNotFound(
            StoryReactionNotFoundException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(StoryAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleStoryAccessDenied(
            StoryAccessDeniedException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(StoryForbiddenException.class)
    public ResponseEntity<ApiResponse<Object>> handleStoryForbidden(
            StoryForbiddenException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(StoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleStoryNotFound(
            StoryNotFoundException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    // ================= PREFERENCE =================
    @ExceptionHandler(PreferenceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handlePreferenceNotFound(
            PreferenceNotFoundException ex
    ){
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    // ================= PROFILE =================
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(
            BadRequestException ex
    ){
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(CoordinatesNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleCoordinatesNotFound(
            CoordinatesNotFoundException ex
    ){
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(DatingProfileNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleDatingProfileNotFound(
            DatingProfileNotFoundException ex
    ){
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    // ================= REPORT =================
    @ExceptionHandler(CannotReportOwnProfileException.class)
    public ResponseEntity<ApiResponse<Object>> handleCannotReportOwnProfile(
            CannotReportOwnProfileException ex
    ){
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(DatingReportAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleDatingReportAlreadyExists(
            DatingReportAlreadyExistsException ex
    ){
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(DatingReportAlreadyReviewedException.class)
    public ResponseEntity<ApiResponse<Object>> handleDatingReportAlreadyReviewed(
            DatingReportAlreadyReviewedException ex
    ){
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(DatingReportNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleDatingReportNotFound(
            DatingReportNotFoundException ex
    ){
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleConnectionError(
            ResourceAccessException ex
    ){

        log.error("External service connection failed", ex);

        return ResponseData.fail(
                "Service unavailable. Please try again later.",
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }
    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<ApiResponse<Object>> handleRedisConnectionFailure(
            RedisConnectionFailureException ex
    ){

        log.error("Redis connection failed", ex);

        return ResponseData.fail(
                "System temporarily unavailable",
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    // ================= PASSWORD RESET =================
    @ExceptionHandler(PasswordResetTokenInvalidException.class)
    public ResponseEntity<ApiResponse<Object>>
    handlePasswordResetTokenInvalid(
            PasswordResetTokenInvalidException ex
    ){

        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(PasswordResetTokenExpiredException.class)
    public ResponseEntity<ApiResponse<Object>>
    handlePasswordResetTokenExpired(
            PasswordResetTokenExpiredException ex
    ){

        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(PasswordResetTokenUsedException.class)
    public ResponseEntity<ApiResponse<Object>>
    handlePasswordResetTokenUsed(
            PasswordResetTokenUsedException ex
    ){

        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(AccountAlreadyLockedException.class)
    public ResponseEntity<ApiResponse<Object>>
    handleAccountAlreadyLockedTokenUsed(
            AccountAlreadyLockedException ex
    ){

        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Object>>
    handleAccountLockedTokenUsed(
            LockedException ex
    ){

        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    // ================= EMAIL VERIFICATION ===========================
    @ExceptionHandler(EmailSendFailedException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailSendFailException(
            EmailSendFailedException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailAlreadyVerifiedException(
            EmailAlreadyVerifiedException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }


    @ExceptionHandler(EmailVerificationTokenInvalidException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidVerificationToken(
            EmailVerificationTokenInvalidException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(EmailVerificationTokenExpiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleExpiredVerificationToken(
            EmailVerificationTokenExpiredException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(EmailVerificationTokenUsedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsedVerificationToken(
            EmailVerificationTokenUsedException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    // ================= REDIS_RATE_LIMIT ===========================
    @ExceptionHandler(TooManyRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleRateLimit(
            TooManyRequestException ex
    ) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);

    }
    // ================= CONVERSATION ===========================
    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleConversationNotFound(ConversationNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConversationAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleConversationAlreadyExists(ConversationAlreadyExistsException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.CONFLICT);
    }

    // ================= MEMBER ===========================
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleMemberNotFound(MemberNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MemberAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleMemberAlreadyExists(MemberAlreadyExistsException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserInMemberAlreadyExists.class)
    public ResponseEntity<ApiResponse<Object>> handleUserInMemberAlreadyExists(UserInMemberAlreadyExists ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.CONFLICT);
    }

    // ================= MESSAGE ===========================
    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleMessageNotFound(MessageNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // ================= NOTIFICATION ===========================
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotificationNotFound(NotificationNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(NotificationAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotificationAlreadyExists(NotificationAlreadyExistsException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.CONFLICT);
    }

    // ================= FILE ===========================
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex
    ) {

        return ResponseData.fail(
                "File size exceeds the maximum allowed limit.",
                HttpStatus.BAD_REQUEST
        );
    }
    // ================= SPRING SECURITY =================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {

        return ResponseData.fail(
                "You do not have permission to access this resource.",
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Object>> handleDisabledException(
            DisabledException ex
    ) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }

    //==================SAVED_POST=============
    @ExceptionHandler(SavedPostNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleSavedPostNotFound(SavedPostNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SavedPostAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleSavedPostAlreadyExists(SavedPostAlreadyExistsException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PostFriendsOnlyException.class)
    public ResponseEntity<ApiResponse<Object>> handlePostFriendOnly(PostFriendsOnlyException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.FORBIDDEN);
    }



    //==================BLOCK==================
    @ExceptionHandler(BlockNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleBlockNotFound(BlockNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BlockAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBlockAlreadyExists(BlockAlreadyExistsException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserBlockedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserBlocked(UserBlockedException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    //===================ROLE==================
    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleRoleNotFound(RoleNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserRoleNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserRoleNotFound(UserRoleNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserRoleAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserRoleAlreadyExists(UserRoleAlreadyExistsException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.CONFLICT);
    }

    //====================USER==================
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleProfileNotFound(ProfileNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserForbidden(ForbiddenException ex) {
        return ResponseData.fail(
                ex.getMessage(),
                HttpStatus.FORBIDDEN
        );
    }


    //====================REPORT==================
    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleReportNotFound(ReportNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ReportAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleReportAlreadyExists(ReportAlreadyExistsException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ReportAlreadyReviewedException.class)
    public ResponseEntity<ApiResponse<Object>> handleReportAlreadyReviewed(ReportAlreadyReviewedException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CannotReportOwnPostException.class)
    public ResponseEntity<ApiResponse<Object>> handleReportOwnPost(CannotReportOwnPostException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    //====================REACTION==================
    @ExceptionHandler(ReactionNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleReactionNotFound(ReactionNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    //====================POST_MEDIA==================
    @ExceptionHandler(MediaNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleMediaNotFound(MediaNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    //====================POST==================
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handlePostNotFound(PostNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidDateRange(InvalidDateRangeException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(CannotSaveOwnPostException.class)
    public ResponseEntity<ApiResponse<Object>> handleSaveOwnPost(CannotSaveOwnPostException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PostPrivateException.class)
    public ResponseEntity<ApiResponse<Object>> handlePostPrivate(PostPrivateException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidImage(InvalidImageException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    //====================REEL==================
    @ExceptionHandler(ReelNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleReelNotFound(ReelNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ReelForbiddenException.class)
    public ResponseEntity<ApiResponse<Object>> handleReelForbidden(ReelForbiddenException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ReelViewNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleReelViewNotFound(ReelViewNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    //====================COMMENT==================

    @ExceptionHandler(CommentAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleCommentAlreadyExists(CommentAlreadyExistsException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CanNotCommentYourself.class)
    public ResponseEntity<ApiResponse<Object>> handleCommentYourself(CanNotCommentYourself ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleCommentNotFound(CommentNotFoundException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // ================= REFRESH TOKEN =================
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleExpiredRefreshToken(RefreshTokenExpiredException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RefreshTokenRevokedException.class)
    public ResponseEntity<ApiResponse<Object>> handleRevokedRefreshToken(RefreshTokenRevokedException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(
            BadCredentialsException ex
    ) {
        return ResponseData.fail(
                "Invalid email or password",
                HttpStatus.UNAUTHORIZED
        );
    }

    // ================= CLOUDINARY =================
    @ExceptionHandler(InvalidMediaException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidImage(InvalidMediaException ex) {
        return ResponseData.fail(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }



    @ExceptionHandler(CloudinaryUploadException.class)
    public ResponseEntity<ApiResponse<Object>> handleUpload(CloudinaryUploadException ex) {

        return ResponseData.fail(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CloudinaryDeleteException.class)
    public ResponseEntity<ApiResponse<Object>> handleDelete(CloudinaryDeleteException ex) {

        return ResponseData.fail(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    // ================= JSON =================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex
    ) {

        if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {

            if (invalidFormatException.getTargetType() != null
                    && invalidFormatException.getTargetType().isEnum()) {

                return ResponseData.fail(
                        "Invalid enum value",
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        return ResponseData.fail(
                "Invalid request body",
                HttpStatus.BAD_REQUEST
        );
    }
    // ================= VALIDATION =================
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseData.fail("Invalid parameter type", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ApiResponse<Object> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage("Validation failed");
        response.setData(errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {

        Map<String, String> errors = new HashMap<>();
        errors.put("error", ex.getMessage());

        ApiResponse<Object> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage("Invalid argument");
        response.setData(errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ================= CLIENT DISCONNECT / ABORT =================
    @ExceptionHandler({
            ClientAbortException.class,
            AsyncRequestNotUsableException.class
    })
    public void handleClientAbortException(Exception ex) {
        log.warn("Client aborted connection | userId={} | msg={}",
                getUserId(),
                ex.getMessage()
        );
    }

    // ================= FALLBACK (ONLY IMPORTANT LOG) =================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        if (isClientAbortException(ex)) {
            log.warn("Client aborted connection (wrapped) | userId={} | msg={}",
                    getUserId(),
                    ex.getMessage()
            );
            return null;
        }

        log.error("INTERNAL_SERVER_ERROR | userId={} | msg={}",
                getUserId(),
                ex.getMessage(),
                ex
        );

        return ResponseData.fail(
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private boolean isClientAbortException(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof ClientAbortException || cause instanceof AsyncRequestNotUsableException) {
                return true;
            }
            String message = cause.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("broken pipe") ||
                        lower.contains("connection reset") ||
                        lower.contains("connection was aborted") ||
                        lower.contains("forcibly closed by the remote host")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }
}