package com.jh.weather.backend.config; // 본인 패키지 확인

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration // 이 클래스가 스프링 설정 정보를 담고 있음을 알림
public class AppConfig {

    @Bean // 이 메소드가 반환하는 객체를 스프링 빈으로 등록
    public RestTemplate restTemplate() {
        // TODO: 실제 운영 환경에서는 타임아웃 등 추가 설정 필요
        return new RestTemplate();
    }
}