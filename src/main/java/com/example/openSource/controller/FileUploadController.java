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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

//                // RedirectAttributes에 파일 경로와 스타일 추가
//                redirectAttributes.addFlashAttribute("filePath", filePath);
//                redirectAttributes.addFlashAttribute("style", style);
                log.info(filePath);
                log.info(style);


                // imageProcess 메서드로 리다이렉트
                return "redirect:/home/process/image";
            }
        } catch (RuntimeException e) {
            log.error("File upload failed", e);
            return "redirect:/home/upload";
        }
        return "redirect:/home/upload";
    }

    @GetMapping("/process/image")
    public String processImage(@RequestParam String filePath,
                               @RequestParam String style,
                               Model model) {
        var imgUrls = service.process(style,filePath);
        try {
            // Flask 서버의 이미지 URI 배열을 가져오는 API 엔드포인트
            String apiUrl = "";

//            // Flask 서버로부터 이미지 URI 배열을 받아옴
//            String[] imageUrls = restTemplate.getForObject(apiUrl, String[].class);

            // 임시
            //String[] imageUrls = {"/img/amekaji/t1.webp", "/img/amekaji/t2.webp"};

            // 모델에 이미지 URI 배열 추가
            model.addAttribute("imageUrls", imgUrls);
            return "files/imageShow";
        } catch (RuntimeException e) {
            log.error("Image processing failed", e);
        }
        return "redirect:/home/upload";
    }
}
