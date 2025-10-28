package com.example.chatserver.chat.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * STOMP 프로토콜 기반 WebSocket 통신에서
 * 클라이언트의 CONNECT 요청 시 JWT 토큰을 검증하기 위한 인터셉터
 *
 * ChannelInterceptor를 구현하여
 * STOMP 메시지가 Broker(메시지 브로커)로 전달되기 전에 가로채어 검사할 수 있음.
 */
@Component
@Log4j2
public class StompHandler implements ChannelInterceptor {

    @Value("${jwt.secretKey}")
    private String secretKey;

    /**
     * WebSocket 통신 중 모든 요청(CONNECT, SUBSCRIBE, SEND, DISCONNECT 등)은
     * Broker로 전달되기 전 이 메서드를 통과함.
     * 즉, WebSocket 연결 시 클라이언트의 JWT 유효성을 검증하는 역할을 수행.
     *
     * @param message 클라이언트로부터 들어온 STOMP 메시지
     * @param channel 메시지가 전송될 채널
     * @return 원본 메시지 (검증 성공 시 그대로 반환)
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // STOMP 헤더 정보를 추출하기 위한 객체
        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        /**
         * STOMP 명령어 종류:
         * - CONNECT: 클라이언트가 WebSocket 연결을 시도할 때
         * - SUBSCRIBE: 특정 topic 구독 요청
         * - SEND: 메시지 전송 요청
         * - DISCONNECT: 연결 해제 요청
         */
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("Connect 요청 감지 → 토큰 유효성 검증 시작");

            // 클라이언트가 보낸 Authorization 헤더 추출
            String bearerToken = accessor.getFirstNativeHeader("Authorization");

            // "Bearer " 부분을 잘라내고 실제 JWT 문자열만 추출
            String token = bearerToken.substring(7);

            // JWT 서명 키(secretKey)를 사용해 토큰의 유효성을 검증
            // - 서명이 유효하지 않거나 만료된 경우 예외 발생
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            log.info("토큰 검증 완료 - user info: {}", claims.getSubject());
        }

        // 검증 완료 후, 메시지를 그대로 다음 단계로 전달 (Broker로 전달됨)
        return message;
    }
}