package com.example.chatserver.chat.config;

import com.example.chatserver.chat.service.ChatService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;

/**
 * STOMP 프로토콜 기반 WebSocket 통신에서
 * 클라이언트의 CONNECT, SUBSCRIBE 요청 시 JWT 토큰을 검증하고
 * 채팅방 접근 권한을 확인하기 위한 인터셉터 클래스.
 *
 * ChannelInterceptor를 구현하면,
 * STOMP 메시지가 Broker(메시지 브로커)로 전달되기 전에
 * preSend() 메서드를 통해 메시지를 가로채어 검사할 수 있다.
 */
@Component
@Log4j2
public class StompHandler implements ChannelInterceptor {

    /**
     * application.yml 에 정의된 JWT 서명용 비밀키(secretKey)를 주입받음.
     * JWT 검증 시 서명(signature) 검증에 사용된다.
     */
    @Value("${jwt.secretKey}")
    private String secretKey;

    private final ChatService chatService;

    /**
     * ChatService를 주입받음.
     * → 사용자가 특정 채팅방에 속해 있는지 확인하는 로직에 활용.
     */
    public StompHandler(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * STOMP 통신 중 발생하는 모든 명령(CONNECT, SUBSCRIBE, SEND, DISCONNECT 등)은
     * Broker로 전달되기 전에 이 메서드를 반드시 거친다.
     *
     * → 이 메서드에서 JWT 인증 검증, 채팅방 권한 확인 등을 수행할 수 있다.
     *
     * @param message 클라이언트로부터 전달된 STOMP 메시지
     * @param channel 메시지가 전달될 채널
     * @return 검증이 완료된 메시지를 그대로 반환 (Broker로 전달)
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // STOMP 메시지 헤더에 접근하기 위한 래퍼 객체 생성
        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        /**
         * STOMP 명령어 종류 (accessor.getCommand()):
         * - CONNECT: 클라이언트가 최초로 WebSocket 연결을 시도할 때
         * - SUBSCRIBE: 특정 Topic(채팅방 등)을 구독할 때
         * - SEND: 서버로 메시지를 전송할 때
         * - DISCONNECT: 연결 해제 요청
         */
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("[CONNECT] WebSocket 연결 요청 감지 → JWT 토큰 검증 시작");

            // 클라이언트에서 Authorization 헤더를 통해 JWT 전달
            // 예시: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
            String bearerToken = accessor.getFirstNativeHeader("Authorization");
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                throw new AuthenticationServiceException("Authorization 헤더가 없거나 잘못된 형식입니다.");
            }

            // "Bearer " 접두어 제거 후 실제 JWT 문자열만 추출
            String token = bearerToken.substring(7);

            // JWT 유효성 검증 (서명 및 만료 검증)
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // JWT 안에 저장된 사용자 식별 정보(subject = 이메일 등)
            String email = claims.getSubject();

            log.info("[CONNECT] JWT 검증 완료 - 사용자: {}", email);
        }

        /**
         * 클라이언트가 특정 채팅방(topic)을 구독할 때 실행됨.
         * 예: /sub/chat/1 → roomId = 1
         * → JWT를 다시 검증하고, 사용자가 해당 채팅방 참가자인지 확인한다.
         */
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            log.info("[SUBSCRIBE] 채팅방 구독 요청 감지 → JWT 토큰 검증 시작");

            // Authorization 헤더에서 JWT 추출
            String bearerToken = accessor.getFirstNativeHeader("Authorization");
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                throw new AuthenticationServiceException("Authorization 헤더가 없거나 잘못된 형식입니다.");
            }

            // "Bearer " 제거
            String token = bearerToken.substring(7);

            // JWT 검증 및 사용자 정보 추출
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // JWT 안의 subject 값(사용자 이메일)
            String email = claims.getSubject();

            // 구독 대상 채팅방 ID 추출
            // 예: /sub/chat/3 → split("/")[2] = "3"
            String destination = accessor.getDestination();
            String roomId = destination.split("/")[2];

            log.info("[SUBSCRIBE] 사용자: {} → 채팅방: {}", email, roomId);

            // 해당 사용자가 실제로 이 채팅방의 참가자인지 확인
            if (!chatService.isRoomPaticipant(email, Long.parseLong(roomId))) {
                throw new AuthenticationServiceException("해당 채팅방에 접근 권한이 없습니다.");
            }

            log.info("[SUBSCRIBE] 구독 권한 검증 완료 - 사용자: {} (roomId: {})", email, roomId);
        }

        // 검증 완료 후 메시지를 그대로 다음 단계(Broker)로 전달
        return message;
    }
}
