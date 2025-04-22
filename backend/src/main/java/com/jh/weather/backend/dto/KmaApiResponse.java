// dto 패키지 안에 KmaApiResponse.java 생성
package com.jh.weather.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // 매핑되지 않는 필드 무시
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // JSON에는 있지만 DTO에는 없는 필드는 무시
public class KmaApiResponse {
    private KmaResponse response; // 최상위 response 객체
}