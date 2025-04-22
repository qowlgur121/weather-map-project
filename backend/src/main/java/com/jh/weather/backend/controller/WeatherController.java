package com.jh.weather.backend.controller;

import com.jh.weather.backend.dto.RegionWeatherDto;
import com.jh.weather.backend.service.WeatherService;
import com.jh.weather.backend.util.GpsConverter; // GpsConverter 임포트
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap; // HashMap 임포트 추가
// WeatherController.java 상단에 필요한 import 추가
import org.springframework.web.bind.annotation.PostMapping; // GetMapping -> PostMapping
import org.springframework.web.bind.annotation.RequestBody; // RequestBody 추가
import com.jh.weather.backend.dto.RegionRequestDto; // 새로 만든 DTO 임포트
import java.util.ArrayList; // ArrayList 임포트

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:8080", "http://127.0.0.1:5173"}) // ★★★ 본인 Frontend 주소 확인!
public class WeatherController {

    private final WeatherService weatherService;

    // WeatherController 클래스 내부의 getWeatherForRegions 메소드를 아래와 같이 수정
    @PostMapping("/weather/regions") // GET -> POST 변경
    public ResponseEntity<List<RegionWeatherDto>> getWeatherForRegions(
            @RequestBody List<RegionRequestDto> regions, // ★ @RequestParam 대신 @RequestBody 사용
            @RequestParam("type") String weatherType) {

        log.info("Request received for weather: {} regions, type={}", regions.size(), weatherType);

        // Service 호출 시 List<RegionRequestDto>를 그대로 전달
        List<RegionWeatherDto> weatherData = weatherService.getWeatherForRegions(regions, weatherType);

        // 결과 반환
        return ResponseEntity.ok(weatherData);
    }

    // 테스트용: 위경도 -> 격자 좌표 변환 API
    @GetMapping("/coordinates/to-grid")
    public ResponseEntity<Map<String, Integer>> convertGpsToGrid(
            @RequestParam double lat,
            @RequestParam double lon) {
        GpsConverter.LatXLonY grid = GpsConverter.convertGpsToGrid(lat, lon);
        Map<String, Integer> response = Map.of("nx", grid.getX(), "ny", grid.getY());
        return ResponseEntity.ok(response);
    }
}