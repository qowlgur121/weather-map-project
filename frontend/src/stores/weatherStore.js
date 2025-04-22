// src/stores/weatherStore.js
import { defineStore } from 'pinia';
import axios from 'axios'; // Axios 임포트

export const useWeatherStore = defineStore('weather', {
  state: () => ({
    selectedWeatherType: 'TMP', // 기본 선택: 현재 기온
    weatherData: {}, // 지역 코드별 날씨 데이터 저장 (예: { "11110": { value: "25", unit: "℃" }, ... })
    isLoading: false, // 로딩 상태
    error: null,      // 오류 상태
  }),
  actions: {
    // 날씨 타입 변경 액션
    async selectWeatherType(weatherType) {
      console.log('WeatherStore: selectWeatherType', weatherType);
      this.selectedWeatherType = weatherType;
      // 날씨 타입이 변경되면 자동으로 데이터 다시 로드
      // TODO: 현재 보이는 지역 코드 목록(visibleRegionCodes)을 인자로 받아야 함
      // await this.fetchWeatherData(visibleRegionCodes);
    },
    // 백엔드 API 호출하여 날씨 데이터 가져오는 액션
    async fetchWeatherData(regions) { // regions: [{ code, lat, lon }, ...] 형태
      if (!regions || regions.length === 0) {
         console.log('WeatherStore: No regions provided to fetch data.');
         this.weatherData = {}; // 지역 목록 없으면 데이터 초기화
         return;
      }
      if (!this.selectedWeatherType) {
         console.error('WeatherStore: No weather type selected.');
         return;
      }

      console.log(`WeatherStore: Fetching weather data for ${regions.length} regions, type: ${this.selectedWeatherType}`);
      this.isLoading = true;
      this.error = null;
      this.weatherData = {}; // 새 데이터 로드 전 초기화

      try {
        // 백엔드 API 호출 (POST 방식)
        const response = await axios.post('/api/weather/regions', regions, {
          params: { type: this.selectedWeatherType } // 쿼리 파라미터로 type 전달
        });

        console.log('WeatherStore: API Response received', response.data);

        // 받아온 데이터를 state에 저장하기 쉬운 형태로 변환 (Map 형태)
        const weatherDataMap = {};
        if (Array.isArray(response.data)) {
           response.data.forEach(item => {
               // 오류가 아닌 경우에만 저장
               if (item.value !== 'Error' && item.value !== 'N/A') {
                   weatherDataMap[item.regionCode] = {
                       value: item.value,
                       unit: item.unit,
                   };
               } else {
                   console.warn(`WeatherStore: No valid data or error for region ${item.regionCode}`);
               }
           });
        }
        this.weatherData = weatherDataMap;

      } catch (err) {
        console.error('WeatherStore: Error fetching weather data', err);
        this.error = err.message || '날씨 정보를 가져오는 데 실패했습니다.';
        this.weatherData = {}; // 오류 발생 시 데이터 초기화
      } finally {
        this.isLoading = false;
      }
    }
  }
});