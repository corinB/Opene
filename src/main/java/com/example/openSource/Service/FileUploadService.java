package com.example.openSource.Service;

import com.example.openSource.repository.FileSystemRepository;
import com.example.openSource.util.DirectoryCleaner;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class FileUploadService {

    private final FileSystemRepository repository;
    private final RestTemplate restTemplate;

    public String store(MultipartFile file){
        clearUploadDirectory(); // 파일 저장 전 디렉토리 비우기
        return repository.store(file);
    }

    private void clearUploadDirectory() {
        String uploadDirectory = "C:/upload-dir"; // 업로드 디렉토리 경로
        DirectoryCleaner.cleanDirectory(uploadDirectory);
    }
    public List<String> process(String style, String path) {
        try {
            // GET 요청으로 URI 구성
            URI uri = UriComponentsBuilder.fromUriString("http://localhost:5000/api")
                    .queryParam("style", style)
                    .queryParam("path", path)
                    .encode().build().toUri();

            // GET 요청 전송
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            // JSON 응답을 파싱하여 이미지 URI 배열을 반환
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, String>> responseList = objectMapper.readValue(response.getBody(), new TypeReference<>() {});

            // "path" 값을 추출하여 리스트로 반환
            List<String> imageUrls = new ArrayList<>();
            for (Map<String, String> map : responseList) {
                String relativePath = map.get("path");
                String fullPath = "/img/" + relativePath; // 상대 경로를 정적 리소스 경로로 변환
                imageUrls.add(fullPath);
            }
            return imageUrls;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process image with Flask server", e);
        }
    }
}