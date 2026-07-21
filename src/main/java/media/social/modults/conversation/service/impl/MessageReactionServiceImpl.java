package media.social.modults.conversation.service.impl;


import lombok.AllArgsConstructor;
import media.social.modults.conversation.dto.response.*;
import media.social.modults.conversation.entity.Message;
import media.social.modults.conversation.entity.MessageReaction;
import media.social.modults.conversation.exception.MessageNotFoundException;
import media.social.modults.conversation.repository.MessageReactionRepository;
import media.social.modults.conversation.repository.MessageRepository;
import media.social.modults.conversation.service.MessageReactionService;
import media.social.modults.post.enums.ReactionType;
import media.social.modults.user.entity.User;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class MessageReactionServiceImpl
        implements MessageReactionService {
    private final MessageReactionRepository messageReactionRepository;

    private final MessageRepository messageRepository;

    private final UserServiceDomain userServiceDomain;

    private Message getMessage(Long messageId){

        return messageRepository.findById(messageId)
                .orElseThrow(
                        () -> new MessageNotFoundException(
                                "Message not found"
                        )
                );
    }
    @Override
    @Transactional
    public void react(
            Long messageId,
            ReactionType type
    ){
        Long userId =
                UserContextHolder.getUserId();
        Message message =
                getMessage(messageId);
        User user =
                userServiceDomain.getByUserId(userId);
        MessageReaction reaction =
                messageReactionRepository
                        .findByUserIdAndMessageId(
                                userId,
                                messageId
                        )
                        .orElse(null);
        if(reaction == null){

            reaction =
                    MessageReaction.builder()
                            .user(user)
                            .message(message)
                            .type(type)
                            .build();
            messageReactionRepository.save(
                    reaction
            );
            return;
        }
        if(reaction.getType() != type){

            reaction.setType(type);

            messageReactionRepository.save(
                    reaction
            );
        }

    }
    @Override
    @Transactional
    public void removeReaction(
            Long messageId
    ){

        Long userId =
                UserContextHolder.getUserId();
        getMessage(messageId);
        MessageReaction reaction =
                messageReactionRepository
                        .findByUserIdAndMessageId(
                                userId,
                                messageId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Reaction not found"
                                )
                        );
        messageReactionRepository.delete(
                reaction
        );

    }

    @Override
    @Transactional(readOnly = true)
    public MessageReactionResponse getMyReaction(
            Long messageId
    ){
        Long userId =
                UserContextHolder.getUserId();
        getMessage(messageId);
        MessageReaction reaction =
                messageReactionRepository
                        .findByUserIdAndMessageId(
                                userId,
                                messageId
                        )
                        .orElse(null);

        if(reaction == null){

            return MessageReactionResponse.builder()
                    .reacted(false)
                    .type(null)
                    .build();
        }
        return MessageReactionResponse.builder()
                .reacted(true)
                .type(reaction.getType())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MessageReactionCountResponse countReaction(
            Long messageId
    ){
        getMessage(messageId);

        Map<ReactionType,Long> counts =
                new EnumMap<>(ReactionType.class);

        for(ReactionType type : ReactionType.values()){

            counts.put(
                    type,
                    0L
            );

        }

        List<Object[]> results =
                messageReactionRepository
                        .countReactionsByMessageId(
                                messageId
                        );
        for(Object[] row : results){

            ReactionType type =
                    (ReactionType) row[0];
            Long count =
                    (Long) row[1];
            counts.put(
                    type,
                    count
            );
        }
        return MessageReactionCountResponse.builder()
                .counts(counts)
                .build();

    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageReactionUserResponse> getUsersReacted(
            Long messageId,
            ReactionType type,
            Pageable pageable
    ){
        getMessage(messageId);
        return messageReactionRepository
                .findUsersReacted(
                        messageId,
                        type,
                        pageable
                );
    }
}