package com.coyote.moyeon.place.consensus;

import static com.coyote.moyeon.place.consensus.TestPlaces.GANGNAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coyote.moyeon.place.GeoMath;
import java.util.List;
import org.junit.jupiter.api.Test;

class CenterFinderTest {

	private static CenterFinder.Point point(double bearing, double meters, double weight) {
		double[] p = GeoMath.offset(GANGNAM[0], GANGNAM[1], bearing, meters);
		return new CenterFinder.Point(p[0], p[1], weight);
	}

	private static double totalDistance(double[] center, List<CenterFinder.Point> points) {
		return points.stream().mapToDouble(
				p -> p.weight() * GeoMath.distanceMeters(center[0], center[1], p.latitude(), p.longitude())).sum();
	}

	private static double[] centroid(List<CenterFinder.Point> points) {
		double total = points.stream().mapToDouble(CenterFinder.Point::weight).sum();
		return new double[] { points.stream().mapToDouble(p -> p.latitude() * p.weight()).sum() / total,
				points.stream().mapToDouble(p -> p.longitude() * p.weight()).sum() / total };
	}

	@Test
	void aSinglePointIsItsOwnMedian() {
		CenterFinder.Point only = point(0, 0, 3);

		assertThat(CenterFinder.geometricMedian(List.of(only))).containsExactly(only.latitude(), only.longitude());
	}

	@Test
	void twoEqualPointsGiveTheMidpoint() {
		List<CenterFinder.Point> points = List.of(point(90, 0, 1), point(90, 2000, 1));

		double[] center = CenterFinder.geometricMedian(points);

		assertThat(GeoMath.distanceMeters(center[0], center[1], points.get(0).latitude(), points.get(0).longitude()))
				.isBetween(995.0, 1005.0);
	}

	@Test
	void aPointWithMoreThanHalfOfTheWeightIsTheMedian() {
		CenterFinder.Point heavy = point(0, 0, 3);
		List<CenterFinder.Point> points = List.of(heavy, point(90, 4000, 1), point(180, 4000, 1));

		assertThat(CenterFinder.geometricMedian(points)).containsExactly(heavy.latitude(), heavy.longitude());
	}

	@Test
	void theMedianHasNoMoreTotalTravelThanTheAverage() {
		List<CenterFinder.Point> points = List.of(point(0, 0, 1), point(90, 3000, 1), point(200, 2500, 1),
				point(300, 6000, 1));

		double[] median = CenterFinder.geometricMedian(points);

		assertThat(totalDistance(median, points)).isLessThanOrEqualTo(totalDistance(centroid(points), points) + 1);
	}

	@Test
	void theMedianIsNotDraggedAwayByASingleFarOutlier() {
		List<CenterFinder.Point> points = List.of(point(0, 0, 1), point(90, 100, 1), point(180, 100, 1),
				point(270, 100, 1), point(45, 8000, 1));

		double[] median = CenterFinder.geometricMedian(points);
		double[] average = centroid(points);

		double medianFromGroup = GeoMath.distanceMeters(median[0], median[1], GANGNAM[0], GANGNAM[1]);
		double averageFromGroup = GeoMath.distanceMeters(average[0], average[1], GANGNAM[0], GANGNAM[1]);
		assertThat(medianFromGroup).isLessThan(300);
		assertThat(averageFromGroup).isGreaterThan(1500);
	}

	@Test
	void pointsOnALineHaveTheMiddleOneAsMedian() {
		List<CenterFinder.Point> points = List.of(point(90, 0, 1), point(90, 5000, 1), point(90, 10000, 1));

		double[] median = CenterFinder.geometricMedian(points);

		assertThat(GeoMath.distanceMeters(median[0], median[1], points.get(1).latitude(), points.get(1).longitude()))
				.isLessThan(50);
	}

	@Test
	void identicalPointsDoNotProduceNaN() {
		List<CenterFinder.Point> points = List.of(point(0, 0, 1), point(0, 0, 1), point(0, 0, 1));

		double[] median = CenterFinder.geometricMedian(points);

		assertThat(median[0]).isFinite();
		assertThat(GeoMath.distanceMeters(median[0], median[1], GANGNAM[0], GANGNAM[1])).isLessThan(1);
	}

	@Test
	void emptyInputIsRejected() {
		assertThatThrownBy(() -> CenterFinder.geometricMedian(List.of())).isInstanceOf(IllegalArgumentException.class);
	}
}
