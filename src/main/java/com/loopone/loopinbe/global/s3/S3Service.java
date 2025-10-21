package com.loopone.loopinbe.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${spring.aws.credentials.s3.bucket}")
    private String BUCKET_NAME;

    // 이미지 업로드
    public String uploadImageFile(MultipartFile file, String dirName) throws IOException {
        String fileName = dirName + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(fileName)
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return "https://" + BUCKET_NAME + ".s3.ap-northeast-2.amazonaws.com/" + fileName;
    }

    // 보고서(PDF, Excel) 파일용 업로드
    public String uploadFile(byte[] content, String fileName, String contentType, String downloadFileName) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(fileName)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(content));
        return "https://" + BUCKET_NAME + ".s3.ap-northeast-2.amazonaws.com/" + fileName;
    }

    public void deleteFile(String fileUrl) {
        String fileName = extractFileName(fileUrl);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(fileName)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    public void deleteAllFile(List<String> imageUrls) {
        for (String fileName : imageUrls) {
            deleteFile(fileName);
        }
    }

    // DB에 저장된 전체 URL에서 S3에 저장된 파일명만 추출
    private String extractFileName(String fileUrl) {
        String prefix = "https://" + BUCKET_NAME + ".s3.ap-northeast-2.amazonaws.com/";
        return fileUrl.replace(prefix, "");
    }

    public String generatePresignedUrl(String fileKey, String downloadFileName) {
        String encodedFileName = encodeRFC5987(downloadFileName);
        String contentDisposition = "attachment; filename=\"" + sanitizeAsciiFallback(downloadFileName) + "\"; filename*=UTF-8''" + encodedFileName;
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(fileKey)
                .responseContentDisposition(contentDisposition)
                .build();
        return s3Presigner.presignGetObject(b -> b
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
        ).url().toString();
    }

    private String encodeRFC5987(String fileName) {
        try {
            return URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20")
                    .replaceAll("%28", "(")
                    .replaceAll("%29", ")")
                    .replaceAll("%27", "'");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding failed", e);
        }
    }

    private String sanitizeAsciiFallback(String fileName) {
        // fallback용 ASCII-safe 이름
        return fileName.replaceAll("[^a-zA-Z0-9 _.-]", "_");
    }
}
