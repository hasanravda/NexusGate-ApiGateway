package com.nexusgate.gateway.filter;

import com.nexusgate.gateway.dto.ServiceRouteResponse;
import com.nexusgate.gateway.util.ErrorResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Filter to validate that the incoming HTTP method is allowed for the matched route.
 * Runs at order -95, after route resolution but before authentication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MethodValidationFilter implements GlobalFilter, Ordered {

    private final ErrorResponseUtil errorResponseUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServiceRouteResponse route = exchange.getAttribute("serviceRoute");

        // If no route found or allowedMethods not configured, skip validation
        if (route == null || route.getAllowedMethods() == null || route.getAllowedMethods().isEmpty()) {
            return chain.filter(exchange);
        }

        HttpMethod requestMethod = exchange.getRequest().getMethod();
        if (requestMethod == null) {
            log.warn("Request method is null for path: {}", exchange.getRequest().getPath());
            return errorResponseUtil.writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, "Invalid HTTP method");
        }

        String methodName = requestMethod.name();
        List<String> allowedMethods = route.getAllowedMethods();

        // Debug: Log the exact comparison
        log.debug("Validating method '{}' against allowed methods: {}", methodName, allowedMethods);
        if (allowedMethods != null) {
            allowedMethods.forEach(method -> 
                log.debug("  Comparing '{}' with '{}' (equals: {}, equalsIgnoreCase: {})", 
                    methodName, method, methodName.equals(method), methodName.equalsIgnoreCase(method))
            );
        }

        // Check if the request method is in the allowed list (case-insensitive)
        boolean isAllowed = allowedMethods != null && allowedMethods.stream()
                .anyMatch(allowed -> allowed != null && allowed.equalsIgnoreCase(methodName));

        if (!isAllowed) {
            log.warn("Method {} not allowed for route {} (path: {}). Allowed methods: {} (null check: {}, stream check: {})", 
                    methodName, route.getId(), route.getPublicPath(), allowedMethods,
                    allowedMethods == null, allowedMethods != null ? allowedMethods.stream().anyMatch(m -> m != null && m.equalsIgnoreCase(methodName)) : "list was null");
            return errorResponseUtil.writeErrorResponse(
                    exchange, 
                    HttpStatus.METHOD_NOT_ALLOWED, 
                    String.format("Method %s is not allowed. Allowed methods: %s", methodName, String.join(", ", allowedMethods))
            );
        }

        log.debug("Method {} is allowed for route {} (path: {})", methodName, route.getId(), route.getPublicPath());
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Run after GlobalRequestFilter (-100) but before AuthenticationFilter (-90)
        return -95;
    }
}
