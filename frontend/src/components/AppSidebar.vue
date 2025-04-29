<template>
  <aside class="sidebar-area">
    <h4>날씨 요약 정보</h4>
    <div v-if="mapStore.hoveredRegionInfo" class="region-info">
      <p><strong>{{ mapStore.hoveredRegionInfo.name }}</strong></p>
      <p><small>({{ mapStore.hoveredRegionInfo.code }})</small></p>
      <p v-if="weatherDataForHoveredRegion" class="weather-value">
        <strong>{{ weatherStore.selectedWeatherTypeName }}:</strong>
        {{ weatherDataForHoveredRegion.value }} {{ weatherDataForHoveredRegion.unit }}
      </p>
      <p v-else-if="weatherStore.selectedWeatherType" class="no-data">
        데이터 없음
      </p>
    </div>
    <div v-else>
      <p>지도 위 지역에 마우스를 올려보세요.</p>
    </div>

    <hr>

    <!-- ★★★ 범례 표시 영역 개선 ★★★ -->
    <div class="legend-area" v-if="weatherStore.selectedWeatherType && weatherStore.hasWeatherData">
      <h5>범례: {{ weatherStore.selectedWeatherTypeName }}</h5>

      <!-- 기온 범례 -->
      <div v-if="weatherStore.selectedWeatherType === 'TMP'" class="legend-item">
        <span class="legend-color" style="background: linear-gradient(to right, rgb(0, 0, 255), rgb(0, 255, 0), rgb(255, 255, 0), rgb(255, 0, 0));"></span>
        <span class="legend-label">0°C</span>
        <span class="legend-label" style="float: right;">35°C+</span>
      </div>

      <!-- 강수확률 범례 -->
      <div v-else-if="weatherStore.selectedWeatherType === 'POP'" class="legend-item">
        <span class="legend-color" style="background: linear-gradient(to right, rgb(255, 255, 255), rgb(200, 200, 255), rgb(50, 50, 255));"></span>
        <span class="legend-label">0%</span>
        <span class="legend-label" style="float: right;">100%</span>
      </div>

      <!-- 습도 범례 -->
      <div v-else-if="weatherStore.selectedWeatherType === 'REH'" class="legend-item">
        <span class="legend-color" style="background: linear-gradient(to right, rgb(180, 100, 150), rgb(100, 255, 255));"></span>
        <span class="legend-label">0%</span>
        <span class="legend-label" style="float: right;">100%</span>
      </div>

      <!-- 풍속 범례 -->
      <div v-else-if="weatherStore.selectedWeatherType === 'WSD'" class="legend-item">
        <span class="legend-color" style="background: linear-gradient(to right, rgb(255, 255, 255), rgb(50, 55, 50));"></span> <!-- 흰색 -> 보라색 계열 -->
        <span class="legend-label">0m/s</span>
        <span class="legend-label" style="float: right;">15m/s+</span>
      </div>

      <p v-else class="no-data">범례 정보 없음</p>
    </div>
    <div v-else-if="weatherStore.selectedWeatherType">
       <p class="no-data">날씨 데이터를 로드해주세요.</p>
    </div>

    <!-- 로딩 및 오류 상태 -->
    <p v-if="weatherStore.isLoading" class="loading-msg">🔄 날씨 정보 로딩 중...</p>
    <p v-if="weatherStore.error" class="error-msg">⚠️ 오류: {{ weatherStore.error }}</p>
  </aside>
</template>

<script setup>
  import { computed } from 'vue';
  import { useMapStore } from '@/stores/mapStore';
  import { useWeatherStore } from '@/stores/weatherStore';
  const mapStore = useMapStore();
  const weatherStore = useWeatherStore();

  // 마우스 오버된 지역의 날씨 데이터를 계산하는 computed 속성
  const weatherDataForHoveredRegion = computed(() => {
    if (mapStore.hoveredRegionInfo && weatherStore.weatherData) {
      const regionCode = mapStore.hoveredRegionInfo.code;
      const weatherData = weatherStore.weatherData[regionCode];
      if (weatherData && weatherData.value !== 'Error' && weatherData.value !== 'N/A') {
        // 단위 정보 추가 (예시)
        let unit = '';
        switch(weatherStore.selectedWeatherType) {
          case 'TMP': unit = '°C'; break;
          case 'POP': unit = '%'; break;
          case 'REH': unit = '%'; break;
          case 'WSD': unit = 'm/s'; break;
        }
        return {
          value: weatherData.value,
          unit: unit
        };
      }
    }
    return null; // 데이터 없거나 오류 시
  });

</script>

<style scoped>
  /* ... (기존 sidebar-area 스타일 유지 또는 개선) ... */
  .sidebar-area {
    position: absolute;
    right: 10px;
    top: 70px; /* Header 높이 + 여백 고려 */
    width: 220px; /* 너비 조정 */
    padding: 15px;
    background-color: rgba(255, 255, 255, 0.7); /* 반투명 배경 */
    backdrop-filter: blur(10px); /* Glassmorphism 효과 */
    -webkit-backdrop-filter: blur(10px); /* Safari 지원 */
    border: 1px solid rgba(255, 255, 255, 0.18);
    border-radius: 10px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    z-index: 1000; /* 다른 요소 위에 오도록 */
    color: #333;
    font-size: 14px;
  }
  .sidebar-area h4 {
    margin-top: 0;
    margin-bottom: 10px;
    color: #111;
  }
  .region-info p { margin: 3px 0; }
  .weather-value { font-weight: bold; }
  .no-data { color: #888; font-style: italic; }
  hr { border: 0; border-top: 1px solid #ddd; margin: 12px 0; }
  .legend-area h5 { margin-bottom: 8px; }
  .legend-item { margin-bottom: 5px; }
  .legend-color {
    display: inline-block;
    width: calc(100% - 80px); /* 양쪽 라벨 공간 제외 */
    height: 15px;
    margin: 0 5px;
    border: 1px solid #ccc;
    vertical-align: middle;
  }
  .legend-label {
    font-size: 12px;
    color: #555;
    vertical-align: middle;
  }
  .loading-msg { color: blue; }
  .error-msg { color: red; font-weight: bold; }
</style>
