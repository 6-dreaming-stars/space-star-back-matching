package com.spacestar.back.quickmatching.service;

import com.spacestar.back.quickmatching.QuickMatchStatus;
import com.spacestar.back.quickmatching.domain.QuickMatching;
import com.spacestar.back.quickmatching.dto.WebSocketInfo;
import com.spacestar.back.quickmatching.repository.QuickMatchingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuickMatchingServiceImpl implements QuickMatchingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final Map<String, WebSocketInfo> socketInfos = new ConcurrentHashMap<>();  // 소켓 연결 정보
    private final QuickMatchingRepository quickMatchingRepository;

    @Override
    public void enterQuickMatching(String uuid, String gameName) {
        redisTemplate.opsForZSet().add(gameName, uuid, System.currentTimeMillis());
    }

    @Override
    public List<String> doQuickMatch(String gameName, List<String> members, List<WebSocketSession> sessions) {
        System.out.println("gameName = " + gameName);
        int maxScore = 0;
        String matchFromMember = null;
        String matchToMember = null;
        List<String> matchedMembers = new ArrayList<>();
        if (members.size() >= 2) {
            for (int i = 0; i < members.size(); i++) {
                System.out.println("members 멤버 " + i + "번 " + members.get(i));
                for (int j = i + 1; j < members.size(); j++) {
                    int score = calculateScore(members.get(i), members.get(j));
                    if (score > maxScore) {
                        maxScore = score;
                        matchFromMember = members.get(j);
                        matchToMember = members.get(i);
                    }
                }
                if (maxScore >= 50) {
                    enterMatchQueue(matchFromMember, matchToMember);
                    matchedMembers.add(matchFromMember);
                    matchedMembers.add(matchToMember);
                    Set<String> allmembers = redisTemplate.opsForZSet().rangeByScore(gameName, 0, System.currentTimeMillis());

                    if (allmembers != null) {
                        for (String member : allmembers) {
                            if (member.equals(matchFromMember) || member.equals(matchToMember)) {
                                redisTemplate.opsForZSet().remove(gameName, member);
                            }
                        }
                    }
                    Iterator<WebSocketSession> sessionIterator = sessions.iterator();
                    while (sessionIterator.hasNext()) {
                        WebSocketSession session = sessionIterator.next();
                        String memberUuid = (String) session.getAttributes().get("memberUuid");
                        log.info("Checking session for memberUuid: {}", memberUuid);
                        if (memberUuid.equals(matchFromMember) || memberUuid.equals(matchToMember)) {
                            sessionIterator.remove();
                            log.info("Removing session for memberUuid: {}", memberUuid);
                            // 세션을 닫아야 하는 경우
//                            try {
//                                session.close();
//                                log.info("Closed WebSocket session for member {}", memberUuid);
//                            } catch (IOException e) {
//                                log.error("Failed to close WebSocket session for member {}", memberUuid, e);
//                            }
                        }
                    }
                    return matchedMembers;
                }
            }
        }
        return matchedMembers;
    }


    private int calculateScore(String matchFromMember, String matchToMember) {
        return 50;
    }

    @Override
    public void enterMatchQueue(String matchFromMember, String matchToMember) {
        QuickMatching quickMatching = QuickMatching.builder()
                .id(matchFromMember + matchToMember)
                .matchFromMember(matchFromMember)
                .matchToMember(matchToMember)
                .matchToMemberStatus(QuickMatchStatus.WAIT)
                .matchFromMemberStatus(QuickMatchStatus.WAIT)
                .build();
        quickMatchingRepository.save(quickMatching);
    }

    @Override
    public void connectSocket(WebSocketSession socketSession) {
        socketInfos.put(socketSession.getId(), new WebSocketInfo(socketSession, 0));
    }

}
