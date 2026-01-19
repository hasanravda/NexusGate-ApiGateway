package com.nexusgate.gateway.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusgate.gateway.dto.ServiceRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRoutingFilter implements GlobalFilter, Ordered {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServiceRouteResponse route = exchange.getAttribute("serviceRoute");
        if (route == null) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        Long apiKeyId = exchange.getAttribute("apiKeyId");

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(request.getHeaders());

        if (route.getCustomHeaders() != null && !route.getCustomHeaders().isEmpty()) {
            try {
                Map<String, String> customHeaders = objectMapper.readValue(
                        route.getCustomHeaders(),
                        new TypeReference<Map<String, String>>() {}
                );
                customHeaders.forEach(headers::add);
            } catch (Exception e) {
                log.error("Failed to parse custom headers", e);
            }
        }

        if (apiKeyId != null) {
            headers.add("X-NexusGate-ApiKey-Id", String.valueOf(apiKeyId));
        }
        headers.add("X-NexusGate-ServiceRoute-Id", String.valueOf(route.getId()));

        int timeoutMs = route.getTimeoutMs() != null ? route.getTimeoutMs() : 30000;
        WebClient client = webClientBuilder
                .baseUrl(route.getTargetUrl())
                .defaultHeaders(h -> h.addAll(headers))
                .build();

        return forwardRequest(client, request, timeoutMs, exchange);
    }

    private Mono<Void> forwardRequest(WebClient client, ServerHttpRequest request,
                                      int timeoutMs, ServerWebExchange exchange) {
        HttpMethod method = request.getMethod();
        String path = request.getPath().value();
        String query = request.getURI().getRawQuery();
        String uri = query != null ? path + "?" + query : path;

        return client.method(method)
                .uri(uri)
                .body((outputMessage, context) -> 
                    outputMessage.writeWith(exchange.getRequest().getBody()))
                .exchangeToMono(clientResponse -> {
                    exchange.getResponse().setStatusCode(clientResponse.statusCode());
                    exchange.getResponse().getHeaders().addAll(clientResponse.headers().asHttpHeaders());
                    return exchange.getResponse()
                            .writeWith(clientResponse.bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class));
                })
                .timeout(Duration.ofMillis(timeoutMs))
                .onErrorResume(e -> {
                    log.error("Error forwarding request", e);
                    exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
                    return exchange.getResponse().setComplete();
                });
    }

    @Override
    public int getOrder() {
        return -70;
    }
}