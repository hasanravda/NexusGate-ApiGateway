package com.nexusgate.gateway.util;

import org.springframework.http.server.reactive.ServerHttpRequest;

public class HeaderUtil {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    public static String extractApiKey(ServerHttpRequest request) {
        return request.getHeaders().getFirst(API_KEY_HEADER);
    }

    public static String extractJwt(ServerHttpRequest request) {
        return request.getHeaders().getFirst(AUTHORIZATION_HEADER);
    }

    public static String getClientIp(ServerHttpRequest request) {
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }

        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }
}