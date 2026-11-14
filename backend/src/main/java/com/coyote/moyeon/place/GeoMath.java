package com.coyote.moyeon.place;

/** 좌표 계산 도우미(WGS84, 구면 근사). */
public final class GeoMath {

	private static final double EARTH_RADIUS_METERS = 6_371_000;
	private static final double METERS_PER_DEGREE_LAT = 111_000;

	private GeoMath() {
	}

	/** 두 지점 사이의 거리(미터, 하버사인). */
	public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
		double p1 = Math.toRadians(lat1);
		double p2 = Math.toRadians(lat2);
		double dLat = p2 - p1;
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(p1) * Math.cos(p2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(a)));
	}

	/** 중심에서 반경만큼 떨어진 정사각형 영역 {최소 경도, 최소 위도, 최대 경도, 최대 위도}. */
	public static double[] boundingBox(double lat, double lon, double radiusMeters) {
		double dLat = radiusMeters / METERS_PER_DEGREE_LAT;
		double dLon = radiusMeters / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));
		return new double[] { lon - dLon, lat - dLat, lon + dLon, lat + dLat };
	}

	/** 중심에서 방위각(도)과 거리(미터)만큼 떨어진 지점의 {위도, 경도}. 개발·테스트용 가짜 데이터에 쓴다. */
	public static double[] offset(double lat, double lon, double bearingDegrees, double distanceMeters) {
		double bearing = Math.toRadians(bearingDegrees);
		double dLat = distanceMeters * Math.cos(bearing) / METERS_PER_DEGREE_LAT;
		double dLon = distanceMeters * Math.sin(bearing) / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));
		return new double[] { lat + dLat, lon + dLon };
	}
}
