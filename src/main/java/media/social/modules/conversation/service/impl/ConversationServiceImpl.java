package media.social.modules.conversation.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.conversation.dto.request.CreateGroupRequest;
import media.social.modules.conversation.dto.request.UpdateGroupNameRequest;
import media.social.modules.conversation.dto.response.ConversationListResponse;
import media.social.modules.conversation.dto.response.ConversationMemberResponse;
import media.social.modules.conversation.dto.response.ConversationResponse;
import media.social.modules.conversation.dto.response.LastMessageResponse;
import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.entity.ConversationMember;
import media.social.modules.conversation.entity.ConversationMemberId;
import media.social.modules.conversation.entity.Message;
import media.social.modules.conversation.enums.ConversationType;
import media.social.modules.conversation.exception.ConversationNotFoundException;
import media.social.modules.conversation.repository.ConversationMemberRepository;
import media.social.modules.conversation.repository.ConversationRepository;
import media.social.modules.conversation.repository.MessageRepository;
import media.social.modules.conversation.service.ConversationService;
import media.social.modules.conversation.service.domain.ConversationDomainService;
import media.social.modules.conversation.websocket.ConversationPublisher;
import media.social.modules.file.image.dto.response.UploadFileResponse;
import media.social.modules.file.image.service.CloudinaryService;
import media.social.modules.post.enums.MediaType;
import media.social.modules.user.entity.User;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMemberRepository conversationMemberRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationDomainService conversationService;
    private final MessageRepository messageRepository;
    private final UserServiceDomain userServiceDomain;
    private final CloudinaryService cloudinaryService;
    private final ConversationPublisher conversationPublisher;

    @Override
    @Transactional
    public ConversationResponse createPrivateConversation(Long targetUserId) {

        Long userId = UserContextHolder.getUserId();

        if(userId.equals(targetUserId)){
            throw new IllegalArgumentException(
                    "Cannot create conversation with yourself"
            );
        }
        User currentUser = userServiceDomain.getByUserId(userId);

        User targetUser = userServiceDomain.getByUserId(targetUserId);

        Optional<Conversation> existingConversation =
                conversationMemberRepository.findPrivateConversation(
                        userId,
                        targetUserId
                );
        if (existingConversation.isPresent()) {
            Conversation conversation = existingConversation.get();
            return mapToResponse(conversation,userId);
        }
        Conversation conversation =
                conversationService.create(
                        ConversationType.PRIVATE
                );
        ConversationMember currentMember =
                ConversationMember.builder()
                        .id(
                                new ConversationMemberId(
                                        conversation.getId(),
                                        userId
                                )
                        )
                        .conversation(conversation)
                        .user(currentUser)
                        .build();

        ConversationMember targetMember =
                ConversationMember.builder()
                        .id(
                                new ConversationMemberId(
                                        conversation.getId(),
                                        targetUserId
                                )
                        )
                        .conversation(conversation)
                        .user(targetUser)
                        .build();

        conversationMemberRepository.save(currentMember);
        conversationMemberRepository.save(targetMember);

        ConversationResponse response =
                mapToResponse(
                        conversation,
                        userId
                );

        conversationPublisher.sendToUser(
                targetUserId,
                response
        );

        return response;
    }

    @Override
    @Transactional
    public ConversationResponse createGroupConversation(
            CreateGroupRequest request
    ) {
        Long userId = UserContextHolder.getUserId();

        User currentUser =
                userServiceDomain.getByUserId(userId);

        if(request.getMemberIds() == null
                || request.getMemberIds().isEmpty()) {

            throw new IllegalArgumentException(
                    "Group must have members"
            );
        }
        Conversation conversation =
                conversationService.create(
                        ConversationType.GROUP
                );
        conversation.setOwner(currentUser);
        conversation.setName(request.getName());

        if(request.getAvatar() != null
                && !request.getAvatar().isEmpty()) {
            cloudinaryService.validateFile(
                    request.getAvatar(),
                    MediaType.IMAGE
            );
            UploadFileResponse upload =
                    cloudinaryService.uploadFile(
                            request.getAvatar(),
                            "conversation/avatar",
                            MediaType.IMAGE
                    );
            conversation.setAvatarUrl(
                    upload.getFileUrl()
            );
            conversation.setAvatarPublicId(
                    upload.getPublicId()
            );
        }
        conversationRepository.save(conversation);
        ConversationMember owner =
                ConversationMember.builder()
                        .id(
                                new ConversationMemberId(
                                        conversation.getId(),
                                        userId
                                )
                        )
                        .conversation(conversation)
                        .user(currentUser)
                        .build();
        conversationMemberRepository.save(owner);
        List<ConversationMember> members =
                request.getMemberIds()
                        .stream()
                        .filter(id -> !id.equals(userId))
                        .distinct()
                        .map(id -> {
                            User user =
                                    userServiceDomain
                                            .getByUserId(id);
                            return ConversationMember.builder()
                                    .id(
                                            new ConversationMemberId(
                                                    conversation.getId(),
                                                    id
                                            )
                                    )
                                    .conversation(conversation)
                                    .user(user)
                                    .build();

                        })
                        .toList();
        conversationMemberRepository.saveAll(members);
        ConversationResponse response =
                mapToResponse(
                        conversation,
                        userId
                );

        members.forEach(member -> {
            conversationPublisher.sendToUser(
                    member.getUser().getId(),
                    response
            );

        });

        return response;
    }

    @Transactional
    public void updateGroupAvatar(
            Long conversationId,
            MultipartFile file
    ){
        Conversation conversation =
                conversationRepository.findById(conversationId)
                        .orElseThrow(
                                () -> new ConversationNotFoundException(
                                        "Conversation not found"
                                )
                        );
        Long userId = UserContextHolder.getUserId();
        if(conversation.getType()
                != ConversationType.GROUP){

            throw new IllegalArgumentException(
                    "Only group can update avatar"
            );
        }
        if(!conversation.getOwner()
                .getId()
                .equals(userId)){

            throw new AccessDeniedException(
                    "Only owner can update avatar"
            );
        }
        cloudinaryService.validateFile(
                file,
                MediaType.IMAGE
        );
        UploadFileResponse upload =
                cloudinaryService.uploadFile(
                        file,
                        "conversation/avatar",
                        MediaType.IMAGE
                );
        if(conversation.getAvatarPublicId()!=null){

            cloudinaryService.deleteFile(
                    conversation.getAvatarPublicId(),
                    MediaType.IMAGE
            );
        }
        conversation.setAvatarUrl(
                upload.getFileUrl()
        );
        conversation.setAvatarPublicId(
                upload.getPublicId()
        );
    }

    @Transactional
    public void updateGroupName(
            Long conversationId,
            UpdateGroupNameRequest request
    ){
        Long userId = UserContextHolder.getUserId();
        Conversation conversation =
                conversationRepository.findById(conversationId)
                        .orElseThrow(
                                () -> new ConversationNotFoundException(
                                        "Conversation not found"
                                )
                        );
        if(conversation.getType()
                != ConversationType.GROUP){

            throw new IllegalArgumentException(
                    "Only group can update name"
            );
        }
        if(!conversation.getOwner()
                .getId()
                .equals(userId)){

            throw new AccessDeniedException(
                    "Only owner can update group"
            );
        }
        conversation.setName(
                request.getName()
        );
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId){
        Long userId =
                UserContextHolder.getUserId();
        Conversation conversation =
                conversationRepository.findByIdWithOwner(conversationId)
                        .orElseThrow(
                                () -> new ConversationNotFoundException(
                                        "Conversation not found"
                                )
                        );
        if(conversation.getType()
                != ConversationType.GROUP){

            throw new IllegalArgumentException(
                    "Cannot delete private conversation"
            );
        }
        if(!conversation.getOwner()
                .getId()
                .equals(userId)){

            throw new AccessDeniedException(
                    "Only owner can delete group"
            );
        }
        if(conversation.getAvatarPublicId()!=null){

            cloudinaryService.deleteFile(
                    conversation.getAvatarPublicId(),
                    MediaType.IMAGE
            );
        }
        conversationRepository.delete(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversationDetail(
            Long conversationId
    ) {

        Long userId = UserContextHolder.getUserId();

        if(!conversationMemberRepository
                .existsByConversationIdAndUserId(
                        conversationId,
                        userId
                )){
            throw new AccessDeniedException(
                    "You are not a member"
            );
        }

        Conversation conversation =
                conversationRepository.findById(conversationId)
                        .orElseThrow(() ->
                                new ConversationNotFoundException(
                                        "Conversation not found"
                                ));

        return mapToResponse(
                conversation,
                userId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationListResponse> getMyConversations() {

        Long userId = UserContextHolder.getUserId();

        List<Conversation> conversations =
                conversationMemberRepository
                        .findConversationsByUserId(userId);

        return conversations.stream()
                .map(c -> mapToConversationListResponse(c, userId))
                .toList();
    }

    private ConversationListResponse mapToConversationListResponse(
            Conversation conversation,
            Long currentUserId
    ) {

        List<ConversationMemberResponse> members =
                conversationMemberRepository
                        .findMembersByConversationId(
                                conversation.getId()
                        );

        String displayName;
        String avatarUrl;

        if (conversation.getType() == ConversationType.PRIVATE) {

            ConversationMemberResponse target =
                    members.stream()
                            .filter(m ->
                                    !m.getUserId().equals(currentUserId)
                            )
                            .findFirst()
                            .orElseThrow();

            displayName = target.getUsername();
            avatarUrl = target.getAvatarUrl();

        } else {

            displayName = conversation.getName();
            avatarUrl = conversation.getAvatarUrl();
        }

        LastMessageResponse lastMessage = null;

        if(conversation.getLastMessage() != null) {
            Message message =
                    conversation.getLastMessage();
            lastMessage =
                    LastMessageResponse.builder()
                            .id(message.getId())
                            .preview(
                                    buildPreview(message)
                            )
                            .senderId(
                                    message.getSender()
                                            .getId()
                            )
                            .senderName(
                                    message.getSender()
                                            .getUsername()
                            )
                            .createdAt(
                                    message.getCreatedAt()
                            )
                            .build();
        }

        long unreadCount =
                messageRepository.countUnreadMessages(
                        conversation.getId(),
                        currentUserId
                );

        return ConversationListResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    private String buildPreview(Message message) {

        if (message.getContent() != null &&
                !message.getContent().isBlank()) {
            return message.getContent();
        }

        int imageCount = message.getMedia()
                .stream()
                .filter(m -> m.getMediaType() == MediaType.IMAGE)
                .toList()
                .size();

        int videoCount = message.getMedia()
                .stream()
                .filter(m -> m.getMediaType() == MediaType.VIDEO)
                .toList()
                .size();

        if (imageCount > 0 && videoCount == 0) {
            return imageCount == 1
                    ? "Đã gửi một hình ảnh"
                    : "Đã gửi " + imageCount + " hình ảnh";
        }

        if (videoCount > 0 && imageCount == 0) {
            return videoCount == 1
                    ? "Đã gửi một video"
                    : "Đã gửi " + videoCount + " video";
        }

        if (imageCount > 0 && videoCount > 0) {
            return "Đã gửi tệp đính kèm";
        }

        return "";
    }

    private ConversationResponse mapToResponse(
            Conversation conversation,
            Long currentUserId
    ){
        List<ConversationMemberResponse> members =
                conversationMemberRepository
                        .findMembersByConversationId(
                                conversation.getId()
                        );
        String displayName;
        String avatarUrl;
        if(conversation.getType()
                == ConversationType.PRIVATE){
            ConversationMemberResponse target =
                    members.stream()
                            .filter(m ->
                                    !m.getUserId()
                                            .equals(currentUserId)
                            )
                            .findFirst()
                            .orElseThrow();
            displayName = target.getUsername();
            avatarUrl = target.getAvatarUrl();
        }else {
            displayName = conversation.getName();
            avatarUrl = conversation.getAvatarUrl();
        }
        return ConversationResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .members(members)
                .createdAt(conversation.getCreatedAt())
                .build();
    }
}