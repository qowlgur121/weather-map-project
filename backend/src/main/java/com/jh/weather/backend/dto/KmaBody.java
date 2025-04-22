// dto 패키지 안에 KmaBody.java 생성
package com.jh.weather.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KmaBody {
    private String dataType; // 데이터 타입 (예: "JSON")
    private KmaItems items;  // 예보 아이템 목록을 담는 객체
    private Integer pageNo;
    private Integer numOfRows;
    private Integer totalCount;
}