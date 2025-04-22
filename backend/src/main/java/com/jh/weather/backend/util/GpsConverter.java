package com.jh.weather.backend.util; // ★★★ 본인 패키지 이름과 일치하는지 확인! ★★★

public class GpsConverter {

    private static final double RE = 6371.00877; // 지구 반경 (km)
    private static final double GRID = 5.0;      // 격자 간격 (km)
    private static final double SLAT1 = 30.0;    // 표준 위도 1 (degree)
    private static final double SLAT2 = 60.0;    // 표준 위도 2 (degree)
    private static final double OLON = 126.0;    // 기준점 경도 (degree)
    private static final double OLAT = 38.0;     // 기준점 위도 (degree)

    private static final double XO = 43; // 예시 값
    private static final double YO = 136; // 예시 값

    // 위도/경도(degree) -> 격자 좌표(X,Y)로 변환하는 정적(static) 메소드
    public static LatXLonY convertGpsToGrid(double latitude, double longitude) {
        double D2R = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * D2R;
        double slat2 = SLAT2 * D2R;
        double olon = OLON * D2R;
        double olat = OLAT * D2R;

        double sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5));
        double sf = Math.pow(Math.tan(Math.PI * 0.25 + slat1 * 0.5), sn) * Math.cos(slat1) / sn;
        double ro = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), sn);

        double ra = Math.pow(Math.tan(Math.PI * 0.25 + latitude * D2R * 0.5), sn);
        ra = re * sf / ra;

        double theta = longitude * D2R - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        double gridX = ra * Math.sin(theta) + XO + 0.5;
        double gridY = ro - ra * Math.cos(theta) + YO + 0.5;

        return new LatXLonY(latitude, longitude, (int) gridX, (int) gridY);
    }

    // 변환된 위경도와 격자 좌표를 함께 담는 내부 클래스
    public static class LatXLonY {
        public final double lat;
        public final double lon;
        public final int x;
        public final int y;

        public LatXLonY(double lat, double lon, int x, int y) {
            this.lat = lat;
            this.lon = lon;
            this.x = x;
            this.y = y;
        }
        public double getLat() { return lat; }
        public double getLon() { return lon; }
        public int getX() { return x; }
        public int getY() { return y; }
    }

    // 테스트용 main 메소드
    public static void main(String[] args) {
        double testLat = 37.5798; // 예시: 서울 근처
        double testLon = 126.9388;
        LatXLonY result = convertGpsToGrid(testLat, testLon);
        System.out.println("Lat: " + result.getLat() + ", Lon: " + result.getLon() +
                " -> Expected X=60, Y=127");
        System.out.println("Calculated X: " + result.getX() + ", Y: " + result.getY());
    }
}