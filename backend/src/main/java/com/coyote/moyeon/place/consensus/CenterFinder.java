package com.coyote.moyeon.place.consensus;

import com.coyote.moyeon.place.GeoMath;
import java.util.List;

/**
 * 대표 중심점을 구한다. 득표로 가중한 "기하 중앙값"으로, 모든 제안 장소까지의 이동 거리 합이 가장 작은 지점이다.
 * 평균 좌표는 멀리 떨어진 장소 하나에 끌려가지만 중앙값은 그렇지 않다. Weiszfeld 반복법으로 계산한다.
 */
public final class CenterFinder {

	private static final int MAX_ITERATIONS = 200;
	private static final double CONVERGED_METERS = 0.1;
	/** 현재 위치가 데이터 점과 겹칠 때 0으로 나누지 않으려고 쓰는 최소 거리(미터) */
	private static final double MIN_DISTANCE_METERS = 1.0;

	private CenterFinder() {
	}

	public record Point(double latitude, double longitude, double weight) {
	}

	/** {위도, 경도}를 돌려준다. */
	public static double[] geometricMedian(List<Point> points) {
		if (points.isEmpty()) {
			throw new IllegalArgumentException("points must not be empty");
		}
		double totalWeight = points.stream().mapToDouble(Point::weight).sum();
		// 한 점의 가중치가 전체의 절반을 넘으면 그 점이 곧 중앙값이다.
		for (Point point : points) {
			if (point.weight() * 2 > totalWeight) {
				return new double[] { point.latitude(), point.longitude() };
			}
		}
		double lat = points.stream().mapToDouble(p -> p.latitude() * p.weight()).sum() / totalWeight;
		double lon = points.stream().mapToDouble(p -> p.longitude() * p.weight()).sum() / totalWeight;
		// 두 점의 가중치가 같으면 이동 거리 합은 두 점 사이 어디서나 같아 중앙값이 하나로 정해지지 않는다.
		// 반복 계산은 이런 경우 한쪽으로 조금씩 밀려 가므로 정중앙으로 고정한다.
		if (points.size() == 2) {
			return new double[] { lat, lon };
		}
		for (int i = 0; i < MAX_ITERATIONS; i++) {
			double numeratorLat = 0;
			double numeratorLon = 0;
			double denominator = 0;
			for (Point point : points) {
				double distance = Math.max(MIN_DISTANCE_METERS,
						GeoMath.distanceMeters(lat, lon, point.latitude(), point.longitude()));
				double weight = point.weight() / distance;
				numeratorLat += point.latitude() * weight;
				numeratorLon += point.longitude() * weight;
				denominator += weight;
			}
			double nextLat = numeratorLat / denominator;
			double nextLon = numeratorLon / denominator;
			double moved = GeoMath.distanceMeters(lat, lon, nextLat, nextLon);
			lat = nextLat;
			lon = nextLon;
			if (moved < CONVERGED_METERS) {
				break;
			}
		}
		return new double[] { lat, lon };
	}
}
