package com.spacestar.back.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spacestar.back.quickmatching.dto.ChatMessageDto;
import com.spacestar.back.quickmatching.dto.WebSocketInfo;
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
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class MatchingWebSocketHandler extends TextWebSocketHandler {

    private final QuickMatchingService matchingService;
    private final ObjectMapper objectMapper;
    @Getter
    private final Map<String, Set<WebSocketSession>> chatRoomSessionMap = new HashMap<>();

    // 웹소켓 연결
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
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
            session.getAttributes().put("memberUuid", chatMessageDto.getMemberUuid());
            matchingService.enterQuickMatching(chatMessageDto.getMemberUuid(), gameName);
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
    public void quickMatch() {
        Map<String, List<String>> quickMember = new HashMap<>();
        Map<String, List<WebSocketSession>> socketMember = new HashMap<>();

        // 게임명별로 접속한 유저들을 quickMember에 저장
        for (Map.Entry<String, Set<WebSocketSession>> entry : getChatRoomSessionMap().entrySet()) {
            String gameName = entry.getKey();
            Set<WebSocketSession> chatRoomSessions = entry.getValue();

            if (!quickMember.containsKey(gameName)) {
                quickMember.put(gameName, new ArrayList<>());
                socketMember.put(gameName, new ArrayList<>());
            }

            for (WebSocketSession session : chatRoomSessions) {
                String memberUuid = (String) session.getAttributes().get("memberUuid");
                quickMember.get(gameName).add(memberUuid);
                socketMember.get(gameName).add(session);
                log.info("Added user {} in game {}", memberUuid, gameName);
            }
        }

        // 매칭 서비스 호출
        for (Map.Entry<String, List<String>> entry : quickMember.entrySet()) {
            String gameName = entry.getKey();
            List<String> members = entry.getValue();
            List<String> matchedMembers = matchingService.doQuickMatch(gameName, members, socketMember.get(gameName));
            if(matchedMembers.size()>1) {
                for (Map.Entry<String, Set<WebSocketSession>> socketEntry : chatRoomSessionMap.entrySet()) {
                    socketEntry.getValue().removeIf(s -> Objects.equals(s.getAttributes().get("memberUuid"), matchedMembers.get(1)));
                    socketEntry.getValue().removeIf(s -> Objects.equals(s.getAttributes().get("memberUuid"), matchedMembers.get(0)));
                }
            }
        }
    }

}