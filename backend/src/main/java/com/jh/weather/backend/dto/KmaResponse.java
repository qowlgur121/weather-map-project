// dto 패키지 안에 KmaResponse.java 생성
package com.jh.weather.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KmaResponse {
    private KmaHeader header;
    private KmaBody body;
}