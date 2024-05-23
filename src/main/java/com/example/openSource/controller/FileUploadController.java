package com.example.openSource.controller;

import com.example.openSource.Service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/home")
public class FileUploadController {

    private final  FileUploadService service;

    @Autowired
    public FileUploadController(FileUploadService service){
        this.service = service;
    }

    @GetMapping("")
    public String home(){
        return "files/home";
    }

    @GetMapping("/upload")
    public String fileUploadHome(){
        return "files/file";
    }

    @PostMapping("/upload/image")
    public String fileUpload(@RequestParam("file") MultipartFile file,
                             @RequestParam("selectbox") String style,
                             RedirectAttributes redirectAttributes) {
        if (!file.isEmpty()) {
            log.info("file getOriginalFilename = {}", file.getOriginalFilename());
            log.info("selected style = {}", style);
            service.store(file);
            redirectAttributes.addFlashAttribute("message",
                    "Successfully file uploaded: " + file.getOriginalFilename() + " with style: " + style + "!");
        }
        return "files/imageShow";
    }
}
