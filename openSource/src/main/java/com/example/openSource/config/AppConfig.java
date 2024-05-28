package com.example.openSource.config;

import com.example.openSource.dto.FlaskResDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ParameterizedTypeReference<ArrayList<FlaskResDto>> parameterizedTypeReference() {
        return new ParameterizedTypeReference<ArrayList<FlaskResDto>>() {};
    }
}
