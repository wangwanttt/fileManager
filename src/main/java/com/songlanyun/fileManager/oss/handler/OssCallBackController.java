package com.songlanyun.fileManager.oss.handler;

import com.aliyun.oss.common.utils.BinaryUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collections;
import java.util.Map;

//import org.springframework.web.reactive.function.server.ServerResponse;
//import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;
//import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@RestController
@RequestMapping(value = "/oss")
@Slf4j
public class OssCallBackController {

   /**
    * OSS直传回调
    **/
   @PostMapping("/ossCallBack")
   public Mono<JSONObject> ossCallBack(ServerWebExchange exchange) {
       ServerHttpRequest serverHttpRequest = exchange.getRequest();
       Flux<DataBuffer> body = serverHttpRequest.getBody();
       ServerHttpResponse response = exchange.getResponse();
       body.flatMap(param->{
           return null;
       });
     //  response.setStatusCode()
    //   response.setStatus(HttpServletResponse.SC_OK);
//             return result.success("success");
       return null;
   }
//
//     @RequestMapping("/ossCallBack")
//     public JSONObject callBack(@RequestBody String ossCallbackBody, @RequestHeader("Authorization") String authorization,
//                                @RequestHeader("x-oss-pub-key-url") String publicKeyUrlBase64, HttpServletRequest request,
//                                HttpServletResponse response) {
//         boolean isCallBack = verifyOSSCallbackRequest(authorization, publicKeyUrlBase64, ossCallbackBody, request.getQueryString(), request.getRequestURI());
//         if (isCallBack) {
//             response.setStatus(HttpServletResponse.SC_OK);
//             return result.success("success");
//         } else {
//             response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//             return JSONResult.error("回调验证失败！");
//         }
//     }
    /**
     * @Description OSS回调请求验证
     * @Author hzl
     * @Date 2018/11/27
     * @Param [authorizationInput, pubKeyInput, ossCallbackBody, queryString, uri]
     * @Return boolean
     */
    public boolean verifyOSSCallbackRequest(String authorizationInput, String pubKeyInput, String ossCallbackBody, String queryString, String uri){
        boolean ret = false;
        try {
            //将base64编码的数据进行还原
            byte[] authorization = BinaryUtil.fromBase64String(authorizationInput);
            byte[] pubKey = BinaryUtil.fromBase64String(pubKeyInput);
            String pubKeyAddr = new String(pubKey);
            if (!pubKeyAddr.startsWith("http://gosspublic.alicdn.com/") && !pubKeyAddr.startsWith("https://gosspublic.alicdn.com/")) {
                log.error("pub key addr must be oss address");
                return false;
            }
            //获取请求中的公钥信息
            String retString = executeGet(pubKeyAddr);
            retString = retString.replace("-----BEGIN PUBLIC KEY-----", "");
            retString = retString.replace("-----END PUBLIC KEY-----", "");
            String decodeUri = URLDecoder.decode(uri, "utf-8");
            if (queryString != null && !"".equals(queryString)) {
                decodeUri += "?" + queryString;
            }
            decodeUri += "\n" + ossCallbackBody;
            ret = doCheck(decodeUri, authorization, retString);
        } catch (Exception e) {
            ret = false;
            log.error("验证OSS请求出现异常：" + e);
        }
        return ret;
    }

    /**
     * @Description 获取请求中的参数
     * @Author hzl
     * @Date 2018/11/27
     * @Param [pubKeyUrl]
     * @Return java.lang.String
     */
    @SuppressWarnings({"finally"})
    private String executeGet(String pubKeyUrl) throws Exception {
        BufferedReader in = null;
        String content = null;
        try {
            // 定义HttpClient
            @SuppressWarnings("resource")
            DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
            // 实例化HTTP方法
            HttpGet request = new HttpGet();
            request.setURI(new URI(pubKeyUrl));
            HttpResponse response = defaultHttpClient.execute(request);
            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            in.close();
            content = sb.toString();
            return content;
        } catch (Exception e) {
            log.error("解析公钥参数失败：" + e);
            throw new Exception("解析公钥参数失败!");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error("关闭BufferedReader出现异常：" + e);
                }
            }
        }
    }

    /**
     * @Description 对请求参数进行规则校验
     * @Author hzl
     * @Date 2018/11/27
     * @Param [content, sign, publicKey]
     * @Return boolean
     */
    private boolean doCheck(String content, byte[] sign, String publicKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = BinaryUtil.fromBase64String(publicKey);
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
            java.security.Signature signature = java.security.Signature.getInstance("MD5withRSA");
            signature.initVerify(pubKey);
            signature.update(content.getBytes());
            boolean bverify = signature.verify(sign);
            return bverify;
        } catch (Exception e) {
            log.error("校验出现异常：" + e);
        }
        return false;
    }


}
