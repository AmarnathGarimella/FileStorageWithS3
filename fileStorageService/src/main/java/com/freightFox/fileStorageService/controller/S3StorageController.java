package com.freightFox.fileStorageService.controller;

import com.freightFox.fileStorageService.service.S3Service;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/files/v1")
public class S3StorageController {
    Logger LOG = LoggerFactory.getLogger((S3StorageController.class));

    @Autowired
    private S3Service s3Service;

    @GetMapping("/search")
    public List<String> searchFiles(@RequestParam String userName, @RequestParam String searchTerm){

        List<String> searchRes = s3Service.searchFiles(userName,searchTerm);
        LOG.info("List of files found is sent");
        return searchRes.isEmpty()?List.of("No Objects found"):searchRes;
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam String userName, @RequestParam("file") String fileName) {
        try {
            InputStream inputStream = s3Service.downloadFile(userName, fileName);

            // Return the file as a downloadable response
            LOG.info("File is being sent");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(new InputStreamResource(inputStream));

        } catch (NoSuchKeyException e) {
            // Handle the case when the file is not found in S3
            LOG.error("",e);
            return ResponseEntity.status(HttpStatus.SC_NOT_FOUND).body("File not found: " + fileName);
        } catch (Exception e) {
            // Handle any other exceptions
            LOG.error("",e);
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body("Error downloading file: " + e.getMessage());
        }
    }


    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam String userName, @RequestParam("file")MultipartFile file){
        try{
            if (file.isEmpty()){
                LOG.error("{}","File cannot be empty");
                return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body("File cannot be empty");
            }
            Path tempFile = Files.createTempFile("upload-",file.getOriginalFilename());
            file.transferTo(tempFile);

            s3Service.uploadFile(userName, file.getOriginalFilename(), tempFile);

            LOG.info("File uploaded successfully!");
            return ResponseEntity.ok("File uploaded successfully!");
        }
        catch (Exception e){
            LOG.error("{}",e.getMessage());
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
