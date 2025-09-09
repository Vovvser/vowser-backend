package com.vowser.backend.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 네트워크 관련 유틸리티
 */
@Component
public class NetworkUtil {

    /**
     * 클라이언트 IP 주소 추출
     * 프록시 서버를 거친 경우에도 실제 클라이언트 IP를 추출
     */
    public String getClientIp(HttpServletRequest request) {
        
        // X-Forwarded-For 헤더: 프록시 체인을 통과할때 원본 클라이언트 IP와 중간 프록시들의 IP를 쉼표로 구분하여 저장
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        // X-Real-IP 헤더: 일부 프록시 서버(Nginx)에서 사용
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // 직접 연결된 클라이언트의 IP 주소
        return request.getRemoteAddr();
    }
}