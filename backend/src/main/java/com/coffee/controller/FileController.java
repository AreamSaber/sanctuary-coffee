package com.coffee.controller;

import com.coffee.common.Result;
import com.coffee.service.FileStorageService;
import com.coffee.vo.UploadFileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传控制器
 */
@Tag(name = "文件上传")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传单张图片")
    @PostMapping("/upload/image")
    public Result<UploadFileVO> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String scene) {
        return Result.success(fileStorageService.uploadImage(file, scene));
    }

    @Operation(summary = "批量上传图片")
    @PostMapping("/upload/images")
    public Result<List<UploadFileVO>> uploadImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(required = false) String scene) {
        return Result.success(fileStorageService.uploadImages(files, scene));
    }
}
