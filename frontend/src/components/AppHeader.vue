<template>
  <div class="header-container">
    <button class="header-button" @click="goHome">Home</button>
    <button class="header-button" @click="selectWeatherType('TMP')">현재기온</button>
    <button class="header-button" @click="selectWeatherType('REH')">습도</button>
    <button class="header-button" @click="selectWeatherType('POP')">강수확률</button>
    <button class="header-button" @click="selectWeatherType('WSD')">풍속</button>
  </div>
</template>

<script setup>
//import { useWeatherStore } from '@/stores/weatherStore'; // ★ 스토어 임포트
// defineEmits는 컴파일러 매크로이므로 import 불필요
import { defineEmits } from 'vue'; // 혹시 모르니 일단 추가 (보통 불필요)

// 부모로 보낼 이벤트 정의
const emit = defineEmits(['navigate-home', 'select-weather']);

// Home 버튼 클릭 처리
const goHome = () => {
  console.log('AppHeader: Home 버튼 클릭됨');
  emit('navigate-home');
};

// 날씨 타입 선택 버튼 클릭 처리
const selectWeatherType = (weatherType) => {
  console.log(`AppHeader: 날씨 타입 선택됨 - ${weatherType}`);
  emit('select-weather', weatherType); // 선택된 날씨 타입을 부모에게 전달
};
</script>

<style scoped>
.header-container {
  position: absolute; /* 지도 위에 위치 */
  top: 10px;
  left: 10px;
  padding: 8px; /* 내부 여백 */
  background-color: rgba(255, 255, 255, 0.6); /* 반투명 배경 */
  backdrop-filter: blur(10px); /* Glassmorphism: 블러 효과 */
  -webkit-backdrop-filter: blur(10px); /* Safari 지원 */
  border: 1px solid rgba(255, 255, 255, 0.18); /* 희미한 테두리 */
  border-radius: 10px; /* 둥근 모서리 */
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); /* 그림자 효과 */
  z-index: 1000; /* 다른 요소 위에 오도록 */
  display: flex; /* 버튼 가로 정렬 */
  gap: 8px; /* 버튼 사이 간격 */
}

.header-button {
  padding: 8px 15px;
  background-color: rgba(255, 255, 255, 0.8); /* 버튼 배경 */
  border: 1px solid rgba(200, 200, 200, 0.5);
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  transition: background-color 0.2s, transform 0.1s;
}

.header-button:hover {
  background-color: rgba(255, 255, 255, 1); /* 호버 시 밝게 */
}

.header-button:active {
   transform: scale(0.98); /* 클릭 시 약간 작아지는 효과 */
}
</style>