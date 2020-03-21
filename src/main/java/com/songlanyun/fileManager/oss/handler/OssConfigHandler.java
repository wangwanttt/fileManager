
package com.songlanyun.fileManager.oss.handler;

import com.songlanyun.fileManager.dao.OssConfigRepository;
import com.songlanyun.fileManager.domain.OssConfig;
import com.songlanyun.fileManager.utils.ResponseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class OssConfigHandler {
    @Autowired
    private
    OssConfigRepository ossConfigRepository;

    public Mono<ServerResponse> save(ServerRequest request) {
        return request
                .bodyToMono(OssConfig.class)
                .flatMap(prjVo -> {
                    return ServerResponse.ok().contentType(APPLICATION_JSON).body(ResponseInfo.ok(insertPrj(prjVo), "保存成功"), ResponseInfo.class);
                });
    }


    public Mono insertPrj(OssConfig prjVo) {
        return ossConfigRepository.insert(prjVo);
    }


    public Mono<ServerResponse> list(ServerRequest request) {
        Mono<List<OssConfig>> m = ossConfigRepository.findAll().collectList();

        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(ResponseInfo.ok(m), OssConfig.class);
    }


    public Mono<ServerResponse> update(ServerRequest request) {
        return request.bodyToMono(OssConfig.class).flatMap(OssConfig -> {
            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(ResponseInfo.ok(ossConfigRepository.save(OssConfig), "更新成功"), OssConfig.class);
        });

    }

    public Mono<ServerResponse> getOssConfigById(ServerRequest request) {
        String forexId = request.pathVariable("id");
        Mono<ServerResponse> notFound = ServerResponse.notFound().build();
        Mono<OssConfig> forex = ossConfigRepository.findById(forexId);

        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(forex, OssConfig.class)
                .switchIfEmpty(notFound);
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        String id = request.pathVariable("id");
        id = id.replaceAll("\"", "");
        Mono<Void> delId = ossConfigRepository.deleteById(id);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(ResponseInfo.ok(delId), OssConfig.class);

    }
}
