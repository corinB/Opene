package com.example.openSource.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Repository
public class FileSystemRepository {

    private final Path storeLocation = Paths.get("C:/upload-dir");

    public void store(MultipartFile file) {
        if(file.isEmpty()){
            throw new RuntimeException("failed to store empty file");
        }
        Path destinationFile = this.storeLocation.resolve(
                Paths.get(file.getOriginalFilename()))
                .normalize().toAbsolutePath();
        log.debug("DestinationPath: " + destinationFile.toString());

        if(!destinationFile.getParent().equals(this.storeLocation.toAbsolutePath())){
            throw new RuntimeException("Cannot store file outside current directory");
        }

        try(InputStream inputStream = file.getInputStream()){
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully file stored: {}", destinationFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file",e);
        }
    }
}
