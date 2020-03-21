package com.songlanyun.fileManager.oss.handler;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import com.aliyun.oss.model.PutObjectResult;
import com.songlanyun.fileManager.dao.OssConfigRepository;
import com.songlanyun.fileManager.domain.OssConfig;
import com.songlanyun.fileManager.error.GlobalException;
import com.songlanyun.fileManager.utils.ResponseInfo;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class OssHandler {


    private OSS oss;
    @Autowired
    private
    OssConfigRepository ossConfigRepository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    /**
     * 文件上传
     *
     * @param request
     * @return
     */
    public Mono<ServerResponse> upload(ServerRequest request) {
        Mono<MultiValueMap<String, Part>> multiValueMapMono = request.body(BodyExtractors.toMultipartData());
        return multiValueMapMono.flatMap(multiValueMap -> {
            FormFieldPart idPart = (FormFieldPart) multiValueMap.getFirst("id");
            String prjId = idPart.value();
            //得到oss项目配置
            Mono<OssConfig> ossConfig = ossConfigRepository.findById(prjId);
            return ossConfig.flatMap(ossVo -> {
                List<Part> editor = multiValueMap.get("file");
                FilePart filePart = (FilePart) editor.get(0);
                FormFieldPart formFieldPart = (FormFieldPart) multiValueMap.getFirst("path");
                String prefix = formFieldPart.value();
                if (prefix.startsWith("/") || prefix.startsWith("\\")) {
                                 prefix = prefix.substring(1);
                }
                try {
                    oss = new OSSClientBuilder().build(ossVo.getOss_endpoint(),
                            ossVo.getOss_access_key_id(),
                            ossVo.getOss_access_key_secret());

                    Path filePath = Files.createTempFile("oss", filePart.filename());
                    File file = filePath.toFile();
                    filePart.transferTo(file);
                    InputStream inputStream = new FileInputStream(file);
                    String fileName = prefix + UUID.randomUUID() + filePart.filename();
                    PutObjectResult putObjectResult = oss.putObject(ossVo.getOss_bucket_name(),
                            fileName, inputStream);
                    inputStream.close();
                    oss.shutdown();
                    return ServerResponse.ok().body(ResponseInfo.ok(Mono.just(ossVo.getPath_prefix() + fileName)), ResponseInfo.class);
                } catch (Exception e) {
                    throw new GlobalException(-200, "文件上传失败3：" + e.getMessage());
                    // 第二种选择： return ServerResponse.ok().body(ResponseInfo.info(-300, "文件上传失败1"), ResponseInfo.class);
                }

            }).switchIfEmpty(
                    ServerResponse.ok().contentType(APPLICATION_JSON).body(
                            ResponseInfo.info(-100, "无上传项目OSS配置信息,上传失败"), ResponseInfo.class)
            );
//    抛了异常就不用它        .onErrorResume(e -> {  //onErrorReturn --- .onErrorReturn("onErrorReturn .... www.xttblog.com")
//                Object err = e;
//                return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(ResponseInfo.info(-200, "文件上传失败2:" + e.getMessage()), ResponseInfo.class);
//            });

        });
    }



    /** 得到oss 直传的签名  **/
    public Mono<ServerResponse> getOssSign(ServerRequest request)  {
        String id = request.pathVariable("id");
        id = id.replaceAll("\"", "");
        String extraParam=request.pathVariable("param");

        Mono<OssConfig> ossConfig = ossConfigRepository.findById(id);
        return ossConfig.flatMap(ossVo -> {
            oss = new OSSClientBuilder().build(ossVo.getOss_endpoint(),
                    ossVo.getOss_access_key_id(),
                    ossVo.getOss_access_key_secret());

            String accessId = ossVo.getOss_access_key_id(); // 请填写您的AccessKeyId。
            String accessKey =ossVo.getOss_access_key_secret(); // 请填写您的AccessKeySecret。
            String endpoint =ossVo.getOss_endpoint(); // 请填写您的 endpoint。
            String bucket = ossVo.getOss_bucket_name(); // 请填写您的 bucketname 。
            String host = "https://" + bucket + "." + endpoint; // host的格式为 bucketname.endpoint
            // callbackUrl为 上传回调服务器的URL，请将下面的IP和Port配置为您自己的真实信息。
            String callbackUrl = ossVo.getCallFunc();
             String dir = "user-dir-prefix/"; // 用户上传文件时指定的前缀。
            oss = new OSSClientBuilder().build(ossVo.getOss_endpoint(),
                    ossVo.getOss_access_key_id(),
                    ossVo.getOss_access_key_secret());

            try {
                long expireTime = 3600;
                long expireEndTime = System.currentTimeMillis() + expireTime * 1000; //10分钟过期
                Date expiration = new Date(expireEndTime);
                PolicyConditions policyConds = new PolicyConditions();
                policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000);
            //  policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);

                String postPolicy = oss.generatePostPolicy(expiration, policyConds);
                byte[] binaryData = postPolicy.getBytes("utf-8");
                String encodedPolicy = BinaryUtil.toBase64String(binaryData);
                String postSignature = oss.calculatePostSignature(postPolicy);

                Map<String, String> respMap = new LinkedHashMap<String, String>();
                respMap.put("accessid", accessId);
                respMap.put("policy", encodedPolicy);
                respMap.put("signature", postSignature);
                respMap.put("dir", dir);
                respMap.put("host", host);
                respMap.put("expire", String.valueOf(expireEndTime / 1000));
                // respMap.put("expire", formatISO8601Date(expiration));

                JSONObject jasonCallback = new JSONObject();
                jasonCallback.put("callbackUrl", callbackUrl);
                //设置扩展参数
                if (StringUtils.isEmpty(extraParam)){
                    jasonCallback.put("callback-var",extraParam);
                }

                jasonCallback.put("callbackBody",  //----oss服务器回调时传递的参数
                        "filename=${object}&size=${size}&mimeType=${mimeType}&height=${imageInfo.height}&width=${imageInfo.width}");
                jasonCallback.put("callbackBodyType", "application/x-www-form-urlencoded");
                String base64CallbackBody = BinaryUtil.toBase64String(jasonCallback.toString().getBytes());
                respMap.put("callback", base64CallbackBody);

                JSONObject ja1 = JSONObject.fromObject(respMap);
                // System.out.println(ja1.toString());

              return  ServerResponse.ok().contentType(APPLICATION_JSON)
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods",  "GET, POST")
                        .header("Access-Control-Allow-Origin", "*")
                        .body(
                        ResponseInfo.ok(Mono.just(ja1.toString())), ResponseInfo.class);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw  new GlobalException(-200,"上传签名JSON化失败");
            } finally {
            // 关闭client
            oss.shutdown();
        }
        }).switchIfEmpty(
                ServerResponse.ok().contentType(APPLICATION_JSON).body(
                        ResponseInfo.info(-100, "无此id的OSS参数设置"), ResponseInfo.class)
        );
    }


    /**
     * 文件上传 gridfs
     *
     * @param request
     * @return
     */
    public Mono<ServerResponse> gridfsUpload(ServerRequest request) {
        Mono<MultiValueMap<String, Part>> multiValueMapMono = request.body(BodyExtractors.toMultipartData());
        return multiValueMapMono.flatMap(multiValueMap -> {
            FilePart filePart = (FilePart) multiValueMap.getFirst("file");
            FormFieldPart path = (FormFieldPart) multiValueMap.getFirst("path");

            String fileName = path.value() + filePart.filename();

            File file = new File(filePart.filename());
            filePart.transferTo(file);
            try (InputStream inputStream = new FileInputStream(file)) {
                ObjectId objectId = gridFsTemplate.store(inputStream, fileName);
                return ServerResponse.ok().body(ResponseInfo.ok(Mono.just(objectId.toString())), ResponseInfo.class);
                //  return ServerResponse.ok().body(Mono.just(Result.success("上传文件成功", objectId.toString())), Result.class);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ServerResponse.ok().body(ResponseInfo.info(-100, "上传失败"), ResponseInfo.class);
        });
    }
}
