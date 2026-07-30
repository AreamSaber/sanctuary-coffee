package com.coffee.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传文件返回值
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileVO {

    /**
     * 访问地址
     */
    private String url;

    /**
     * 相对路径
     */
    private String relativePath;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件大小
     */
    private Long size;
}
