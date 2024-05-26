package com.example.openSource.Service;

import com.example.openSource.repository.FileSystemRepository;
import com.example.openSource.util.DirectoryCleaner;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

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
    public String process(String style,String path){
        URI uri = UriComponentsBuilder.fromUriString("http://localhost:5000")
                .queryParam("style", style)
                .queryParam("path", path)
                .encode().build().toUri();

        var reqEntity = RequestEntity.get(uri).build();
        var resEntity = restTemplate.exchange(reqEntity, String.class);
        var res = resEntity.getBody();

        return res;
    }
}