package com.coyote.moyeon.place.consensus;

import static com.coyote.moyeon.place.consensus.TestPlaces.GANGNAM;
import static com.coyote.moyeon.place.consensus.TestPlaces.at;
import static org.assertj.core.api.Assertions.assertThat;

import com.coyote.moyeon.place.GeoMath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceClustererTest {

	private static PlaceVote vote(String id, double bearing, double meters, int votes, int order) {
		return new PlaceVote(at(id, GANGNAM, bearing, meters), votes, order);
	}

	@Test
	void emptyInputGivesNoClusters() {
		assertThat(PlaceClusterer.cluster(List.of(), 1000)).isEmpty();
	}

	@Test
	void aSinglePlaceIsItsOwnCluster() {
		List<Cluster> clusters = PlaceClusterer.cluster(List.of(vote("a", 0, 0, 2, 1)), 1000);

		assertThat(clusters).hasSize(1);
		assertThat(clusters.get(0).totalVotes()).isEqualTo(2);
		assertThat(clusters.get(0).spreadMeters()).isZero();
	}

	@Test
	void placesWithinTheThresholdAreMergedAndPlacesBeyondItAreNot() {
		assertThat(PlaceClusterer.cluster(List.of(vote("a", 90, 0, 1, 1), vote("b", 90, 990, 1, 2)), 1000)).hasSize(1);
		assertThat(PlaceClusterer.cluster(List.of(vote("a", 90, 0, 1, 1), vote("b", 90, 1010, 1, 2)), 1000)).hasSize(2);
	}

	@Test
	void theClusterCenterIsWeightedByVotes() {
		// 2표 장소(0m)와 1표 장소(900m)를 합치면 중심은 0m에서 300m 지점이다.
		List<Cluster> clusters = PlaceClusterer.cluster(List.of(vote("a", 90, 0, 2, 1), vote("b", 90, 900, 1, 2)), 1000);

		assertThat(clusters).hasSize(1);
		Cluster merged = clusters.get(0);
		assertThat(merged.totalVotes()).isEqualTo(3);
		assertThat(GeoMath.distanceMeters(merged.centerLatitude(), merged.centerLongitude(),
				at("x", GANGNAM, 90, 0).latitude(), at("x", GANGNAM, 90, 0).longitude())).isBetween(295.0, 305.0);
		assertThat(merged.firstOrder()).isEqualTo(1);
	}

	@Test
	void theClosestPairIsMergedFirst() {
		// a-b 800m, b-c 950m. 가장 가까운 a-b를 먼저 합치면 중심(400m)에서 c까지 1350m라 c는 따로 남는다.
		List<Cluster> clusters = PlaceClusterer.cluster(
				List.of(vote("a", 90, 0, 1, 1), vote("b", 90, 800, 1, 2), vote("c", 90, 1750, 1, 3)), 1000);

		assertThat(clusters).hasSize(2);
		assertThat(clusters.get(0).members()).extracting(member -> member.place().id()).containsExactlyInAnyOrder("a", "b");
		assertThat(clusters.get(1).members()).extracting(member -> member.place().id()).containsExactly("c");
	}

	@Test
	void theResultDoesNotDependOnTheInputOrder() {
		List<PlaceVote> votes = new ArrayList<>(List.of(vote("a", 90, 0, 1, 1), vote("b", 90, 400, 1, 2),
				vote("c", 0, 5000, 1, 3), vote("d", 0, 5300, 1, 4), vote("e", 180, 8000, 1, 5)));
		List<List<String>> expected = describe(PlaceClusterer.cluster(votes, 1000));

		for (int seed = 0; seed < 5; seed++) {
			Collections.shuffle(votes, new java.util.Random(seed));
			assertThat(describe(PlaceClusterer.cluster(votes, 1000))).isEqualTo(expected);
		}
	}

	private static List<List<String>> describe(List<Cluster> clusters) {
		return clusters.stream()
				.map(cluster -> cluster.members().stream().map(member -> member.place().id()).sorted().toList())
				.toList();
	}
}
