package com.ns.gateway.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.RequestPath;
import org.springframework.stereotype.Component;

import com.ns.gateway.LogState;

import reactor.core.publisher.Mono;

@Slf4j
@Component("Logging")
public class LoggingGatewayFilterFactory extends AbstractGatewayFilterFactory<LoggingGatewayFilterFactory.Config> implements Ordered {


    public LoggingGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        LogState curLogLevel = config.getLogLevel() != null ? config.getLogLevel() : LogState.INFO;
        String curMessage = config.getCustomMessage() != null ? config.getCustomMessage() : "default";

        return (exchange, chain) -> {
            RequestPath path = exchange.getRequest().getPath();
            HttpMethod method = exchange.getRequest().getMethod();
            log.info("[{}] Request Path: {} Method: {}. Message: {}", curLogLevel, path, method, curMessage);

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
                log.info("[{}] Response Status: {}. Message: {}", curLogLevel, statusCode, curMessage);
            }));
        };
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Getter
    @Setter
    public static class Config {
        private LogState logLevel; // INFO, WARN, ERROR
        private String customMessage; // 원하는 메시지
    }
}