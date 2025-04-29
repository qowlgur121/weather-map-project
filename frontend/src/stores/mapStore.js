// src/stores/mapStore.js 지도 관련 상태를 관리할 스토어

import { defineStore } from 'pinia';

export const useMapStore = defineStore('map', {

    // 1. state: 이 "지도 담당 직원"이 기억해야 할 정보들
    state: () => ({
        // ★ visibleRegions 상태 구조 변경: 문자열 배열 -> 객체 배열 ★
        /** @type {Array<{code: string, lat: number, lon: number}>} 현재 화면에 보이는 지역 정보 목록 */
        visibleRegions: [],
        /** @type {{code: string, name: string} | null} 마우스 오버된 지역 정보 */
        hoveredRegionInfo: null,
        /** @type {object | null} 로드된 전체 GeoJSON 데이터 (선택적) */
        geoJsonData: null, // MapView에서 로드 후 여기 저장 가능
    }),
    actions: {
        /**
         * 보이는 지역 정보 목록(코드+좌표)을 업데이트한다.
         * @param {Array<{code: string, lat: number, lon: number}>} regionsData
         */
        setVisibleRegions(regionsData) {
        // ★ 데이터 구조가 바뀌었으므로, 단순 비교 대신 실제 변경 여부 확인 필요
        // (지금은 간단하게 항상 업데이트하도록 둔다)
        this.visibleRegions = regionsData;
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
        // ★★★ setGeoJsonData 액션 추가 ★★★
        setGeoJsonData(geoJson) {
          this.geoJsonData = geoJson;
          console.log("MapStore: GeoJSON 데이터 저장됨.");
        }
    }, // actions 정의 끝
});
