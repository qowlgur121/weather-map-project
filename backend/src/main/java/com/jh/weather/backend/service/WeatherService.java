package com.jh.weather.backend.service;

import com.jh.weather.backend.dto.KmaApiResponse;
import com.jh.weather.backend.dto.KmaItem;
import com.jh.weather.backend.dto.RegionRequestDto;
import com.jh.weather.backend.dto.RegionWeatherDto;
import com.jh.weather.backend.util.GpsConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap; // HashMap 임포트 추가
import java.time.LocalDateTime; // 날짜/시간 관련 임포트 추가
import java.time.format.DateTimeFormatter; // 날짜/시간 포맷 관련 임포트 추가
import org.springframework.http.ResponseEntity; // ResponseEntity 임포트 추가
import org.springframework.web.client.HttpClientErrorException; // 예외 임포트 추가
import org.springframework.web.client.HttpServerErrorException; // 예외 임포트 추가
import org.springframework.web.client.RestClientException; // 예외 임포트 추가
import org.springframework.web.util.UriComponentsBuilder; // UriComponentsBuilder 임포트 추가
import java.net.URI; // URI 임포트 추가
import java.net.URLEncoder; // URLEncoder 임포트 추가
import java.nio.charset.StandardCharsets; // StandardCharsets 임포트 추가
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; // ★ ObjectMapper 주입 추가

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.base-url}")
    private String apiBaseUrl;

    // WeatherService 클래스 내부의 getWeatherForRegions 메소드를 아래 코드로 업데이트
    /**
     * 여러 지역(위경도)의 특정 날씨 정보를 조회하는 메소드 (API 호출 최적화 포함)
     * @param regions 조회할 지역(코드, 위도, 경도) 정보 리스트
     * @param weatherType 조회할 날씨 종류 (TMP, POP 등)
     * @return 각 지역의 날씨 정보를 담은 DTO 리스트
     */
    public List<RegionWeatherDto> getWeatherForRegions(List<RegionRequestDto> regions, String weatherType) {
        log.info("Fetching weather for {} regions, type: {}", regions.size(), weatherType);
        List<RegionWeatherDto> results = new ArrayList<>(); // 최종 결과를 담을 리스트

        // 0. 유효한 base_date, base_time 계산 (API 호출 전 한 번만)
        Map<String, String> baseDateTime = getValidBaseDateTime();
        if (baseDateTime.get("baseDate") == null || baseDateTime.get("baseTime") == null) {
            log.error("Cannot proceed without valid base date/time.");
            // 모든 요청된 지역에 대해 오류 DTO 반환
            for(RegionRequestDto region : regions) {
                results.add(new RegionWeatherDto(region.getCode(), weatherType, "Error", "BaseTime"));
            }
            return results;
        }

        // 1. 최적화: 지역들을 격자 좌표(nx, ny) 기준으로 그룹화
        //   -> Key: "nx,ny" 문자열, Value: 해당 격자에 속하는 RegionRequestDto 리스트
        Map<String, List<RegionRequestDto>> regionsByGrid = new HashMap<>();
        for (RegionRequestDto region : regions) {
            try {
                GpsConverter.LatXLonY grid = GpsConverter.convertGpsToGrid(region.getLat(), region.getLon());
                String gridKey = grid.getX() + "," + grid.getY();
                // computeIfAbsent: 키가 없으면 새 ArrayList를 만들고, 있으면 기존 리스트를 반환하여 region 추가
                regionsByGrid.computeIfAbsent(gridKey, k -> new ArrayList<>()).add(region);
            } catch (Exception e) {
                log.error("Error converting GPS for region {}: lat={}, lon={}", region.getCode(), region.getLat(), region.getLon(), e);
                // 오류 발생 시 해당 지역은 결과에 포함하지 않거나 오류 DTO 추가 가능
                results.add(new RegionWeatherDto(region.getCode(), weatherType, "Error", "GPS Conv"));
            }
        }
        log.info("Grouped {} regions into {} unique grid cells.", regions.size(), regionsByGrid.size());

        // 2. 각 고유 격자점(gridKey)에 대해 API 호출 수행
        for (Map.Entry<String, List<RegionRequestDto>> entry : regionsByGrid.entrySet()) {
            String gridKey = entry.getKey();
            List<RegionRequestDto> regionsInThisGrid = entry.getValue();
            // 이 격자점을 대표하는 지역 정보 (API 호출 시 사용할 위경도)
            RegionRequestDto representativeRegion = regionsInThisGrid.get(0); // 첫 번째 지역 사용
            double representativeLat = representativeRegion.getLat();
            double representativeLon = representativeRegion.getLon();

            // 3. 해당 격자점의 날씨 데이터 API 호출
            String jsonResponse = getWeatherDataFromApi(representativeLat, representativeLon); // 내부적으로 nx, ny 변환 및 baseDateTime 사용

            // 4. API 응답 파싱
            KmaApiResponse kmaResponse = parseWeatherData(jsonResponse); // 이전 단계에서 만든 파싱 메소드 사용

            // 5. 파싱 결과 처리
            String value = "N/A"; // 기본값: 데이터 없음
            String unit = "";     // 기본값: 단위 없음
            boolean apiError = true; // API 또는 파싱 오류 여부 플래그

            if (kmaResponse != null) {
                apiError = false; // 일단 파싱 성공
                // 필요한 날씨 항목(KmaItem) 찾기
                KmaItem targetItem = findTargetWeatherItem(kmaResponse, weatherType, baseDateTime.get("baseDate"), baseDateTime.get("baseTime"));
                if (targetItem != null) {
                    value = targetItem.getFcstValue(); // 예보 값 추출
                    unit = getWeatherUnit(weatherType); // 날씨 타입에 맞는 단위 가져오기
                    log.debug("Found value for {} at grid {}: {}", weatherType, gridKey, value);
                } else {
                    log.warn("Weather type '{}' not found in API response for grid {}", weatherType, gridKey);
                    // value는 "N/A" 유지
                }
            } else {
                log.error("Failed to get or parse weather data for grid {}", gridKey);
                value = "Error"; // 오류 표시
                unit = "Parse";
            }

            // 6. 이 격자점에 속하는 모든 지역에 대해 결과 DTO 생성 및 추가
            for (RegionRequestDto region : regionsInThisGrid) {
                if (apiError) { // API 호출 또는 파싱 자체에서 오류가 난 경우
                    results.add(new RegionWeatherDto(region.getCode(), weatherType, "Error", unit));
                } else { // API 호출/파싱은 성공했으나 해당 weatherType 데이터가 없을 수도 있음
                    results.add(new RegionWeatherDto(region.getCode(), weatherType, value, unit));
                }
            }
        }

        return results;
    }

    // --- Helper Methods ---

    /**
     * 파싱된 KmaApiResponse 객체에서 원하는 날씨 타입(weatherType)의
     * 가장 적절한 예보 항목(KmaItem)을 찾는 헬퍼 메소드.
     * 현재는 가장 가까운 예보 시간을 가진 항목을 찾는 로직 (개선 필요)
     */
    private KmaItem findTargetWeatherItem(KmaApiResponse kmaResponse, String weatherType, String baseDate, String baseTime) {
        if (kmaResponse == null || kmaResponse.getResponse() == null || kmaResponse.getResponse().getBody() == null || kmaResponse.getResponse().getBody().getItems() == null || kmaResponse.getResponse().getBody().getItems().getItem() == null) {
            return null;
        }

        List<KmaItem> items = kmaResponse.getResponse().getBody().getItems().getItem();
        KmaItem foundItem = null;
        // 가장 가까운 미래 예보 시간을 찾기 위한 로직 (개선된 버전)
        LocalDateTime baseDateTime = LocalDateTime.parse(baseDate + baseTime, DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        LocalDateTime nearestFcstDateTime = null;

        for (KmaItem item : items) {
            if (weatherType.equals(item.getCategory())) {
                try {
                    LocalDateTime fcstDateTime = LocalDateTime.parse(item.getFcstDate() + item.getFcstTime(), DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
                    // 발표 시각 이후의 예보 중에서
                    if (!fcstDateTime.isBefore(baseDateTime)) {
                        // 가장 가까운 예보 시간을 찾음
                        if (nearestFcstDateTime == null || fcstDateTime.isBefore(nearestFcstDateTime)) {
                            nearestFcstDateTime = fcstDateTime;
                            foundItem = item;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse fcstDate/Time: {}/{}", item.getFcstDate(), item.getFcstTime(), e);
                }
            }
        }

        if(foundItem != null) {
            log.debug("Selected item for {}: Date={}, Time={}, Value={}",
                    weatherType, foundItem.getFcstDate(), foundItem.getFcstTime(), foundItem.getFcstValue());
        } else {
            log.warn("No suitable item found for category: {}", weatherType);
        }
        return foundItem;
    }

    /**
     * 날씨 타입 코드(category)에 따른 단위를 반환하는 헬퍼 메소드
     */
    private String getWeatherUnit(String weatherType) {
        return switch (weatherType) {
            case "TMP", "TMN", "TMX", "T1H" -> "℃"; // 기온, 최저/최고 기온, (초단기)기온
            case "POP" -> "%";             // 강수확률
            case "PTY" -> "코드";          // 강수형태 (코드값 그대로 반환, 프론트에서 처리)
            case "PCP", "RN1" -> "mm";     // 1시간 강수량 (값은 문자열 "강수없음" 또는 숫자), (초단기)강수량
            case "REH" -> "%";             // 습도
            case "SNO" -> "cm";            // 1시간 신적설 (값은 문자열 "적설없음" 또는 숫자)
            case "SKY" -> "코드";          // 하늘상태 (코드값 그대로 반환)
            case "UUU", "VVV" -> "m/s";    // 동서/남북 바람성분
            case "WAV" -> "M";             // 파고
            case "VEC" -> "deg";           // 풍향 (각도)
            case "WSD" -> "m/s";           // 풍속
            default -> "";
        };
    }

    /**
     * 특정 위경도의 날씨 API 원본 응답(JSON)을 가져오는 메소드
     */
    public String getWeatherDataFromApi(double latitude, double longitude) {
        GpsConverter.LatXLonY gridCoords = GpsConverter.convertGpsToGrid(latitude, longitude);
        int nx = gridCoords.getX();
        int ny = gridCoords.getY();
        log.info("Converted GPS ({}, {}) to Grid ({}, {})", latitude, longitude, nx, ny);

        Map<String, String> baseDateTime = getValidBaseDateTime();
        String baseDate = baseDateTime.get("baseDate");
        String baseTime = baseDateTime.get("baseTime");

        if (baseDate == null || baseTime == null) {
            log.error("Failed to get valid base date and time.");
            return "{\"error\": \"Failed to calculate base date/time\"}";
        }
        log.info("Using baseDate: {}, baseTime: {} for grid ({}, {})", baseDate, baseTime, nx, ny);

        try {
            // 서비스 키는 URL 인코딩 필수
            String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8.toString());

            URI uri = UriComponentsBuilder
                    .fromUriString(apiBaseUrl + "/getVilageFcst") // 오퍼레이션 경로 포함!
                    .queryParam("serviceKey", encodedApiKey) // 인코딩된 키 사용
                    .queryParam("pageNo", 1)
                    // ★★★ numOfRows 설정: 단기예보는 보통 3시간 간격, 하루 8번 발표.
                    // 각 발표 시각마다 여러 항목(TMP, POP, PTY, REH, SKY, VEC, WSD 등 약 10~12개)
                    // 보통 +3일(72시간) 정도의 예보 제공.
                    // 넉넉하게 1000개 정도 요청하면 대부분의 데이터를 한 번에 받을 수 있음.
                    .queryParam("numOfRows", 1000)
                    .queryParam("dataType", "JSON")
                    .queryParam("base_date", baseDate)
                    .queryParam("base_time", baseTime)
                    .queryParam("nx", nx)
                    .queryParam("ny", ny)
                    .build(true) // 인코딩된 서비스 키가 있으므로 true로 설정
                    .toUri();

            log.debug("Requesting URL: {}", uri); // 로그 레벨 debug로 변경 (URL이 너무 길어서)

            // RestTemplate으로 GET 요청 보내고 응답 받기
            // getForEntity: 응답 본문뿐 아니라 상태 코드, 헤더 등 전체 응답을 받음
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);

            // 응답 상태 코드 확인
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                // 성공 (200 OK 등)
                log.info("API call successful for grid ({}, {}). Status: {}", nx, ny, responseEntity.getStatusCode());
                if (responseEntity.getBody() == null || responseEntity.getBody().isEmpty()) {
                    log.warn("API response body is null or empty for grid ({}, {})", nx, ny);
                    return "{\"error\": \"Empty response body from API\"}";
                }
                // 정상 응답 JSON 문자열 반환
                return responseEntity.getBody();
            } else {
                // API 서버에서 오류 응답 (4xx, 5xx 등)
                log.error("API call failed for grid ({}, {}) with status: {}. Response: {}", nx, ny, responseEntity.getStatusCode(), responseEntity.getBody());
                return "{\"error\": \"API call failed\", \"status\": " + responseEntity.getStatusCodeValue() + "}";
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // API 서버 오류 (4xx, 5xx) - RestClientException의 하위 예외
            log.error("API Error for grid ({}, {}): Status Code = {}, Response Body = {}", nx, ny, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return "{\"error\": \"API returned error\", \"status\": " + e.getStatusCode().value() + ", \"message\": \"" + e.getStatusText() + "\"}";
        } catch (RestClientException e) {
            // 네트워크 연결 오류, 타임아웃 등 RestTemplate 관련 오류
            log.error("Network or RestTemplate Error for grid ({}, {}): {}", nx, ny, e.getMessage(), e);
            return "{\"error\": \"Network or RestTemplate error\", \"message\": \"" + e.getMessage() + "\"}";
        } catch (Exception e) {
            // 기타 예외 (URL 인코딩 실패 등)
            log.error("Unexpected error during API call for grid ({}, {})", nx, ny, e);
            return "{\"error\": \"Unexpected error\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 현재 시간 기준, API 조회 가능한 가장 최신의 base_date, base_time 계산
     */
    private Map<String, String> getValidBaseDateTime() {
        LocalDateTime now = LocalDateTime.now();
        int[] baseTimes = {200, 500, 800, 1100, 1400, 1700, 2000, 2300};
        int currentHourMinute = now.getHour() * 100 + now.getMinute();
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = null;

        for (int i = baseTimes.length - 1; i >= 0; i--) {
            int availableTime = baseTimes[i] + 10; // API 제공 시작 시간 (발표 시간 + 10분)
            if (currentHourMinute >= availableTime) {
                baseTime = String.format("%04d", baseTimes[i]);
                break;
            }
        }
        if (baseTime == null) {
            baseDate = now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            baseTime = "2300";
        }
        Map<String, String> result = new HashMap<>();
        result.put("baseDate", baseDate);
        result.put("baseTime", baseTime);
        return result;
    }

    /**
     * API 응답(JSON 문자열)을 파싱하여 KmaApiResponse 객체로 변환하는 private 헬퍼 메소드
     */
    private KmaApiResponse parseWeatherData(String jsonResponse) {
        try {
            // JSON 문자열을 KmaApiResponse DTO 객체로 변환
            KmaApiResponse response = objectMapper.readValue(jsonResponse, KmaApiResponse.class);

            // API 자체 오류 코드 확인 (예: "03" - NODATA_ERROR)
            if (response == null || response.getResponse() == null || response.getResponse().getHeader() == null) {
                log.error("Failed to parse JSON or missing header structure: {}", jsonResponse);
                return null; // 또는 예외 발생
            }
            if (!"00".equals(response.getResponse().getHeader().getResultCode())) {
                log.error("KMA API Error: Code={}, Msg={}",
                        response.getResponse().getHeader().getResultCode(),
                        response.getResponse().getHeader().getResultMsg());
                // NODATA_ERROR 등 특정 코드에 대한 처리가 필요할 수 있음
                return null; // 또는 예외 발생 / 특정 오류 객체 반환
            }
            // Body나 Items가 null인 경우도 처리
            if (response.getResponse().getBody() == null || response.getResponse().getBody().getItems() == null || response.getResponse().getBody().getItems().getItem() == null) {
                log.warn("KMA API Response body or items are null/empty for resultCode 00.");
                // 데이터가 없는 정상 응답일 수 있음
                // return response; // 헤더 정보만 있는 응답 객체 반환 가능
                return null; // 여기서는 파싱 실패로 간주
            }

            return response; // 성공적으로 파싱된 객체 반환
        } catch (Exception e) {
            // JSON 파싱 중 오류 발생 시
            log.error("Failed to parse weather data JSON: {}", jsonResponse, e);
            return null; // 또는 예외 발생
        }
    }
}