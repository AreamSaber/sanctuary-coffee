package com.coffee.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件上传配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    /**
     * 上传根目录
     */
    private String path = "/uploads";

    /**
     * 单文件最大字节数
     */
    private long maxSize = 10 * 1024 * 1024L;
}
