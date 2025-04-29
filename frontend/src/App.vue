<!-- App.vue -->
<template>
  <div id="app-container">
    <AppHeader @navigate-home="moveToCurrentUserLocation" />

    <main id="main-content">
      <!-- ★★★ MapView에 이벤트 리스너 연결 ★★★ -->
      <MapView
        ref="mapViewRef"
        @region-hover="updateSidebar"
        @region-mouseout="clearSidebar"
        @region-click="handleRegionClick"
      />
    </main>

    <!-- ★★★ Sidebar 컴포넌트 사용 ★★★ -->
    <AppSidebar />

    <div class="map-controls">
      <button @click="zoomIn">+</button>
      <button @click="zoomOut">-</button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'; // watch 추가
import AppHeader from './components/AppHeader.vue';
import MapView from './components/MapView.vue';
import AppSidebar from './components/AppSidebar.vue'; // Sidebar 컴포넌트 이름 변경
import { useMapStore } from './stores/mapStore'; // mapStore import 추가

const mapViewRef = ref(null);
const hoveredRegionName = ref(''); // Sidebar 표시용 임시 상태
const hoveredRegionCode = ref(''); // Sidebar 표시용 임시 상태
const mapStore = useMapStore(); // mapStore 인스턴스 생성

// Home 버튼 클릭 처리 수정
const moveToCurrentUserLocation = () => {
  console.log('App.vue: navigate-home 이벤트 수신');
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const latitude = position.coords.latitude;
        const longitude = position.coords.longitude;
        console.log(`현재 위치: 위도 ${latitude}, 경도 ${longitude}`);

        // ★★★ MapView의 노출된 메소드 호출 ★★★
        if (mapViewRef.value) {
          mapViewRef.value.moveToLocation(latitude, longitude, 14); // 줌 레벨 14로 이동
          // TODO: 마커 추가 로직도 MapView 내부 메소드로 만들거나 여기서 직접 처리
           // (여기서 하려면 여전히 map 객체 접근 필요... moveToLocation에 마커 추가 로직 포함시키는게 나을수도)
        } else {
           console.error('App.vue: MapView 참조를 찾을 수 없습니다.');
        }
      },
      (error) => {
        console.error('App.vue: Geolocation 오류:', error.message);
        alert(`위치 정보를 가져올 수 없습니다: ${error.message}`);
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    );
  } else {
    console.error('App.vue: 이 브라우저는 Geolocation을 지원하지 않습니다.');
    alert('현재 위치 기능을 사용할 수 없는 브라우저입니다.');
  }
};

// 확대 버튼 클릭 처리 수정
const zoomIn = () => {
  console.log("App.vue: zoomIn 버튼 클릭");
  if (mapViewRef.value) {
    mapViewRef.value.zoomInMap(); // ★ MapView의 메소드 호출
  } else {
    console.error("App.vue: MapView 참조 없음 (zoomIn)");
  }
};

// 축소 버튼 클릭 처리 수정
const zoomOut = () => {
  console.log("App.vue: zoomOut 버튼 클릭");
  if (mapViewRef.value) {
    mapViewRef.value.zoomOutMap(); // ★ MapView의 메소드 호출
  } else {
     console.error("App.vue: MapView 참조 없음 (zoomOut)");
  }
};

// MapView에서 'region-hover' 이벤트 발생 시 실행
const updateSidebar = (regionInfo) => {
  console.log("App.vue: region-hover event received", regionInfo); // 로그 추가
  hoveredRegionName.value = regionInfo.name;
  hoveredRegionCode.value = regionInfo.code;
};

// MapView에서 'region-mouseout' 이벤트 발생 시 실행
const clearSidebar = () => {
  console.log("App.vue: region-mouseout event received"); // 로그 추가
   hoveredRegionName.value = '';
   hoveredRegionCode.value = '';
};

import { useWeatherStore } from './stores/weatherStore';

const weatherStore = useWeatherStore();

const handleRegionClick = (regionInfo) => {
  console.log("App.vue: region-click event received", regionInfo);
  // 필요한 로직 추가
};

watch(() => mapStore.visibleRegions, (newVisibleRegionsData) => {
  console.log("App.vue: visibleRegions 변경 감지됨", newVisibleRegionsData);

  // newVisibleRegionsData가 유효한 배열인지 확인
  if (Array.isArray(newVisibleRegionsData) && newVisibleRegionsData.length > 0) {
    // 지역 코드와 좌표 정보를 담을 배열
    const regionsWithCoords = newVisibleRegionsData.map(region => ({
      code: region.code,
      lat: region.lat,
      lon: region.lon
    }));

    console.log("App.vue: fetchWeatherData 호출 전 regionsWithCoords", regionsWithCoords);
    weatherStore.fetchWeatherData({
      type: weatherStore.selectedWeatherType,
      regions: regionsWithCoords
    });
  } else {
    console.warn("App.vue: 유효하지 않은 visibleRegions 데이터:", newVisibleRegionsData);
  }
}, { deep: true });

</script>


<style>
/* 전역 스타일 */
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
  overflow: hidden; /* 스크롤바 제거 */
  font-family: Avenir, Helvetica, Arial, sans-serif; /* 기본 폰트 적용 */
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  position: relative; /* 자식 absolute 요소 기준점 */
}

#main-content {
  flex-grow: 1; /* 남은 공간 차지 */
  position: relative; /* 내부 요소의 position: absolute 기준 */
}

/* Sidebar 스타일 */
/* AppSidebar.vue에서 스타일 관리 */

/* 지도 컨트롤 스타일 */
.map-controls {
  position: absolute;
  right: 20px;
  bottom: 20px;
  display: flex;
  flex-direction: column;
  z-index: 1000;
}

.map-controls button {
  width: 34px; /* 버튼 크기 조정 */
  height: 34px;
  margin-bottom: 8px;
  cursor: pointer;
  border: 1px solid #ccc;
  background-color: white;
  border-radius: 4px;
  font-size: 18px; /* 아이콘 크기 */
  font-weight: bold;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
  transition: background-color 0.2s;
  display: flex;
  justify-content: center;
  align-items: center;
}

.map-controls button:hover {
   background-color: #f0f0f0;
}
</style>
