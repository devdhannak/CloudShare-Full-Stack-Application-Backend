package com.cloud.service;

import org.springframework.web.multipart.MultipartFile;

public interface AwsService {
    String uploadFile(MultipartFile file);
}
