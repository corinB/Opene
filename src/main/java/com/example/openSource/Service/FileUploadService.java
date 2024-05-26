package com.example.openSource.Service;

import com.example.openSource.repository.FileSystemRepository;
import com.example.openSource.util.DirectoryCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
public class FileUploadService {

    private final FileSystemRepository repository;

    @Autowired
    public FileUploadService(FileSystemRepository repository) {
        this.repository = repository;
    }

    public void store(MultipartFile file){
        repository.store(file);
    }

    public void clearUploadDirectory() {
        String uploadDirectory = "C:/upload-dir"; // 업로드 디렉토리 경로
        DirectoryCleaner.cleanDirectory(uploadDirectory);
    }
}
