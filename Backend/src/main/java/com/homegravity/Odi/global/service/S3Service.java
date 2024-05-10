package com.homegravity.Odi.global.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.homegravity.Odi.global.response.error.ErrorCode;
import com.homegravity.Odi.global.response.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /*
    S3 파일 업로드
     */
    public String saveFile(MultipartFile multipartFile) {
        // 파일 원본 이름
        String originalFilename = multipartFile.getOriginalFilename();

        try {
            // S3 메타데이터 설정
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(multipartFile.getSize());
            metadata.setContentType(multipartFile.getContentType());

            // 버킷에 파일 저장
            amazonS3.putObject(bucket, originalFilename, multipartFile.getInputStream(), metadata);
            // 파일 주소 반환
            return amazonS3.getUrl(bucket, originalFilename).toString();
        } catch (IOException e) {
            log.info("S3 파일 저장 실퍠");
            throw new BusinessException(ErrorCode.S3_SAVE_ERROR, ErrorCode.S3_SAVE_ERROR.getMessage());
        }

    }

    /*
    S3 버킷 파일 삭제
     */
    public void deleteFile(String originalFilenUrl)  {
        try {
            // URL 주소에서 key값 생성
            String key = originalFilenUrl.split("/")[3];
            // 버킷에 파일 삭제
            amazonS3.deleteObject(bucket, key);
        } catch (Exception e) {
            log.info("S3 파일 삭제 실패");
            throw new BusinessException(ErrorCode.S3_DELETE_ERROR, ErrorCode.DELETE_ERROR.getMessage());
        }
    }
}