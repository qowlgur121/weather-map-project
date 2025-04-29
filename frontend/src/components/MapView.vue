<!-- MapView.vue -->
<template>
  <div id="map-container" style="width: 100%; height: 100%;"></div>
</template>

<script setup>
/* global naver */
import { onMounted, ref, defineExpose, watch } from 'vue';
import { useMapStore } from '@/stores/mapStore'; // Use alias path
import { useWeatherStore } from '@/stores/weatherStore'; // Use alias path

const map = ref(null);
// const geoJsonData = ref(null); // 스토어 사용
const mapStore = useMapStore();
const weatherStore = useWeatherStore();
const currentPolygon = ref(null); // ★ 현재 마우스 오버된 폴리곤 추적 ★

// ★ 지도 위에 그려진 Naver Polygon 객체들을 저장할 배열 ★
const mapPolygons = ref([]); // [{ polygon: naver.maps.Polygon, code: string, name: string, center: {lat, lon} }, ...]

onMounted(() => {
  if (window.naver && window.naver.maps) {
    initMap();
  } else {
    // Naver Maps API 스크립트 로드를 기다리는 로직 추가 가능
    const interval = setInterval(() => {
      if (window.naver && window.naver.maps) {
        clearInterval(interval);
        initMap();
      }
    }, 100);
    // 일정 시간 후에도 로드되지 않으면 에러 처리
    setTimeout(() => {
        if (!map.value) {
            clearInterval(interval);
            console.error('MapView: Naver Maps API 스크립트 로드 시간 초과!');
        }
    }, 10000); // 10초 타임아웃
  }
});

const initMap = async () => {
  try {
    const mapContainer = document.getElementById('map-container');
    if (!mapContainer) {
        console.error('MapView: Map container not found!');
        return;
    }
    const mapOptions = {
        center: new window.naver.maps.LatLng(36.5, 127.5), // 초기 중심 좌표
        zoom: 7, // 초기 줌 레벨
        mapTypeControl: true,
        mapTypeControlOptions: {
            style: naver.maps.MapTypeControlStyle.BUTTON,
            position: naver.maps.Position.TOP_RIGHT
        },
        zoomControl: true,
        zoomControlOptions: {
            position: naver.maps.Position.RIGHT_CENTER,
            style: naver.maps.ZoomControlStyle.SMALL
        },
        scaleControl: false,
        logoControl: false,
        mapDataControl: false, // 데이터 레이어 컨트롤 비활성화
        minZoom: 6 // 최소 줌 레벨
    };
    map.value = new window.naver.maps.Map(mapContainer, mapOptions);
    console.log('MapView: Naver Map 객체 생성 완료.');

    // ★ 지도 초기화 및 이벤트 리스너 등록 ★
    // 지도 타일 로드가 완료되면 GeoJSON 로드 및 그리기 시작
    naver.maps.Event.addListener(map.value, 'tilesloaded', async () => {
        console.log("MapView: Map tiles loaded event received");
        if (mapPolygons.value.length === 0) { // 폴리곤이 아직 로드되지 않았을 경우에만 실행
            await loadAndDrawGeoJson(); // ★ GeoJSON 로드 및 Polygon 그리기 함수 호출

            // idle 이벤트 리스너 등록: 지도 멈추면 지연 후 업데이트 시도
            if (!map.value.hasListener('idle')) {
                 naver.maps.Event.addListener(map.value, 'idle', () => {
                    setTimeout(updateVisibleRegions, 250); // 250ms 지연
                 });
            }
        }
    });

  } catch (error) {
      console.error('MapView: 지도 초기화 중 오류 발생', error);
  }
};

