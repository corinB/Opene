package com.example.openSource.controller;


import com.example.openSource.service.FileUploadService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Controller
@RequestMapping("/home")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService service;


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

//                // RedirectAttributes에 파일 경로와 스타일 추가
//                redirectAttributes.addFlashAttribute("filePath", filePath);
//                redirectAttributes.addFlashAttribute("style", style);
                log.info(filePath);
                log.info(style);

                try{
                    var imgUrls = service.process(style, filePath);
                    log.info("imgUrls = {}", imgUrls);
                    model.addAttribute("imgUrls", imgUrls);
                    return "files/imageShow";
                }catch(Exception e){
                    log.error("Image processing failed", e);
                    return "redirect:/home/upload";
                }
            }
        } catch (RuntimeException e) {
            log.error("File upload failed", e);
            return "redirect:/home/upload";
        }
        return "redirect:/home/upload";
    }
}
