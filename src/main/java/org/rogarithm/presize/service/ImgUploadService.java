package org.rogarithm.presize.service;

import io.awspring.cloud.s3.S3Exception;
import org.rogarithm.presize.config.ErrorCode;
import org.rogarithm.presize.exception.S3FileUploadFailException;
import org.rogarithm.presize.web.request.ImgSquareRequest;
import org.rogarithm.presize.web.request.ImgUncropRequest;
import org.rogarithm.presize.web.request.ImgUpscaleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Service
public class ImgUploadService {

    private static final Logger log = LoggerFactory.getLogger(ImgUploadService.class);

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${cloud.aws.region}")
    private String region;

    private final S3Client s3Client;

    public ImgUploadService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public CompletableFuture<Void> uploadUpscaleImgToS3(ImgUpscaleRequest request,
                                                        String polishedImg
    ) {
        String fileName = makeFullFileName(request.getTaskId());
        return processUpload(fileName, polishedImg);
    }

    public CompletableFuture<Void> uploadUncropImgToS3(ImgUncropRequest request,
                                                       String polishedImg
    ) {
        String fileName = makeFullFileName(request.getTaskId());
        return processUpload(fileName, polishedImg);
    }

    public CompletableFuture<Void> uploadSquareImgToS3(ImgSquareRequest request,
                                                       String polishedImg
    ) {
        String fileName = makeFullFileName(request.getTaskId());
        return processUpload(fileName, polishedImg);
    }

    private String makeFullFileName(String taskId) {
        String fileExtension = ".png";
        return taskId + fileExtension;
    }

    private CompletableFuture<Void> processUpload(String fileName, String polishedImg) {
        String directoryName = "img/";
        byte[] decodedBytes = Base64.getDecoder().decode(polishedImg);

        try (InputStream inputStream = new ByteArrayInputStream(decodedBytes)) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(directoryName + fileName)
                            .contentType("image/png")
                            .contentLength(Long.valueOf(decodedBytes.length))
                            .build(),
                    RequestBody.fromInputStream(inputStream, decodedBytes.length)
            );

            return new CompletableFuture<>();
        } catch (IOException | S3Exception e) {
            log.error(e.getMessage());
            throw new S3FileUploadFailException(ErrorCode.SERVER_FAULT); // "Error occurred during file upload: " + e.getMessage()
        }
    }

    public String makeUrl(String taskId) {
        String fileExtension = ".png";
        String fileName = taskId + fileExtension;
        String directoryName = "img";
        return "https://" + bucket + ".s3." + region + ".amazonaws.com" + "/" + directoryName + "/" + fileName;
    }
}