// ★★★ GeoJSON 로드 및 Naver Polygon 객체 생성/그리기 함수 ★★★
const loadAndDrawGeoJson = async () => {
  try {
    console.log("MapView: Loading GeoJSON...");
    const response = await fetch('/korea_sig.geojson'); // public 폴더 기준 경로
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    const geoJson = await response.json();
    mapStore.setGeoJsonData(geoJson); // 스토어에도 저장 (선택적)
    console.log("MapView: GeoJSON loaded successfully.");

    if (!map.value || !geoJson.features) {
        console.error("MapView: Map object or GeoJSON features not available.");
        return;
    }

    const polygons = []; // 생성된 Polygon 정보 임시 저장
    mapPolygons.value = []; // 기존 폴리곤 초기화 (중복 방지)

    // 각 Feature를 순회하며 Naver Polygon 객체 생성
    for (const feature of geoJson.features) {
      // 올바른 속성 이름('code')으로 조건 확인
      if (!feature.geometry || !feature.properties || !feature.properties.code) {
          console.warn("MapView: Skipping feature due to missing geometry, properties, or code:", feature.properties);
          continue;
      }

      // 올바른 속성 이름('code', 'name') 사용
      const code = feature.properties.code;
      const name = feature.properties.name;
      const geometry = feature.geometry;

      // Polygon 또는 MultiPolygon 처리
      let pathsArray = []; // MultiPolygon을 위해 경로 배열의 배열로 처리
      if (geometry.type === 'Polygon') {
        // GeoJSON 좌표(경도, 위도) -> Naver LatLng(위도, 경도) 변환
        const singlePath = geometry.coordinates[0].map(coord => {
            if (coord.length >= 2 && typeof coord[0] === 'number' && typeof coord[1] === 'number') {
                return new naver.maps.LatLng(coord[1], coord[0]);
            } else {
                console.warn(`Invalid coordinate format in Polygon for ${name}:`, coord);
                return null; // 잘못된 좌표는 null 반환
            }
        }).filter(p => p !== null); // null 제거
        if (singlePath.length > 2) { // 유효한 폴리곤 경로인지 확인 (최소 3개 점)
            pathsArray.push(singlePath);
        }
      } else if (geometry.type === 'MultiPolygon') {
        geometry.coordinates.forEach(polygonCoords => {
            if (polygonCoords.length > 0) {
                const multiPath = polygonCoords[0].map(coord => {
                     if (coord.length >= 2 && typeof coord[0] === 'number' && typeof coord[1] === 'number') {
                        return new naver.maps.LatLng(coord[1], coord[0]);
                    } else {
                        console.warn(`Invalid coordinate format in MultiPolygon for ${name}:`, coord);
                        return null;
                    }
                }).filter(p => p !== null);
                if (multiPath.length > 2) {
                    pathsArray.push(multiPath);
                }
            }
        });
      }

      if (pathsArray.length > 0) {
        // Naver Polygon 객체 생성
        const polygon = new naver.maps.Polygon({
          map: map.value, // 지도에 바로 추가
          paths: pathsArray, // 경로 배열 (외부 링 + 내부 링들)
          fillColor: '#ffffff',
          fillOpacity: 0.1, // 기본 투명도 낮게
          strokeColor: '#666666',
          strokeWeight: 1,
          clickable: true,
          // ★ 각 폴리곤에 식별 정보 저장 (나중에 참조하기 위해) ★
          custom_properties: { // 사용자 정의 속성
              code: code,
              name: name
          }
        });

        // 폴리곤 이벤트 리스너 추가
        addPolygonEventListeners(polygon);

        // 중심 좌표 계산 (나중에 visibleRegions 계산용) - 폴리곤의 bounds 사용
        let center = null;
        try {
            if (typeof polygon.getBounds === 'function') {
               const bounds = polygon.getBounds();
               if (bounds && typeof bounds.getCenter === 'function') {
                   const centerLatLng = bounds.getCenter();
                   center = { lat: centerLatLng.lat(), lon: centerLatLng.lng() };
               } else {
                   console.warn(`MapView: Could not get center for polygon ${name} (bounds: ${bounds})`);
               }
            } else {
                console.warn(`MapView: getBounds function not available for polygon ${name}`);
            }
        } catch (boundsError) {
            console.error(`MapView: Error getting bounds/center for polygon ${name}`, boundsError);
        }

        // 관리 목록에 추가
        polygons.push({ polygon: polygon, code: code, name: name, center: center });
      } else {
          // console.warn(`MapView: No valid paths generated for feature ${name} (Code: ${code})`);
      }
    } // for 루프 끝

    mapPolygons.value = polygons; // ref 변수에 저장
    console.log(`MapView: ${mapPolygons.value.length}개의 Naver Polygon 생성 및 추가 완료.`);
    // 초기 스타일 적용
    updatePolygonStyles();

  } catch (error) {
    console.error("MapView: GeoJSON 로드 및 Polygon 생성 실패", error);
  }
};

