package com.freightFox.fileStorageService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3Service {

    Logger LOG = LoggerFactory.getLogger(S3Service.class);

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Autowired
    private S3Client s3Client;
    public List<String> searchFiles(String userName, String searchTerm){
        String prefix = userName + "/";

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        return response.contents().stream()
                .map(S3Object::key)
                .filter(key->key.split("/")[1].contains(searchTerm))
                .collect(Collectors.toList());
    }

    public InputStream downloadFile(String userName, String fileName) {
        String fileKey = userName + "/" + fileName;

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        ResponseInputStream<?> s3Object = s3Client.getObject(request);
        return s3Object;
    }

    public void uploadFile(String userName, String fileName, Path filePath) throws Exception {
        String key = userName + "/" + fileName;  // This creates a folder-like structure

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(filePath));

    }
}
