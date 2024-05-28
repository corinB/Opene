package com.example.openSource.controller;

import com.example.openSource.Service.FileUploadService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/home")
@AllArgsConstructor
public class FileUploadController {

    private final FileUploadService service;
    private final RestTemplate restTemplate;


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
                             Model model) {
        try {
            if (!file.isEmpty()) {
                log.info("file getOriginalFilename = {}", file.getOriginalFilename());
                log.info("selected style = {}", style);

                // 파일 저장
                String filePath = service.store(file);

                log.info(filePath);
                log.info(style);

                try{
                    var imgUrls = service.process(style,filePath);
//                    List<String> imgUrls = new ArrayList<>();
//                    imgUrls.add("/img/amekaji/t1.webp");
//                    imgUrls.add("/img/amekaji/t2.webp");
//                    imgUrls.add("/img/amekaji/t1.webp");
//                    imgUrls.add("/img/amekaji/t2.webp");
//                    imgUrls.add("/img/amekaji/t1.webp");
//                    imgUrls.add("/img/amekaji/t2.webp");
                    log.info("imageUrls = {}",imgUrls);
                    model.addAttribute("imageUrls", imgUrls);
                    return "files/imageShow";
                }catch (RuntimeException e) {
                    log.error("Image processing failed", e);
                }
                return "redirect:/home/upload";
            }
        } catch (RuntimeException e) {
            log.error("File upload failed", e);
            return "redirect:/home/upload";
        }
        return "redirect:/home/upload";
    }
}
