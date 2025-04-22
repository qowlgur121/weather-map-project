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

    <aside class="sidebar-area">
      <h4>날씨 요약 정보</h4>
      <p>지역명: {{ hoveredRegionName }}</p>
      <p>지역코드: {{ hoveredRegionCode }}</p>
    </aside>

    <div class="map-controls">
      <button @click="zoomIn">+</button>
      <button @click="zoomOut">-</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import AppHeader from './components/AppHeader.vue';
import MapView from './components/MapView.vue';

const mapViewRef = ref(null);
const hoveredRegionName = ref(''); // Sidebar 표시용 임시 상태
const hoveredRegionCode = ref(''); // Sidebar 표시용 임시 상태


// Header에서 'navigate-home' 이벤트 수신 시 실행될 함수
const moveToCurrentUserLocation = () => {
  console.log('App.vue: navigate-home 이벤트 수신');
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const latitude = position.coords.latitude;
        const longitude = position.coords.longitude;
        console.log(`현재 위치: 위도 ${latitude}, 경도 ${longitude}`);

        if (mapViewRef.value && mapViewRef.value.map) {
          const currentMap = mapViewRef.value.map.value; // MapView에서 노출한 ref 변수 접근 시 .value 추가
          if (currentMap) { // map.value가 실제 지도 객체인지 한번 더 확인
             const newCenter = new window.naver.maps.LatLng(latitude, longitude);
             currentMap.setCenter(newCenter);
             currentMap.setZoom(14); // 적절한 줌 레벨
             // 기존 마커 제거 로직 필요 시 추가
             new window.naver.maps.Marker({
                position: newCenter,
                map: currentMap,
                title: '현재 위치'
             });
          } else {
             console.error('App.vue: MapView의 map 객체가 유효하지 않습니다.');
          }
        } else {
           console.error('App.vue: MapView 참조 또는 map 객체를 찾을 수 없습니다.');
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

// 확대 버튼 클릭 시
const zoomIn = () => {
  if (mapViewRef.value && mapViewRef.value.map && mapViewRef.value.map.value) {
    const currentMap = mapViewRef.value.map.value;
    currentMap.setZoom(currentMap.getZoom() + 1);
  }
};

// 축소 버튼 클릭 시
const zoomOut = () => {
   if (mapViewRef.value && mapViewRef.value.map && mapViewRef.value.map.value) {
    const currentMap = mapViewRef.value.map.value;
    currentMap.setZoom(currentMap.getZoom() - 1);
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

const handleRegionClick = (regionInfo) => {
  console.log("App.vue: region-click event received", regionInfo);
  // 필요한 로직 추가
};

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