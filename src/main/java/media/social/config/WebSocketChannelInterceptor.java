package media.social.config;

import lombok.RequiredArgsConstructor;
import media.social.modults.user.security.jwt.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;


@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor
        implements ChannelInterceptor {


    private final JwtUtil jwtUtil;


    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(
                accessor.getCommand()
        )) {
            String authHeader =
                    accessor.getFirstNativeHeader(
                            "Authorization"
                    );
            if(authHeader == null ||
                    !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException(
                        "Missing websocket token"
                );
            }
            String token =
                    authHeader.substring(7);

            if(!jwtUtil.isValid(token)) {
                throw new IllegalArgumentException(
                        "Invalid token"
                );
            }
            Long userId =
                    jwtUtil.getUserId(token);
            accessor.setUser(
                    new Principal() {
                        @Override
                        public String getName() {
                            return userId.toString();
                        }
                    }
            );
        }


        return message;
    }
}