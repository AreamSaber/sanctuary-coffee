package com.coffee.service;

import com.coffee.vo.UploadFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {

    UploadFileVO uploadImage(MultipartFile file, String scene);

    List<UploadFileVO> uploadImages(MultipartFile[] files, String scene);
}
