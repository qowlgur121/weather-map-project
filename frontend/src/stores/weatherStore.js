// // src/stores/weatherStore.js
// import { defineStore } from 'pinia';
// import axios from 'axios'; // Axios 임포트

// export const useWeatherStore = defineStore('weather', {
//   state: () => ({
//     selectedWeatherType: 'TMP', // 기본 선택: 현재 기온
//     weatherData: {}, // 지역 코드별 날씨 데이터 저장 (예: { "11110": { value: "25", unit: "℃" }, ... })
//     isLoading: false, // 로딩 상태
//     error: null,      // 오류 상태
//   }),
//   actions: {
//     // 날씨 타입 변경 액션
//     async selectWeatherType(weatherType) {
//       console.log('WeatherStore: selectWeatherType', weatherType);
//       this.selectedWeatherType = weatherType;
//       // 날씨 타입이 변경되면 자동으로 데이터 다시 로드
//       // TODO: 현재 보이는 지역 코드 목록(visibleRegionCodes)을 인자로 받아야 함
//       // await this.fetchWeatherData(visibleRegionCodes);
//     },
//     // 백엔드 API 호출하여 날씨 데이터 가져오는 액션
//     async fetchWeatherData(regions) { // regions: [{ code, lat, lon }, ...] 형태
//       if (!regions || regions.length === 0) {
//          console.log('WeatherStore: No regions provided to fetch data.');
//          this.weatherData = {}; // 지역 목록 없으면 데이터 초기화
//          return;
//       }
//       if (!this.selectedWeatherType) {
//          console.error('WeatherStore: No weather type selected.');
//          return;
//       }

//       console.log(`WeatherStore: Fetching weather data for ${regions.length} regions, type: ${this.selectedWeatherType}`);
//       this.isLoading = true;
//       this.error = null;
//       this.weatherData = {}; // 새 데이터 로드 전 초기화

//       try {
//         // 백엔드 API 호출 (POST 방식)
//         const response = await axios.post('/api/weather/regions', regions, {
//           params: { type: this.selectedWeatherType } // 쿼리 파라미터로 type 전달
//         });

//         console.log('WeatherStore: API Response received', response.data);

//         // 받아온 데이터를 state에 저장하기 쉬운 형태로 변환 (Map 형태)
//         const weatherDataMap = {};
//         if (Array.isArray(response.data)) {
//            response.data.forEach(item => {
//                // 오류가 아닌 경우에만 저장
//                if (item.value !== 'Error' && item.value !== 'N/A') {
//                    weatherDataMap[item.regionCode] = {
//                        value: item.value,
//                        unit: item.unit,
//                    };
//                } else {
//                    console.warn(`WeatherStore: No valid data or error for region ${item.regionCode}`);
//                }
//            });
//         }
//         this.weatherData = weatherDataMap;

//       } catch (err) {
//         console.error('WeatherStore: Error fetching weather data', err);
//         this.error = err.message || '날씨 정보를 가져오는 데 실패했습니다.';
//         this.weatherData = {}; // 오류 발생 시 데이터 초기화
//       } finally {
//         this.isLoading = false;
//       }
//     }
//   }
// });



//////////////////////////////

// src/stores/weatherStore.js 날씨 관련 상태를 관리할 스토어 파일을 만들자.

// 1. Pinia 라이브러리에서 스토어 정의 함수(defineStore)를 가져온다.
//    'import'는 다른 파일에 있는 기능을 가져와서 쓰겠다는 뜻이다.
//    '{ defineStore }'는 pinia 라이브러리 안에 있는 여러 기능 중 defineStore만 쏙 뽑아오겠다는 의미다.
import { defineStore } from 'pinia';
import axios from 'axios'; // Axios 임포트

