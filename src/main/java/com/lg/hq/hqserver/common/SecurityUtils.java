package com.lg.hq.hqserver.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security 공통 유틸리티
 * 현재 로그인한 사용자 정보를 어디서든 꺼낼 수 있음
 */
@Component
public class SecurityUtils {

    /**
     * 실제 클라이언트 IP 추출
     * Nginx, AWS ELB 등 프록시 뒤에 있어도 올바른 IP 반환
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";

        // 프록시 헤더 순서대로 확인
        String[] headers = {
                "X-Forwarded-For",      // Nginx, AWS ELB
                "X-Real-IP",            // Nginx
                "Proxy-Client-IP",      // Apache
                "WL-Proxy-Client-IP",   // WebLogic
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For는 여러 IP가 콤마로 구분됨
                // "192.168.0.10, 10.0.0.1" → 첫 번째가 실제 클라이언트
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    // ── 현재 사용자 이름 ──────────────────────────────────
    public static String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return "anonymous";
            return auth.getName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ── 현재 사용자 권한 목록 ─────────────────────────────
    public static Collection<? extends GrantedAuthority> getCurrentRoles() {
        try {
            Authentication auth = SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth == null) return Collections.emptyList();
            return auth.getAuthorities();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ── 특정 권한 보유 여부 ───────────────────────────────
    public static boolean hasRole(String role) {
        return getCurrentRoles().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    // ── 관리자 여부 ───────────────────────────────────────
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
}