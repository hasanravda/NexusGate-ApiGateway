package com.nexusgate.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GatewayConfig {

    @Value("${config.service.url}")
    private String configServiceUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)); // 10MB
    }

    @Bean
    public WebClient configServiceWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(configServiceUrl)
                .defaultStatusHandler(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Config service error: " + body))
                )
                .build();
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        // Minimal route configuration - actual routing is handled by GlobalFilters
        // Using httpbin as a dummy URI (will be overridden by ServiceRoutingFilter)
        return builder.routes()
                .route("api_routes", r -> r
                        .path("/**")
                        .uri("http://httpbin.org"))
                .build();
    }
}
