package com.songlanyun.fileManager.config;

import com.songlanyun.fileManager.oss.handler.OssHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class OssRouters {
    static final String API_BASE_URL = "/api/v1/oss/";

    @Bean
    public RouterFunction<ServerResponse> osseRouter(OssHandler ossHandler) {
        return RouterFunctions
                .route(RequestPredicates.POST(API_BASE_URL + "upload/aliyun")
                                .and(RequestPredicates.accept(MediaType.MULTIPART_FORM_DATA)),
                        ossHandler::upload)
                .andRoute(RequestPredicates.POST(API_BASE_URL + "upload/gridfs")
                                .and(RequestPredicates.accept(MediaType.MULTIPART_FORM_DATA)),
                        ossHandler::gridfsUpload)
                .andRoute(RequestPredicates.GET(API_BASE_URL + "getOssSign/{id}/{param}"),
                        ossHandler::getOssSign);


    }

}

