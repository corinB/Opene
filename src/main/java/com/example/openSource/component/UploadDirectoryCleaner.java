package com.example.openSource.component;

import com.example.openSource.Service.FileUploadService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UploadDirectoryCleaner {

    @Autowired
    private FileUploadService fileUploadService;

    @PostConstruct
    public void init() {
        fileUploadService.clearUploadDirectory();
    }
}
