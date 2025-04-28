// src/stores/mapStore.js 지도 관련 상태를 관리할 스토어

import { defineStore } from 'pinia';

export const useMapStore = defineStore('map', {
    // 1. state: 이 "지도 담당 직원"이 기억해야 할 정보들
  state: () => ({ // 새 직원에게 새 메모장을!

    // visibleRegions: 현재 지도 화면에 보이는 지역들의 정보 목록을 담을 배열.
    // 예: [{code: '11000', lat: 37.5, lon: 127.0}, {code: '21000', lat: 35.1, lon: 129.0}, ...]
    // 처음엔 아무것도 안 보이니 빈 배열([])로 시작.
    visibleRegions: [],

    // hoveredRegionInfo: 사용자가 마우스를 올려놓은 지역의 정보를 담을 곳.
    // 예: {code: '11000', name: '서울'}
    // 처음엔 아무 곳에도 마우스를 안 올렸으니 비워둠 (null).
    hoveredRegionInfo: null,
  }), // state 정의 끝 (쉼표!)

  // 2. actions: 이 "지도 담당 직원"이 수행할 수 있는 작업들
  actions: {

    // setVisibleRegions(regionsData): "화면에 보이는 지역 목록 업데이트 해!" 라는 작업.
    // 외부(지도 컴포넌트 등)에서 새로운 지역 목록(regionsData)을 주면,
    // 그걸 그대로 직원의 메모장(state)에 있는 visibleRegions 칸에 덮어쓴다.
    setVisibleRegions(regionsData) {
      // 주석 설명: 만약 regionsData가 이전 값과 완전히 똑같으면 업데이트 안 하고 싶을 수도 있는데,
      // 지금 코드는 그냥 무조건 새로 받은 regionsData로 덮어쓰는 방식이다. (이것도 괜찮음!)
      this.visibleRegions = regionsData;
      // 콘솔에 로그를 남겨서 몇 개의 지역이 업데이트되었는지 확인 (개발할 때 유용!)
      console.log(`MapStore: 보이는 지역 업데이트 (${this.visibleRegions.length}개)`);
    },

    // setHoveredRegionInfo(info): "마우스 올린 지역 정보 업데이트 해!" 라는 작업.
    // 외부에서 마우스 올린 지역의 정보(info)를 주면,
    // 그걸 메모장의 hoveredRegionInfo 칸에 기록한다.
    setHoveredRegionInfo(info) {
      this.hoveredRegionInfo = info;
    },

    // clearHoveredRegionInfo(): "마우스 올린 지역 정보 지워!" 라는 작업.
    // 외부에서 마우스가 지역 밖으로 나갔다고 알려주면,
    // 메모장의 hoveredRegionInfo 칸을 다시 비운다 (null로 만든다).
    clearHoveredRegionInfo() {
      this.hoveredRegionInfo = null;
    },
  }, // actions 정의 끝
});