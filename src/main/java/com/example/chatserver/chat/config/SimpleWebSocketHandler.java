//package com.example.chatserver.chat.config;
//
//import lombok.extern.log4j.Log4j2;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.*;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * SimpleWebSocketHandler
// * - 클라이언트로부터 들어오는 WebSocket 연결, 메시지 송수신, 연결 종료 이벤트를 처리하는 핸들러 클래스
// * - Spring의 TextWebSocketHandler를 상속받아 텍스트 기반 메시지(WebSocket text frame)를 다룸
// */
//@Component
//@Log4j2
//public class SimpleWebSocketHandler extends TextWebSocketHandler {
//
//    /**
//     * 현재 연결된 WebSocket 세션을 저장하는 Set
//     * - ConcurrentHashMap.newKeySet()을 사용해 스레드 안전하게 관리
//     * - 여러 클라이언트가 동시에 접속하거나 종료해도 안전하게 동작
//     */
//    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
//
//    /**
//     * 클라이언트가 WebSocket 서버에 최초로 연결되었을 때 호출됨
//     * - 새로운 세션을 sessions 집합에 추가
//     * - 연결 성공 로그 출력
//     *
//     * @param session 연결된 클라이언트의 세션 객체
//     */
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        sessions.add(session);
//        log.info("Connected: " + session.getId());
//    }
//
//    /**
//     * 클라이언트로부터 텍스트 메시지를 수신했을 때 호출됨
//     * - 전달받은 payload(문자열 메시지)를 로그로 출력
//     * - 연결된 모든 세션에 동일한 메시지를 전송 (브로드캐스트 방식)
//     *
//     * @param session 메시지를 보낸 클라이언트 세션
//     * @param message 클라이언트가 보낸 텍스트 메시지
//     */
//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//        String payload = message.getPayload();
//        log.info("Received message: " + payload);
//
//        // 연결된 모든 세션에 메시지를 전송
//        for (WebSocketSession s : sessions) {
//            if (s.isOpen()) {
//                s.sendMessage(new TextMessage(payload));
//            }
//        }
//    }
//
//    /**
//     * 클라이언트가 WebSocket 연결을 종료했을 때 호출됨
//     * - 종료된 세션을 sessions 집합에서 제거
//     * - 종료 로그 출력
//     *
//     * @param session 종료된 세션 객체
//     * @param status  종료 상태 정보 (예: 정상 종료, 에러 등)
//     */
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
//        sessions.remove(session);
//        log.info("Disconnected: " + session.getId());
//    }
//}
