package com.vowser.backend.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 백엔드, MCP 서버, 그리고 연결된 클라이언트 간의 통신을 관리하고
 * WebSocket 세션을 관리
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ControlService {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 새로운 WebSocket 세션을 등록
     *
     * @param session 등록할 WebSocket 세션
     */
    public void registerSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("웹소켓 세션 등록: sessionId=[{}], totalSessions=[{}]", 
                session.getId(), sessions.size());
    }

    /**
     * WebSocket 세션을 등록 해제
     *
     * @param session 제거할 WebSocket 세션
     */
    public void unregisterSession(WebSocketSession session) {
        sessions.remove(session.getId());
        log.info("웹소켓 세션 제거: sessionId=[{}], remainingSessions=[{}]", 
                session.getId(), sessions.size());
    }

    /**
     * WebSocket을 통해 연결된 클라이언트로 명령을 전송
     *
     * @param command 전송할 명령 데이터
     */
    public void sendCommandToClient(Map<String, Object> command) {
        WebSocketSession session = getLastConnectedSession();
        
        if (session == null || !session.isOpen()) {
            log.warn("명령 전송 실패: 연결된 클라이언트 세션이 없거나 닫혀있습니다. command=[{}]", command);
            return;
        }

        try {
            String jsonCommand = objectMapper.writeValueAsString(command);
            session.sendMessage(new TextMessage(jsonCommand));
            
            log.info("클라이언트로 명령 전송 완료: sessionId=[{}], command=[{}]", 
                    session.getId(), jsonCommand);
                    
        } catch (JsonProcessingException e) {
            log.error("명령 직렬화 실패: command=[{}]", command, e);
        } catch (IOException e) {
            log.error("명령 전송 중 IO 오류 발생: sessionId=[{}]", session.getId(), e);
        } catch (Exception e) {
            log.error("명령 전송 중 예상치 못한 오류 발생: sessionId=[{}]", session.getId(), e);
        }
    }

    /**
     * MCP 서버 응답을 연결된 클라이언트로 중계
     *
     * @param messageJson MCP 서버로부터 수신한 JSON 메시지
     */
    public void relayMcpResponse(String messageJson) {
        WebSocketSession session = getLastConnectedSession();

        if (session == null || !session.isOpen()) {
            log.warn("MCP 응답 중계 실패: 연결된 클라이언트 세션이 없거나 닫혀있습니다.");
            return;
        }

        try {
            session.sendMessage(new TextMessage(messageJson));
            log.info("MCP 서버 응답을 클라이언트로 전송 완료: messageLength=[{}]", messageJson.length());

        } catch (IOException e) {
            log.error("MCP 응답 중계 중 IO 오류 발생", e);
        } catch (Exception e) {
            log.error("MCP 응답 중계 중 예상치 못한 오류 발생", e);
        }
    }

    /**
     * 가장 최근에 연결된 세션을 가져옴
     *
     * @return 마지막으로 연결된 WebSocket 세션 (없으면 null)
     */
    private WebSocketSession getLastConnectedSession() {
        return sessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    /**
     * 모니터링용 현재 활성 세션 수를 반환
     *
     * @return 활성 세션 개수
     */
    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }
}