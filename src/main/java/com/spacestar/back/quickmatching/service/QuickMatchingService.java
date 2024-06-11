package com.spacestar.back.quickmatching.service;

import org.springframework.web.socket.WebSocketSession;

import java.util.List;

public interface QuickMatchingService {
    void enterMatchQueue(String matchFromMember, String matchToMember);
    void connectSocket(WebSocketSession session);
    void enterQuickMatching(String uuid, String gameName);
    List<String> doQuickMatch(String gameName, List<String> members, List<WebSocketSession> webSocketSessions);
}
