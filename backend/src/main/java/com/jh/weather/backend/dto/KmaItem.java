// dto 패키지 안에 KmaItem.java 생성
package com.jh.weather.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // JSON의 다른 필드(nx, ny, baseDate, baseTime)는 무시
public class KmaItem {
    // 필요한 예보 정보 필드만 정의
    private String category;   // 자료 구분 코드 (TMP, POP, PTY, REH, SKY, VEC, WSD 등)
    private String fcstDate;   // 예보 일자 (YYYYMMDD)
    private String fcstTime;   // 예보 시각 (HHMM)
    private String fcstValue;  // 예보 값 (문자열)
    // 필요하다면 nx, ny 등 다른 필드도 추가 가능
}