// ★★★ 각 Naver Polygon 객체에 이벤트 리스너 추가 함수 ★★★
const addPolygonEventListeners = (polygon) => {
  if (!polygon || !polygon.custom_properties) {
      console.warn("MapView: Invalid polygon object passed to addPolygonEventListeners");
      return;
  }
  const props = polygon.custom_properties; // 생성 시 저장한 정보 가져오기

  // eslint-disable-next-line no-unused-vars
  naver.maps.Event.addListener(polygon, 'mouseover', (_e) => {
      // console.log(`Mouseover: Name=${props.name}, Code=${props.code}`); // 로그 줄임
      if (currentPolygon.value && currentPolygon.value !== polygon) {
         // 이전 폴리곤 스타일 되돌리기 (현재 날씨 데이터 기반)
         const prevProps = currentPolygon.value.custom_properties;
         currentPolygon.value.setOptions({
             fillColor: getFillColor(prevProps.code),
             fillOpacity: getFillOpacity(prevProps.code),
             strokeWeight: 1,
             strokeColor: '#666666',
             zIndex: 0
         });
      }
      // 현재 폴리곤 강조
      polygon.setOptions({
          fillColor: 'yellow', // 강조 색상
          fillOpacity: 0.7,
          strokeWeight: 2,
          strokeColor: '#000000',
          zIndex: 1 // 강조 시 위로 올라오도록
      });
      currentPolygon.value = polygon;
      mapStore.setHoveredRegionInfo({ name: props.name, code: props.code }); // 스토어 업데이트
  });

  // eslint-disable-next-line no-unused-vars
  naver.maps.Event.addListener(polygon, 'mouseout', (_e) => {
      // console.log(`Mouseout: Name=${props.name}, Code=${props.code}`); // 로그 줄임
      if (currentPolygon.value === polygon) { // 현재 강조된 폴리곤이 맞는지 확인
          // 스타일 되돌리기 (현재 날씨 데이터 기반)
          polygon.setOptions({
             fillColor: getFillColor(props.code),
             fillOpacity: getFillOpacity(props.code),
             strokeWeight: 1,
             strokeColor: '#666666',
             zIndex: 0
          });
          currentPolygon.value = null;
          mapStore.clearHoveredRegionInfo();
      } else if (currentPolygon.value !== null) {
          // 마우스가 다른 폴리곤으로 바로 이동한 경우, 이전 폴리곤 스타일 복원 로직이 mouseover에서 처리됨
          // 이 경우는 현재 폴리곤이 아닌 다른 폴리곤에서 mouseout이 발생한 경우이므로 무시하거나,
          // 혹은 모든 폴리곤의 스타일을 다시 적용해야 할 수도 있음 (상황에 따라)
          // 여기서는 현재 강조된 폴리곤이 아닌 경우 스타일 변경 안 함
      } else {
          // currentPolygon.value가 null인 경우 (강조된 폴리곤이 없는 상태에서 mouseout)
          // 이 폴리곤의 스타일을 기본/데이터 기반으로 설정
           polygon.setOptions({
             fillColor: getFillColor(props.code),
             fillOpacity: getFillOpacity(props.code),
             strokeWeight: 1,
             strokeColor: '#666666',
             zIndex: 0
          });
      }
  });

  // eslint-disable-next-line no-unused-vars
  naver.maps.Event.addListener(polygon, 'click', (_e) => {
      console.log(`Region Clicked: Code=${props.code}, Name=${props.name}`);
      alert(`클릭: ${props.name} (코드: ${props.code})`);
      // 필요시 부모로 emit 또는 다른 작업 수행
      // 예: weatherStore.fetchDetailedWeather(props.code);
  });
};

