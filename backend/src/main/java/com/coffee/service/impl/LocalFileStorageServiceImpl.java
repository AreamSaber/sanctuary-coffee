package com.coffee.service.impl;

import com.coffee.common.exception.BusinessException;
import com.coffee.config.FileUploadProperties;
import com.coffee.service.FileStorageService;
import com.coffee.vo.UploadFileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 本地文件存储服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );
    private static final Pattern SCENE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final FileUploadProperties fileUploadProperties;

    @Override
    public UploadFileVO uploadImage(MultipartFile file, String scene) {
        validateImage(file);

        String normalizedScene = normalizeScene(scene);
        String extension = resolveExtension(file.getOriginalFilename());
        String dateFolder = LocalDate.now().format(DATE_FORMATTER);
        String storedFileName = UUID.randomUUID().toString().replace("-", "") + extension;

        Path uploadRoot = getUploadRoot();
        Path targetDirectory = uploadRoot.resolve(normalizedScene).resolve(dateFolder);
        Path targetFile = targetDirectory.resolve(storedFileName);

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("保存上传图片失败: {}", file.getOriginalFilename(), e);
            throw new BusinessException("图片上传失败");
        }

        String relativePath = normalizedScene + "/" + dateFolder + "/" + storedFileName;
        String accessUrl = "/api/uploads/" + encodePath(relativePath);

        return new UploadFileVO(accessUrl, relativePath, file.getOriginalFilename(), file.getSize());
    }

    @Override
    public List<UploadFileVO> uploadImages(MultipartFile[] files, String scene) {
        if (files == null || files.length == 0) {
            throw new BusinessException("上传文件不能为空");
        }

        return Arrays.stream(files)
            .map(file -> uploadImage(file, scene))
            .toList();
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        if (file.getSize() > fileUploadProperties.getMaxSize()) {
            throw new BusinessException("图片大小不能超过10MB");
        }

        String extension = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 jpg、jpeg、png、gif、webp、bmp 格式图片");
        }
    }

    private String normalizeScene(String scene) {
        if (scene == null || scene.isBlank()) {
            return "common";
        }

        String normalized = scene.trim().toLowerCase();
        if (!SCENE_PATTERN.matcher(normalized).matches()) {
            return "common";
        }
        return normalized;
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank() || !originalFilename.contains(".")) {
            throw new BusinessException("图片文件名无效");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (extension.length() > 10) {
            throw new BusinessException("图片文件名无效");
        }
        return extension;
    }

    private Path getUploadRoot() {
        return Paths.get(fileUploadProperties.getPath()).toAbsolutePath().normalize();
    }

    private String encodePath(String relativePath) {
        return Arrays.stream(relativePath.split("/"))
            .map(part -> UriUtils.encodePathSegment(part, StandardCharsets.UTF_8))
            .reduce((left, right) -> left + "/" + right)
            .orElse(relativePath);
    }
}
