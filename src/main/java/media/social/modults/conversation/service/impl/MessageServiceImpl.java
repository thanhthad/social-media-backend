package media.social.modults.conversation.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modults.conversation.dto.projection.MessageProjection;
import media.social.modults.conversation.dto.request.CreateMessageRequest;
import media.social.modults.conversation.dto.response.MessageMediaResponse;
import media.social.modults.conversation.dto.response.MessageResponse;
import media.social.modults.conversation.entity.Conversation;
import media.social.modults.conversation.entity.Message;
import media.social.modults.conversation.entity.MessageMedia;
import media.social.modults.conversation.exception.ConversationNotFoundException;
import media.social.modults.conversation.exception.InvalidMediaException;
import media.social.modults.conversation.exception.MessageNotFoundException;
import media.social.modults.conversation.repository.ConversationMemberRepository;
import media.social.modults.conversation.repository.ConversationRepository;
import media.social.modults.conversation.repository.MessageMediaRepository;
import media.social.modults.conversation.repository.MessageRepository;
import media.social.modults.conversation.service.MessageService;
import media.social.modults.file.image.dto.response.UploadFileResponse;
import media.social.modults.file.image.service.CloudinaryService;
import media.social.modults.post.enums.MediaType;
import media.social.modults.user.entity.User;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserServiceDomain userServiceDomain;
    private final CloudinaryService cloudinaryService;
    private final MessageMediaRepository messageMediaRepository;

    @Override
    @Transactional
    public MessageResponse create(CreateMessageRequest request) {
        validateMessage(request);

        Long userId = UserContextHolder.getUserId();

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found"));

        checkMember(conversation.getId(), userId);

        User sender = userServiceDomain.getByUserId(userId);

        Message replyMessage = null;

        if(request.getReplyToMessageId() != null) {

            replyMessage = messageRepository.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new MessageNotFoundException("Reply message not found"));

            if(!replyMessage.getConversation().getId().equals(conversation.getId())) {
                throw new IllegalArgumentException("Reply message not belong conversation");
            }
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .replyToMessage(replyMessage)
                .build();

        if(request.getFiles()!=null) {

            List<MessageMedia> medias = new ArrayList<>();

            for(MultipartFile file : request.getFiles()) {

                MediaType type = detectMediaType(file);

                UploadFileResponse upload =
                        cloudinaryService.uploadFile(
                                file,
                                "conversation/message",
                                type
                        );

                MessageMedia media =
                        MessageMedia.builder()
                                .message(message)
                                .url(upload.getFileUrl())
                                .publicId(upload.getPublicId())
                                .mediaType(type)
                                .build();

                medias.add(media);
            }

            message.getMedia().addAll(medias);
        }

        Message saved = messageRepository.save(message);

        conversation.setLastMessageAt(saved.getCreatedAt());

        conversationRepository.save(conversation);

        return mapToResponse(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(
            Long conversationId,
            Pageable pageable
    ) {
        Long userId = UserContextHolder.getUserId();

        checkMember(conversationId, userId);

        Page<MessageProjection> messages =
                messageRepository.findMessages(
                        conversationId,
                        pageable
                );

        List<Long> messageIds =
                messages.getContent()
                        .stream()
                        .map(MessageProjection::getId)
                        .toList();

        List<MessageMedia> medias =
                messageMediaRepository.findByMessageIds(
                        messageIds
                );

        Map<Long, List<MessageMediaResponse>> mediaMap =
                medias.stream()
                        .collect(Collectors.groupingBy(
                                media -> media.getMessage().getId(),
                                Collectors.mapping(
                                        media -> MessageMediaResponse.builder()
                                                .id(media.getId())
                                                .url(media.getUrl())
                                                .mediaType(
                                                        media.getMediaType().name()
                                                )
                                                .build(),
                                        Collectors.toList()
                                )
                        ));

        return messages.map(message ->
                MessageResponse.builder()
                        .id(message.getId())
                        .senderId(
                                message.getSenderId()
                        )
                        .senderName(
                                message.getSenderName()
                        )
                        .avatarUrl(
                                message.getAvatarUrl()
                        )
                        .content(
                                message.getContent()
                        )
                        .replyToMessageId(
                                message.getReplyToMessageId()
                        )
                        .medias(
                                mediaMap.getOrDefault(
                                        message.getId(),
                                        List.of()
                                )
                        )
                        .createdAt(
                                message.getCreatedAt()
                        )
                        .build()
        );
    }


    @Override
    @Transactional(readOnly = true)
    public MessageResponse findById(Long messageId) {

        Long userId = UserContextHolder.getUserId();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found"));

        checkMember(
                message.getConversation().getId(),
                userId
        );

        return mapToResponse(message);
    }


    @Override
    @Transactional
    public void delete(Long messageId) {

        Long userId = UserContextHolder.getUserId();

        Message message = messageRepository.findByIdWithSenderAndMedia(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found"));

        if(!message.getSender().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "Only sender can delete message"
            );
        }

        message.getMedia()
                .forEach(media -> {

                    if(media.getPublicId() != null) {
                        cloudinaryService.deleteFile(
                                media.getPublicId(),
                                media.getMediaType()
                        );
                    }
                });

        message.setDeleted(true);
        message.setContent("Tin nhắn đã bị xóa");
    }


    private void checkMember(
            Long conversationId,
            Long userId
    ) {

        if(!conversationMemberRepository
                .existsByConversationIdAndUserId(
                        conversationId,
                        userId
                )) {

            throw new AccessDeniedException(
                    "You are not member"
            );
        }
    }


    private MessageResponse mapToResponse(Message message) {

        return MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getUsername())
                .content(message.getContent())
                .replyToMessageId(
                        message.getReplyToMessage() == null
                                ? null
                                : message.getReplyToMessage().getId()
                )
                .medias(
                        message.getMedia()
                                .stream()
                                .map(media ->
                                        MessageMediaResponse.builder()
                                                .id(media.getId())
                                                .url(media.getUrl())
                                                .mediaType(media.getMediaType().name())
                                                .build()
                                )
                                .toList()
                )
                .createdAt(message.getCreatedAt())
                .build();
    }


    private MediaType detectMediaType(MultipartFile file){

        if(file == null || file.isEmpty()){
            throw new InvalidMediaException(
                    "File empty"
            );
        }

        String filename = file.getOriginalFilename();

        if(filename == null || !filename.contains(".")){
            throw new InvalidMediaException(
                    "Invalid filename"
            );
        }

        String extension =
                filename.substring(
                        filename.lastIndexOf(".")+1
                ).toLowerCase();


        if(List.of(
                "jpg",
                "jpeg",
                "png",
                "webp"
        ).contains(extension)){
            return MediaType.IMAGE;
        }


        if(List.of(
                "mp4",
                "mov",
                "avi",
                "mkv",
                "webm"
        ).contains(extension)){
            return MediaType.VIDEO;
        }


        throw new InvalidMediaException(
                "Unsupported file extension"
        );
    }

    private void validateMessage(CreateMessageRequest request){

        boolean hasContent =
                request.getContent()!=null
                        && !request.getContent().isBlank();

        boolean hasMedia =
                request.getFiles()!=null
                        && !request.getFiles().isEmpty();

        if(!hasContent && !hasMedia){
            throw new IllegalArgumentException(
                    "Message cannot be empty"
            );
        }

        if(request.getContent()!=null
                && request.getContent().length()>5000){

            throw new IllegalArgumentException(
                    "Message too long"
            );
        }

        if(request.getFiles()!=null
                && request.getFiles().size()>10){

            throw new IllegalArgumentException(
                    "Maximum 10 files allowed"
            );
        }
    }
}