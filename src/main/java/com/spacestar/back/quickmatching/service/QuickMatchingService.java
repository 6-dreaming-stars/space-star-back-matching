package com.spacestar.back.quickmatching.service;

import org.springframework.web.socket.WebSocketSession;

public interface QuickMatchingService {
    void connectSocket(WebSocketSession session);
    void enterQuickMatching(String uuid, String gameName);
}
