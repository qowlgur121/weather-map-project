// dto 패키지 안에 KmaHeader.java 생성
package com.jh.weather.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KmaHeader {
    private String resultCode; // 결과 코드 (예: "00")
    private String resultMsg;  // 결과 메시지 (예: "NORMAL_SERVICE")
}