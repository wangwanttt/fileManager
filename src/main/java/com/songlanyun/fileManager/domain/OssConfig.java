package com.songlanyun.fileManager.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

/**
 * 城市实体类
 *
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OssConfig {
    /**
     * 编号private
     */
    @Id
    private String id;
    private  String prjId;
    private String oss_endpoint="oss-cn-beijing.aliyuncs.com";
    private String oss_access_key_id="LTAIoZGQDu9vTi5j";
    private String  oss_access_key_secret="48xaPufwan7t3o37USEwBMKJfGgmeq";
    private String oss_bucket_name="jushi-oss";
    private String path_prefix="https://jushi-oss.oss-cn-beijing.aliyuncs.com/";
    private int maxSize=0;
    private String callFunc="";

}
