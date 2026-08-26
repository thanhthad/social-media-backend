package media.social.modules.conversation.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.conversation.dto.projection.ConversationListProjection;
import media.social.modules.conversation.dto.projection.ConversationMemberProjection;
import media.social.modules.conversation.dto.projection.DatingConversationListProjection;
import media.social.modules.conversation.dto.projection.UnreadCountProjection;
import media.social.modules.conversation.dto.request.CreateGroupRequest;
import media.social.modules.conversation.dto.request.UpdateGroupNameRequest;
import media.social.modules.conversation.dto.response.ConversationListResponse;
import media.social.modules.conversation.dto.response.ConversationMemberResponse;
import media.social.modules.conversation.dto.response.ConversationResponse;
import media.social.modules.conversation.dto.response.DatingConversationListResponse;
import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.entity.ConversationMember;
import media.social.modules.conversation.entity.ConversationMemberId;
import media.social.modules.conversation.enums.ConversationType;
import media.social.modules.conversation.exception.ConversationNotFoundException;
import media.social.modules.conversation.repository.ConversationMemberRepository;
import media.social.modules.conversation.repository.ConversationRepository;
import media.social.modules.conversation.service.ConversationService;
import media.social.modules.conversation.service.domain.ConversationDomainService;
import media.social.modules.conversation.websocket.ConversationPublisher;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.file.media.upload.service.MediaUploadService;
import media.social.modules.post.enums.MediaType;
import media.social.modules.user.entity.User;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMemberRepository conversationMemberRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationDomainService conversationService;
    private final UserServiceDomain userServiceDomain;
    private final MediaUploadService mediaUploadService;
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

        if (request.getAvatar() != null
                && !request.getAvatar().isEmpty()) {

            UploadFileResponse upload =
                    mediaUploadService.upload(request.getAvatar(), MediaUploadContext.MESSAGE);

            conversation.setAvatarUrl(upload.getFileUrl());
            conversation.setAvatarPublicId(upload.getPublicId());
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
        UploadFileResponse upload =
                mediaUploadService.upload(file, MediaUploadContext.MESSAGE);
        if (conversation.getAvatarPublicId() != null) {
            mediaUploadService.delete(conversation.getAvatarPublicId(), MediaType.IMAGE);
        }
        conversation.setAvatarUrl(upload.getFileUrl());
        conversation.setAvatarPublicId(upload.getPublicId());
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
        if (conversation.getAvatarPublicId() != null) {
            mediaUploadService.delete(conversation.getAvatarPublicId(), MediaType.IMAGE);
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

    private ConversationResponse mapToResponse(
            Conversation conversation,
            Long currentUserId
    ) {

        List<ConversationMemberProjection> projections =
                conversationMemberRepository
                        .findMembersProjectionByConversationId(
                                conversation.getId()
                        );

        List<ConversationMemberResponse> members =
                projections.stream()
                        .map(m -> ConversationMemberResponse.builder()
                                .userId(m.getUserId())
                                .username(m.getUsername())
                                .avatarUrl(m.getAvatarUrl())
                                .build()
                        )
                        .toList();

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

        return ConversationResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .members(members)
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationListResponse> getMyConversations() {

        Long userId = UserContextHolder.getUserId();

        List<ConversationListProjection> conversations =
                conversationRepository.findConversationList(
                        userId,
                        ConversationType.PRIVATE,
                        ConversationType.DATING
                );

        if (conversations.isEmpty()) {
            return List.of();
        }

        List<Long> conversationIds =
                conversations.stream()
                        .map(ConversationListProjection::getConversationId)
                        .toList();

        List<UnreadCountProjection> unreadCounts =
                conversationMemberRepository
                        .countUnreadMessagesByConversationIds(
                                userId,
                                conversationIds
                        );

        Map<Long, Long> unreadCountMap =
                unreadCounts.stream()
                        .collect(Collectors.toMap(
                                UnreadCountProjection::getConversationId,
                                UnreadCountProjection::getUnreadCount
                        ));

        return conversations.stream()
                .map(c ->
                        mapToConversationListResponse(
                                c,
                                unreadCountMap.getOrDefault(
                                        c.getConversationId(),
                                        0L
                                )
                        )
                )
                .toList();
    }

    private ConversationListResponse mapToConversationListResponse(
            ConversationListProjection c,
            Long unreadCount
    ) {

        return ConversationListResponse.builder()
                .conversation_id(c.getConversationId())
                .user_id(c.getUserId())
                .type(c.getType())
                .displayName(c.getDisplayName())
                .avatarUrl(c.getAvatarUrl())
                .preview(c.getPreview())
                .lastMessageAt(c.getLastMessageAt())
                .unreadCount(unreadCount)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public List<DatingConversationListResponse> getMyDatingConversations() {

        Long userId = UserContextHolder.getUserId();

        List<DatingConversationListProjection> conversations =
                conversationRepository.findDatingConversationList(
                        userId,
                        ConversationType.DATING
                );

        if (conversations.isEmpty()) {
            return List.of();
        }

        List<Long> conversationIds =
                conversations.stream()
                        .map(DatingConversationListProjection::getConversationId)
                        .toList();

        List<UnreadCountProjection> unreadCounts =
                conversationMemberRepository
                        .countUnreadMessagesByConversationIds(
                                userId,
                                conversationIds
                        );

        Map<Long, Long> unreadCountMap =
                unreadCounts.stream()
                        .collect(Collectors.toMap(
                                UnreadCountProjection::getConversationId,
                                UnreadCountProjection::getUnreadCount
                        ));

        return conversations.stream()
                .map(c ->
                        mapToDatingConversationListResponse(
                                c,
                                unreadCountMap.getOrDefault(
                                        c.getConversationId(),
                                        0L
                                )
                        )
                )
                .toList();
    }

    private DatingConversationListResponse mapToDatingConversationListResponse(
            DatingConversationListProjection c,
            Long unreadCount
    ) {

        return DatingConversationListResponse.builder()
                .conversation_id(c.getConversationId())
                .datingProfile_id(c.getDatingProfileId())
                .type(c.getType())
                .displayName(c.getDisplayName())
                .avatarUrl(c.getAvatarUrl())
                .preview(c.getPreview())
                .lastMessageAt(c.getLastMessageAt())
                .unreadCount(unreadCount)
                .build();
    }
}