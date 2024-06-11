    package com.spacestar.back.config;

    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.spacestar.back.quickmatching.dto.ChatMessageDto;
    import com.spacestar.back.quickmatching.service.QuickMatchingService;
    import lombok.Getter;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.scheduling.annotation.Scheduled;
    import org.springframework.stereotype.Component;
    import org.springframework.web.socket.TextMessage;
    import org.springframework.web.socket.WebSocketSession;
    import org.springframework.web.socket.handler.TextWebSocketHandler;

    import java.io.IOException;
    import java.util.HashMap;
    import java.util.HashSet;
    import java.util.Map;
    import java.util.Set;
    import java.util.stream.Collectors;

    @Component
    @Slf4j
    @RequiredArgsConstructor
    public class MatchingWebSocketHandler extends TextWebSocketHandler {

        private final QuickMatchingService matchingService;
        private final ObjectMapper objectMapper;
        private final RedisTemplate<String,String> redisTemplate;
        @Getter
        private final Map<String, Set<WebSocketSession>> chatRoomSessionMap = new HashMap<>();

        // 웹소켓 연결
        @Override
        public void afterConnectionEstablished(WebSocketSession session){
            log.info("웹소켓 연결 - {}", session.getId());
            matchingService.connectSocket(session);
        }

        // 양방향 데이터 통신 - 클라이언트에서 메세지를 보내오는 경우
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            log.info("양방향 데이터 통신 - {}", session.getId());

            String payload = message.getPayload();
            log.info("payload {}", payload);

            // 페이로드 -> chatMessageDto로 변환
            ChatMessageDto chatMessageDto = objectMapper.readValue(payload, ChatMessageDto.class);
            log.info("session {}", chatMessageDto.toString());
            String gameName = chatMessageDto.getGameName();

            if (!chatRoomSessionMap.containsKey(gameName)) {
                chatRoomSessionMap.put(gameName, new HashSet<>());
            }

            Set<WebSocketSession> chatRoomSession = chatRoomSessionMap.get(gameName);

            if (chatMessageDto.getMessageType().equals(ChatMessageDto.MessageType.ENTER)) {
                chatRoomSession.add(session);
                session.getAttributes().put("memberUuid",chatMessageDto.getMemberUuid());
                matchingService.enterQuickMatching(chatMessageDto.getMemberUuid(),gameName);
            } else if (chatMessageDto.getMessageType().equals(ChatMessageDto.MessageType.TALK)) {
                sendMessageToChatRoom(chatMessageDto, chatRoomSession);
            }
        }

        private void sendMessageToChatRoom(ChatMessageDto chatMessageDto, Set<WebSocketSession> chatRoomSession) {
            chatRoomSession.parallelStream().forEach(sess -> sendMessage(sess, chatMessageDto));

        }

        public <T> void sendMessage(WebSocketSession session, T message) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        @Scheduled(fixedDelay = 5_000L)
        public void quickMatch(){
            for (Map.Entry<String, Set<WebSocketSession>> entry : getChatRoomSessionMap().entrySet()) {
                String gameName = entry.getKey();
                Set<WebSocketSession> chatRoomSessions = entry.getValue();

                for (WebSocketSession session : chatRoomSessions) {
                    log.info("Matched user {} in game {}", session.getAttributes().get("memberUuid"), gameName);
                }
            }
        }
    }