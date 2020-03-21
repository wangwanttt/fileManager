package com.songlanyun.fileManager.config;

import com.songlanyun.fileManager.oss.handler.OssConfigHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class OssConfigRouter {
    static final String API_BASE_URL = "/api/v1/oss/";
    @Bean
    public RouterFunction<ServerResponse> routePrj(OssConfigHandler projectHandler) {
        return RouterFunctions
                .route(RequestPredicates.GET(API_BASE_URL+"list")
                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        projectHandler::list)
                .andRoute(RequestPredicates.DELETE(API_BASE_URL+"delete/{id}")
                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        projectHandler::delete)
                .andRoute(RequestPredicates.POST(API_BASE_URL+"update")
                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        projectHandler::update)
                .andRoute(RequestPredicates.POST(API_BASE_URL+"save")
                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        projectHandler::save);
    }
}

