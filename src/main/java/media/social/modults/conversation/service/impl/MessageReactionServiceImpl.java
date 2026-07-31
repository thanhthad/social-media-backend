package media.social.modults.conversation.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.conversation.dto.response.*;
import media.social.modults.conversation.entity.Message;
import media.social.modults.conversation.entity.MessageReaction;
import media.social.modults.conversation.exception.MessageNotFoundException;
import media.social.modults.conversation.repository.MessageReactionRepository;
import media.social.modults.conversation.repository.MessageRepository;
import media.social.modults.conversation.service.MessageReactionService;
import media.social.modults.conversation.websocket.MessageReactionPublisher;
import media.social.modults.post.enums.ReactionType;
import media.social.modults.user.entity.User;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.domain.UserServiceDomain;
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
    private final MessageReactionPublisher messageReactionPublisher;
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
        MessageReactionResponse response =
                buildReactionResponse(
                        messageId,
                        userId
                );

        messageReactionPublisher.send(
                userId,
                response
        );
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
        MessageReactionResponse response =
                buildReactionResponse(
                        messageId,
                        userId
                );

        messageReactionPublisher.send(
                userId,
                response
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageReactionUserResponse> getUsersReacted(
            Long messageId
    ) {

        getMessage(messageId);

        return messageReactionRepository
                .findUsersReacted(messageId);
    }

    private MessageReactionResponse buildReactionResponse(
            Long messageId,
            Long userId
    ){
        List<MessageReaction> reactions =
                messageReactionRepository
                        .findAllByMessageId(messageId);

        Map<ReactionType, Long> counts =
                new EnumMap<>(ReactionType.class);

        reactions.forEach(reaction -> {

            counts.merge(
                    reaction.getType(),
                    1L,
                    Long::sum
            );

        });

        ReactionType myReaction =
                reactions.stream()
                        .filter(
                                reaction ->
                                        reaction.getUser()
                                                .getId()
                                                .equals(userId)
                        )
                        .map(MessageReaction::getType)
                        .findFirst()
                        .orElse(null);

        return MessageReactionResponse.builder()
                .messageId(messageId)
                .totalReactions(
                        (long) reactions.size()
                )
                .counts(counts)
                .myReaction(myReaction)
                .build();
    }


}