// 2. 스토어를 정의하고 외부에서 사용할 수 있도록 내보낸다(export).
//    'useWeatherStore'는 다른 컴포넌트에서 이 스토어를 사용하기 위해 호출할 함수의 이름이다. (관례적으로 'use'로 시작하고 스토어 이름 + 'Store'를 붙인다)
//    defineStore 함수의 첫 번째 인자('weather')는 이 스토어의 고유한 ID(이름표)다. 나중에 개발자 도구에서 구별하기 위해 쓴다.
//    두 번째 인자는 객체({})인데, 이 안에 state, getters, actions를 정의한다.
export const useWeatherStore = defineStore('weather', {
  // 여기에 state, getters, actions를 채워 넣을 거다!
  // 어떤 정보들을 기억하고 있어야 하는지 (state)
  // 기억한 정보를 바탕으로 뭘 계산해서 알려줄 수 있는지 (getters)
  // 어떤 구체적인 행동(정보 업데이트 등)을 할 수 있는지 (actions)
  // state는 함수 형태로 작성하고, 그 함수가 상태 객체를 반환하도록 해야 한다.
  state: () => {
    return {
      selectedWeatherType: null,
      weatherData: {}, 
      isLoading: false,
      error: null,
    };
  }, 

  // getters는 객체 형태로 정의한다.
  getters: {
    // 예시: 특정 지역 코드의 날씨 데이터를 가져오는 getter
    // getWeatherDataByCode 라는 이름의 getter 함수 정의
    // 이 함수는 state를 첫 번째 인자로 받는다.
    // 그리고 지역 코드(regionCode)를 인자로 받는 또 다른 함수를 반환한다. (고차 함수 방식)
    getWeatherDataByCode: (state) => {
      return (regionCode) => {
        // state.weatherData 객체에서 regionCode 키에 해당하는 값을 찾아서 반환한다. 없으면 null.
        return state.weatherData[regionCode] || null;
      };
    },

    // 예시: 날씨 데이터가 있는지 확인하는 getter
    // hasWeatherData 라는 이름의 getter 함수 정의
    hasWeatherData: (state) => {
      // state.weatherData 객체의 키(key)들의 개수를 센다.
      // 개수가 0보다 크면 데이터가 있다는 뜻이므로 true 반환.
      return Object.keys(state.weatherData).length > 0;
    },

    // 예시: 선택된 날씨 타입 코드를 한국어 이름으로 바꿔주는 getter
    selectedWeatherTypeName: (state) => {
      // state.selectedWeatherType 값에 따라 다른 문자열 반환 (switch 문 사용)
      switch (state.selectedWeatherType) {
        case 'TMP': return '현재기온';
        case 'REH': return '습도';
        case 'POP': return '강수확률';
        case 'WSD': return '풍속';
        // 필요하면 다른 타입 추가...
        default: return '날씨 정보'; // 선택된 타입이 없거나 모르는 타입일 때
      }
    }
  }, // getters 정의 끝 (쉼표 주의!)

  // actions는 객체 형태로 정의한다.
  // actions: 직원이 할 수 있는 행동들
  actions: {
    // 사용자가 날씨 종류를 선택하면 그걸 기억하는 행동
    setSelectedWeatherType(weatherType) {
      console.log('WeatherStore: setSelectedWeatherType', weatherType);
      this.selectedWeatherType = weatherType;
      // 참고: 날씨 타입 변경 시 데이터 자동 재로드는 App.vue에서 visibleRegions 변경 감지를 통해 처리될 수 있음
    },

    // ★★★ 서버에서 날씨 정보 가져오는 행동 ★★★
    async fetchWeatherData(regions) { // 'async'는 서버 응답을 기다려야 한다는 표시
                                      // 'regions'는 [{ code, lat, lon }, ...] 형태의 지역 목록

      // 1. 준비물 확인: 날씨 종류가 선택되었고, 지역 목록이 비어있지 않은지 확인
      if (!this.selectedWeatherType || !regions || regions.length === 0) {
        console.warn('WeatherStore: 아직 준비 안됨! (날씨 종류나 지역 목록 없음)');
        this.weatherData = {}; // 혹시 모르니 이전 날씨 정보는 지움
        return; // 여기서 작업 중단
      }

      // 2. 작업 시작 알림 및 준비
      console.log(`WeatherStore: 서버에 날씨 정보 물어보러 출발! (${regions.length}개 지역, 종류: ${this.selectedWeatherType})`);
      this.isLoading = true; // "지금 작업 중" 상태로 변경 (로딩 스피너 보여줄 수 있음)
      this.error = null;     // 이전에 에러가 있었으면 일단 지움

      // 3. 서버와 대화 시도 (try: 시도해보다)
      try {
        // ★★★ 진짜 서버에게 물어보는 부분 ★★★
        const response = await axios.post( // 'await'는 서버 응답 올 때까지 잠시 기다리라는 뜻
          // 요청 주소: 상대 경로 사용 (API_BASE_URL 제거)
          '/api/weather/regions',
          // 요청 내용(본문): 서버에게 줄 정보 (어떤 지역들인지 알려줌)
          regions,
          // 추가 정보 (쿼리 파라미터): 어떤 종류의 날씨인지 주소 뒤에 붙여서 알려줌 (?type=TMP)
          { params: { type: this.selectedWeatherType } }
        );
        // ★★★ 서버 응답 도착! ★★★

        // 4. 서버 응답 확인 및 정리
        console.log('WeatherStore: 서버가 대답함!', response.data);
        const newWeatherData = {}; // 서버 응답을 정리해서 담을 새 그릇 준비

        // 서버가 준 데이터(response.data)가 우리가 예상한 목록(배열) 형태인지 확인
        if (response.data && Array.isArray(response.data)) {
          // 목록에 있는 각 날씨 정보(item)를 꺼내서 정리
          response.data.forEach(item => {
            // 지역 코드(예: '11000')를 이름표로 해서 값과 단위를 저장
            newWeatherData[item.regionCode] = {
              value: item.value, // 실제 날씨 값 (예: 25)
              unit: item.unit,   // 단위 (예: 'C')
            };
          });
          // ★ 정리된 최신 날씨 정보를 직원의 메모장(state)에 업데이트! ★
          this.weatherData = newWeatherData;
          console.log(`WeatherStore: 날씨 정보 저장 완료! (${Object.keys(this.weatherData).length}개 지역)`);
        } else {
          // 서버가 이상한 형태로 응답하면 에러 처리
          console.warn("WeatherStore: 서버 응답 형식이 이상함.", response.data);
          this.error = '서버 응답 형식 오류';
        }

      // 5. 서버와 대화 중 문제 발생 시 (catch: 잡다)
      } catch (err) {
        console.error('WeatherStore: 서버와 통신 중 에러 발생!', err);
        // 에러 메시지를 직원의 메모장(state)에 기록
        this.error = err.response?.data?.message || err.message || '날씨 정보 로딩 실패';
        // 에러 났으니 날씨 정보는 비워둠
        this.weatherData = {};
      } finally {
        // 6. 작업 완료 (finally: 마지막에 항상 실행)
        this.isLoading = false; // "작업 끝" 상태로 변경 (로딩 스피너 숨김)
        console.log('WeatherStore: 날씨 정보 요청 처리 종료.');
      }
    }, // fetchWeatherData 액션 끝
  }, // actions 끝
})
