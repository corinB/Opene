package com.example.openSource.Service;

import com.example.openSource.repository.FileSystemRepository;
import com.example.openSource.util.DirectoryCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {

    private final FileSystemRepository repository;

    @Autowired
    public FileUploadService(FileSystemRepository repository) {
        this.repository = repository;
    }

    public String store(MultipartFile file){
        clearUploadDirectory(); // 파일 저장 전 디렉토리 비우기
        return repository.store(file);
    }

    private void clearUploadDirectory() {
        String uploadDirectory = "C:/upload-dir"; // 업로드 디렉토리 경로
        DirectoryCleaner.cleanDirectory(uploadDirectory);
    }
}