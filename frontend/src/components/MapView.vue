<template>
  <div id="map-container" style="width: 100%; height: 100%;"></div>
</template>

<script setup>
import { defineEmits } from 'vue';
import { defineExpose } from 'vue';
// defineExpose는 컴파일러 매크로이므로 import 불필요
import { onMounted, ref } from 'vue';
// TODO: Pinia 스토어 또는 emit 임포트 (상호작용용)
// import { useMapStore } from '@/stores/mapStore';
const emit = defineEmits(['region-hover', 'region-mouseout', 'region-click']);

const map = ref(null);
const geoJsonData = ref(null);
const currentPolygon = ref(null);
// const mapStore = useMapStore();

onMounted(() => {
  console.log("MapView: onMounted hook 시작");
  if (window.naver && window.naver.maps) {
    console.log("MapView: Naver Maps API 확인됨, initMap 호출");
    initMap();
  } else {
    console.error('MapView: Naver Maps API 스크립트 로드 실패!');
  }
});

const initMap = async () => {
  try {
    const mapContainer = document.getElementById('map-container');
    if (!mapContainer) {
      console.error('MapView: 지도 컨테이너 요소를 찾을 수 없습니다.');
      return;
    }
    console.log("MapView: 지도 컨테이너 요소 찾음");

    const mapOptions = {
      center: new window.naver.maps.LatLng(36.5, 127.5),
      zoom: 7, minZoom: 6, zoomControl: false, mapDataControl: false
    };
    console.log("MapView: 지도 옵션 설정 완료");

    map.value = new window.naver.maps.Map(mapContainer, mapOptions);
    console.log('MapView: Naver Map 객체 생성 완료.', map.value);

    await loadGeoJson();
    setMapDataStyleAndEvents();

  } catch (error) {
     console.error("MapView: initMap 함수 실행 중 오류 발생", error);
  }
};

const loadGeoJson = async () => {
  try {
    const response = await fetch('/korea_sig.geojson'); // public 폴더 기준
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    geoJsonData.value = await response.json();
    console.log("MapView: GeoJSON loaded successfully.");

    if (map.value && geoJsonData.value) {
      map.value.data.addGeoJson(geoJsonData.value, true);
      console.log("MapView: GeoJSON added to map data layer.");
    } else { console.error("MapView: Map or GeoJSON not ready."); }
  } catch (error) { console.error("MapView: GeoJSON load/add failed", error); }
};

const setMapDataStyleAndEvents = () => {
  if (!map.value) { console.error("MapView: Map not ready for styles/events."); return; }

  // 1. 기본 폴리곤 스타일 설정 (콜백 함수 대신 스타일 객체 직접 전달)
  map.value.data.setStyle({ // ★★★ 콜백 함수 제거, 스타일 객체 직접 전달 ★★★
    fillColor: '#ffffff', // 기본 채우기: 흰색
    fillOpacity: 0.5,     // 기본 투명도
    strokeColor: '#666666', // 기본 테두리: 회색
    strokeWeight: 1,       // 기본 테두리 두께
    clickable: true        // 클릭 가능하게 설정
  });

  // 2. Mouseover
  map.value.data.addListener('mouseover', (e) => {
    const feature = e.feature;
    //const featureProperties = feature.getProperties();
    const regionName = feature.getProperty('name'); // 예: 'name' 또는 'SIG_KOR_NM'
    const regionCode = feature.getProperty('code'); // 예: 'code' 또는 'SIG_CD'

    if (currentPolygon.value && currentPolygon.value !== feature) {
       map.value.data.revertStyle(currentPolygon.value);
    }
    map.value.data.overrideStyle(feature, {
      fillColor: 'yellow', fillOpacity: 0.7, strokeWeight: 2, strokeColor: '#000000',
    });
    // 현재 마우스 올린 지역 스타일 변경 (강조)
      map.value.data.overrideStyle(feature, {
        fillColor: 'yellow',
        fillOpacity: 0.7,
        strokeWeight: 2,
        strokeColor: '#000000',
      });
      currentPolygon.value = feature; // 현재 강조된 폴리곤 참조 저장
    console.log(`Mouseover: Name=${regionName}, Code=${regionCode}`);
    // 부모 컴포넌트(App.vue)로 호버 정보 전달 (emit 사용)
    emit('region-hover', { name: regionName, code: regionCode });
  });

  // 3. Mouseout
  map.value.data.addListener('mouseout', () => { // 파라미터 제거
    if (currentPolygon.value) {
       map.value.data.revertStyle(currentPolygon.value);
       currentPolygon.value = null;
       // 부모 컴포넌트로 마우스 아웃 알림
       emit('region-mouseout');
    }
  });

  // 4. Click
  map.value.data.addListener('click', (e) => {
    const feature = e.feature;
    const regionCode = feature.getProperty('code'); // ★★★ 실제 속성 이름 확인! ★★★
    const regionName = feature.getProperty('name'); // ★★★ 실제 속성 이름 확인! ★★★
    console.log(`Region Clicked: Code=${regionCode}, Name=${regionName}`);
    alert(`클릭: ${regionName} (코드: ${regionCode})`);
    // 부모 컴포넌트로 클릭 정보 전달
    emit('region-click', { name: regionName, code: regionCode });
  });

  console.log("MapView: Data layer styles and event listeners set.");
};

// App.vue에서 map 객체 접근 위해 노출
defineExpose({ map });
console.log("MapView: script setup 완료, defineExpose 호출됨");
</script>

<style scoped>
#map-container {
  border: 1px solid #ccc; /* 임시 테두리 */
}
</style>