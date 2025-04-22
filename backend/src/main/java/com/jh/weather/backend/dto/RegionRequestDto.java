package com.jh.weather.backend.dto; // 본인 패키지 확인

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter // JSON 역직렬화를 위해 Setter 필요할 수 있음
@NoArgsConstructor
public class RegionRequestDto {
    private String code; // 지역 코드 (예: SIG_CD)
    private double lat;  // 해당 지역 대표 위도
    private double lon;  // 해당 지역 대표 경도
}