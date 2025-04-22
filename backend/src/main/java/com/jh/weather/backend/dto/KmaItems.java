// dto 패키지 안에 KmaItems.java 생성
package com.jh.weather.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List; // List 임포트

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KmaItems {
    // 중요하다! 실제 JSON 필드명 "item"과 변수명을 맞춰야 함!
    private List<KmaItem> item; // 예보 아이템 리스트
}