// ★★★ 보이는 지역 계산 함수 (Naver Polygon 객체 기반) ★★★
const updateVisibleRegions = () => {
  if (!map.value || mapPolygons.value.length === 0) {
    // console.log("MapView: 업데이트 건너뜀 (지도 또는 폴리곤 준비 안됨)");
    mapStore.setVisibleRegions([]);
    return;
  }
  // console.log("MapView: 보이는 지역 정보 업데이트 시작...");
  const bounds = map.value.getBounds(); // ★ 직접 getBounds 호출 ★

  // ★★★ bounds 객체 및 contains 메소드 유효성 검사 강화 ★★★
  if (!bounds || typeof bounds.contains !== 'function') {
      console.warn("MapView: Map bounds not available or invalid for updateVisibleRegions.", bounds);
      mapStore.setVisibleRegions([]);
      return;
  }
  // ★★★ 검사 끝 ★★★

  const visibleRegionsData = [];
  // let containedCount = 0; // Removed as it's unused

  for (const item of mapPolygons.value) {
      // 중심 좌표가 유효하고, 지도 경계 내에 있는지 확인
      if (item.center && typeof item.center.lat === 'number' && typeof item.center.lon === 'number') {
          const latLng = new naver.maps.LatLng(item.center.lat, item.center.lon);
          // 유효성 검사를 통과했으므로 안전하게 contains 호출
          if (bounds.contains(latLng)) {
              // containedCount++; // Removed as it's unused
              visibleRegionsData.push({ code: item.code, lat: item.center.lat, lon: item.center.lon });
          }
      } else {
          // 중심 좌표가 없는 경우, 폴리곤 전체가 보이는지 확인 (더 복잡하고 성능 저하 가능성 있음)
          // 예: if (bounds.intersects(item.polygon.getBounds())) { ... }
          // 여기서는 중심점 기준으로만 판단
          // console.warn(`MapView: Missing or invalid center for region ${item.name} (${item.code})`);
      }
  }

  // console.log(`MapView: 보이는 지역 정보 업데이트 완료. 화면 내 ${containedCount}개 포함.`);
  mapStore.setVisibleRegions(visibleRegionsData); // 스토어 업데이트
};


// ★★★ 날씨 데이터 변경 시 폴리곤 스타일 업데이트 (Naver Polygon 객체 기반) ★★★
// eslint-disable-next-line no-unused-vars
watch(() => weatherStore.weatherData, (_newWeatherData, _oldWeatherData) => {
  console.log("MapView: 날씨 데이터 변경 감지, 지도 스타일 업데이트 시작...");
  updatePolygonStyles(); // 스타일 업데이트 함수 호출
}, { deep: true });

// ★★★ 선택된 날씨 타입 변경 시 폴리곤 스타일 업데이트 ★★★
watch(() => weatherStore.selectedWeatherType, (newType, oldType) => {
    console.log(`MapView: 선택된 날씨 타입 변경 (${oldType} -> ${newType}), 지도 스타일 업데이트 시작...`);
    updatePolygonStyles(); // 스타일 업데이트 함수 호출
});


// ★★★ 폴리곤 스타일 업데이트 함수 (공통 로직) ★★★
const updatePolygonStyles = () => {
    if (mapPolygons.value.length === 0) {
        // console.log("MapView: 폴리곤이 없어 스타일 업데이트 건너뜀.");
        return; // 폴리곤 없으면 실행 안 함
    }
    // console.log("MapView: 폴리곤 스타일 업데이트 중...");

    // let updatedCount = 0; // Removed as it's unused
    mapPolygons.value.forEach(item => {
        const regionCode = item.code;
        const polygon = item.polygon;

        // 현재 마우스오버된 폴리곤은 스타일 변경하지 않음 (강조 유지)
        if (currentPolygon.value === polygon) {
            return;
        }

        const fillColor = getFillColor(regionCode); // ★ 헬퍼 함수 사용
        const fillOpacity = getFillOpacity(regionCode); // ★ 헬퍼 함수 사용

        // 현재 폴리곤의 옵션과 비교하여 변경이 필요할 때만 setOptions 호출 (성능 최적화)
        const currentOptions = polygon.getOptions();
        if (currentOptions.fillColor !== fillColor || currentOptions.fillOpacity !== fillOpacity) {
            polygon.setOptions({
                fillColor: fillColor,
                fillOpacity: fillOpacity,
                strokeWeight: 1, // mouseout 등에서 변경되었을 수 있으므로 기본값 설정
                strokeColor: '#666666',
                zIndex: 0
            });
            // updatedCount++; // Removed as it's unused
        }
    });
    // console.log(`MapView: ${updatedCount}개 폴리곤 스타일 업데이트 완료.`);
};


