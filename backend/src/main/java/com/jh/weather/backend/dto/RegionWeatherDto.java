package com.jh.weather.backend.dto; // 본인 패키지 확인

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
// import lombok.Setter; // 필요하다면 추가

@Getter // 각 필드에 대한 public Getter 메소드를 자동으로 생성 (Lombok)
// @Setter // 필요 시 Setter 메소드 자동 생성
@NoArgsConstructor // 파라미터 없는 기본 생성자를 자동 생성 (JPA 등 일부 라이브러리에서 필요)
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자를 자동 생성 (테스트 또는 객체 생성 시 편리)
public class RegionWeatherDto {

    private String regionCode; // 지역 코드 (예: 시군구 코드 SIG_CD)
    private String weatherType; // 날씨 정보 종류 (예: "TMP", "POP", "REH", "WSD", "VEC")
    private String value;       // 예보 값 (문자열 형태 - 기상청 API 응답 기준)
    private String unit;        // 단위 (예: "℃", "%", "m/s")

    // 필요에 따라 다른 필드 추가 가능 (예: 예보 시간 fcstTime 등)
}