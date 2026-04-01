package com.cloud.controller;

import com.cloud.service.AwsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AwsController {

    private final AwsService awsService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFileToS3(
            @RequestPart("file") MultipartFile file) {
        String url = awsService.uploadFile(file);
        return ResponseEntity.ok().body(Map.of("imageUrl", url));
    }
}