// ★★★ 헬퍼 함수: 특정 지역 코드의 날씨 값으로 색상 계산 ★★★
const getFillColor = (regionCode) => {
    const data = weatherStore.weatherData[regionCode];
    let color = '#ffffff'; // 기본 흰색 (데이터 없음)
    let value = null;

    if (data && data.value !== undefined && data.value !== null && data.value !== 'Error' && data.value !== 'N/A') {
        value = parseFloat(data.value);
    } else if (data && data.value === 'Error') {
        return '#BDBDBD'; // 회색 (오류)
    } else {
        return color; // 흰색 (데이터 없음)
    }

    if (isNaN(value)) {
        return '#EAEAEA'; // 연한 회색 (값 변환 실패)
    }

    const weatherType = weatherStore.selectedWeatherType;

    try {
        if (weatherType === 'TMP') { // 기온
            // -10도(파랑) ~ 40도(빨강) 범위로 색상 매핑
            const temp = Math.max(-10, Math.min(40, value));
            const ratio = (temp + 10) / 50; // 0 ~ 1 사이 값
            const red = Math.round(255 * ratio);
            const blue = Math.round(255 * (1 - ratio));
            color = `rgb(${red}, 80, ${blue})`; // 초록색은 약하게 고정
        } else if (weatherType === 'POP') { // 강수확률
            // 0%(흰색) ~ 100%(진한 파랑)
            const pop = Math.max(0, Math.min(100, value));
            const intensity = pop / 100; // 0 ~ 1
            // 흰색(255, 255, 255) -> 파랑(0, 0, 255)
            const red = 255 * (1 - intensity);
            const green = 255 * (1 - intensity);
            const blue = 255;
            color = `rgb(${Math.round(red)}, ${Math.round(green)}, ${blue})`;
        } else if (weatherType === 'REH') { // 습도
            // 0%(연두) ~ 100%(진녹)
            const reh = Math.max(0, Math.min(100, value));
            const intensity = reh / 100; // 0 ~ 1
            // 연두(150, 255, 150) -> 진녹(0, 100, 0)
            const red = 150 * (1 - intensity);
            const green = 255 - 155 * intensity;
            const blue = 150 * (1 - intensity);
            color = `rgb(${Math.round(red)}, ${Math.round(green)}, ${Math.round(blue)})`;
        } else if (weatherType === 'WSD') { // 풍속
            // 0m/s(하늘) ~ 20m/s(보라)
            const wsd = Math.max(0, Math.min(20, value));
            const ratio = wsd / 20; // 0 ~ 1
            // 하늘(135, 206, 250) -> 보라(128, 0, 128)
            const red = 135 + (128 - 135) * ratio;
            const green = 206 + (0 - 206) * ratio;
            const blue = 250 + (128 - 250) * ratio;
            color = `rgb(${Math.round(red)}, ${Math.round(green)}, ${Math.round(blue)})`;
        }
        // ... 다른 날씨 타입에 대한 색상 로직 추가 ...
        else {
            color = '#cccccc'; // 기타 타입은 회색
        }
    } catch (e) {
        console.error(`Error calculating color for ${weatherType}, value ${value}:`, e);
        color = '#EAEAEA'; // 계산 오류 시 연한 회색
    }

    return color;
};

// ★★★ 헬퍼 함수: 특정 지역 코드의 날씨 값으로 투명도 계산 ★★★
const getFillOpacity = (regionCode) => {
    const data = weatherStore.weatherData[regionCode];
    if (data && data.value !== undefined && data.value !== null && data.value !== 'Error' && data.value !== 'N/A') {
        return 0.6; // 데이터 있으면 0.6 (기존 0.7보다 약간 낮춤)
    } else if (data && data.value === 'Error') {
        return 0.4; // 오류 시 0.4 (기존 0.5보다 약간 낮춤)
    }
    return 0.1; // 데이터 없으면 0.1 (거의 투명)
};

// --- 지도 제어 함수들 (이전과 유사) ---
const moveToLocation = (lat, lng) => {
  if (map.value) {
    map.value.panTo(new window.naver.maps.LatLng(lat, lng));
  }
};

const zoomInMap = () => {
  if (map.value) {
    map.value.setZoom(map.value.getZoom() + 1);
  }
};

const zoomOutMap = () => {
  if (map.value) {
    map.value.setZoom(map.value.getZoom() - 1);
  }
};

// 메소드 노출
defineExpose({ moveToLocation, zoomInMap, zoomOutMap });
</script>

<style scoped>
#map-container {
  width: 100%;
  height: 100%;
  /* 필요한 경우 추가 스타일 */
}
</style>
