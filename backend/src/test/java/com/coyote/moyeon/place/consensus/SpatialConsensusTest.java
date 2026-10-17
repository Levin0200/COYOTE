package com.coyote.moyeon.place.consensus;

import static com.coyote.moyeon.place.consensus.TestPlaces.GANGNAM;
import static com.coyote.moyeon.place.consensus.TestPlaces.HONGDAE;
import static com.coyote.moyeon.place.consensus.TestPlaces.at;
import static com.coyote.moyeon.place.consensus.TestPlaces.proposal;
import static org.assertj.core.api.Assertions.assertThat;

import com.coyote.moyeon.place.GeoMath;
import com.coyote.moyeon.place.Place;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpatialConsensusTest {

	private static ConsensusOutcome.Spatial spatial(ConsensusOutcome outcome) {
		assertThat(outcome).isInstanceOf(ConsensusOutcome.Spatial.class);
		return (ConsensusOutcome.Spatial) outcome;
	}

	private static List<String> ids(ConsensusOutcome.Spatial spatial) {
		return spatial.cluster().members().stream().map(vote -> vote.place().id()).sorted().toList();
	}

	@Test
	void noProposalsMeansNoPlace() {
		assertThat(SpatialConsensus.decide(List.of())).isInstanceOf(ConsensusOutcome.NoProposals.class);
	}

	@Test
	void aSingleProposalWins() {
		Place a = at("a", GANGNAM, 0, 0);

		ConsensusOutcome outcome = SpatialConsensus.decide(List.of(proposal(a, 1)));

		assertThat(outcome).isInstanceOf(ConsensusOutcome.Majority.class);
		assertThat(((ConsensusOutcome.Majority) outcome).winner().place().id()).isEqualTo("a");
	}

	@Test
	void aUniqueTopPlaceWinsOutright() {
		Place a = at("a", GANGNAM, 0, 0);
		Place b = at("b", HONGDAE, 0, 0);
		Place c = at("c", GANGNAM, 90, 3000);

		ConsensusOutcome outcome = SpatialConsensus.decide(List.of(proposal(a, 1), proposal(b, 2), proposal(a, 3),
				proposal(c, 4), proposal(a, 5)));

		ConsensusOutcome.Majority majority = (ConsensusOutcome.Majority) outcome;
		assertThat(majority.winner().place().id()).isEqualTo("a");
		assertThat(majority.winner().votes()).isEqualTo(3);
		assertThat(majority.totalVotes()).isEqualTo(5);
	}

	@Test
	void aUniqueTopWinsEvenWithoutAbsoluteMajority() {
		Place a = at("a", GANGNAM, 0, 0);
		Place b = at("b", HONGDAE, 0, 0);
		Place c = at("c", GANGNAM, 90, 3000);

		// 4표 중 2표(과반이 아님)지만 단독 1위다.
		ConsensusOutcome outcome = SpatialConsensus.decide(
				List.of(proposal(a, 1), proposal(a, 2), proposal(b, 3), proposal(c, 4)));

		assertThat(outcome).isInstanceOf(ConsensusOutcome.Majority.class);
	}

	@Test
	void aTieOfNearbyPlacesBecomesOneClusterWithACenterBetweenThem() {
		Place a = at("a", GANGNAM, 0, 0);
		Place b = at("b", GANGNAM, 90, 800);

		ConsensusOutcome.Spatial result = spatial(SpatialConsensus.decide(
				List.of(proposal(a, 1), proposal(b, 2), proposal(a, 3), proposal(b, 4))));

		assertThat(ids(result)).containsExactly("a", "b");
		assertThat(result.cluster().totalVotes()).isEqualTo(4);
		assertThat(result.clusterCount()).isEqualTo(1);
		assertThat(result.totalVotes()).isEqualTo(4);
		assertThat(GeoMath.distanceMeters(result.centerLatitude(), result.centerLongitude(), a.latitude(), a.longitude()))
				.isBetween(350.0, 450.0);
		assertThat(result.regionLabel()).isEqualTo("강남구 일대");
	}

	@Test
	void theBiggerClusterWinsWhenEveryPlaceHasOneVote() {
		List<Proposal> proposals = List.of(
				proposal(at("g1", GANGNAM, 0, 0), 1), proposal(at("h1", HONGDAE, 0, 0), 2),
				proposal(at("g2", GANGNAM, 90, 300), 3), proposal(at("h2", HONGDAE, 90, 300), 4),
				proposal(at("g3", GANGNAM, 180, 400), 5));

		ConsensusOutcome.Spatial result = spatial(SpatialConsensus.decide(proposals));

		assertThat(ids(result)).containsExactly("g1", "g2", "g3");
		assertThat(result.clusterCount()).isEqualTo(2);
		assertThat(GeoMath.distanceMeters(result.centerLatitude(), result.centerLongitude(), GANGNAM[0], GANGNAM[1]))
				.isLessThan(500);
	}

	@Test
	void equallyLargeClustersAreCombinedInsteadOfFavoringTheTighterOne() {
		// 홍대 쪽 두 곳은 900m, 강남 쪽 두 곳은 300m 떨어져 있다. 둘 다 2표지만 강남이 더 촘촘하다.
		List<Proposal> proposals = List.of(
				proposal(at("h1", HONGDAE, 0, 0), 1), proposal(at("h2", HONGDAE, 90, 900), 2),
				proposal(at("g1", GANGNAM, 0, 0), 3), proposal(at("g2", GANGNAM, 90, 300), 4));

		ConsensusOutcome.Spatial result = spatial(SpatialConsensus.decide(proposals));

		assertThat(ids(result)).containsExactly("g1", "g2", "h1", "h2");
		assertThat(result.cluster().totalVotes()).isEqualTo(4);
	}

	@Test
	void equallyLargeSinglePlaceClustersUseACompromiseCenter() {
		// 동쪽으로 5km 간격의 세 곳. 전체의 중심은 가운데 장소이고, 그 장소는 가장 늦게 제안됐다.
		Place west = at("west", GANGNAM, 90, 0);
		Place middle = at("middle", GANGNAM, 90, 5000);
		Place east = at("east", GANGNAM, 90, 10000);

		ConsensusOutcome.Spatial result = spatial(SpatialConsensus.decide(
				List.of(proposal(west, 1), proposal(east, 2), proposal(middle, 3))));

		assertThat(ids(result)).containsExactly("east", "middle", "west");
		assertThat(GeoMath.distanceMeters(result.centerLatitude(), result.centerLongitude(), middle.latitude(),
				middle.longitude())).isLessThan(1);
	}

	@Test
	void twoFarApartTiedProposalsAreBothKeptRegardlessOfResponseOrder() {
		Place first = at("first", GANGNAM, 0, 0);
		Place second = at("second", HONGDAE, 0, 0);
		List<Proposal> proposals = new ArrayList<>(List.of(proposal(first, 1), proposal(second, 2)));

		assertThat(ids(spatial(SpatialConsensus.decide(proposals)))).containsExactly("first", "second");
		Collections.reverse(proposals);
		assertThat(ids(spatial(SpatialConsensus.decide(proposals)))).containsExactly("first", "second");
		// 응답 순서 자체를 바꿔도 어느 한쪽이 탈락하지 않는다.
		assertThat(ids(spatial(SpatialConsensus.decide(
				List.of(proposal(first, 2), proposal(second, 1)))))).containsExactly("first", "second");
	}

	@Test
	void placesStrungOutInALineDoNotChainIntoOneLongCluster() {
		// 0m, 800m, 1750m. 이웃끼리는 1km 이내지만, 앞의 두 곳을 합친 중심(400m)에서 세 번째는 1350m라 합치지 않는다.
		Place a = at("a", GANGNAM, 90, 0);
		Place b = at("b", GANGNAM, 90, 800);
		Place c = at("c", GANGNAM, 90, 1750);

		ConsensusOutcome.Spatial result = spatial(SpatialConsensus.decide(
				List.of(proposal(a, 1), proposal(b, 2), proposal(c, 3))));

		assertThat(result.clusterCount()).isEqualTo(2);
		assertThat(ids(result)).containsExactly("a", "b");
	}

	@Test
	void theOutcomeDoesNotDependOnTheListOrder() {
		List<Proposal> proposals = new ArrayList<>(List.of(
				proposal(at("g1", GANGNAM, 0, 0), 1), proposal(at("h1", HONGDAE, 0, 0), 2),
				proposal(at("g2", GANGNAM, 90, 300), 3), proposal(at("h2", HONGDAE, 90, 300), 4),
				proposal(at("g3", GANGNAM, 180, 400), 5)));
		ConsensusOutcome.Spatial expected = spatial(SpatialConsensus.decide(proposals));

		for (int seed = 0; seed < 5; seed++) {
			Collections.shuffle(proposals, new java.util.Random(seed));
			ConsensusOutcome.Spatial shuffled = spatial(SpatialConsensus.decide(proposals));
			assertThat(ids(shuffled)).isEqualTo(ids(expected));
			assertThat(shuffled.centerLatitude()).isEqualTo(expected.centerLatitude());
			assertThat(shuffled.centerLongitude()).isEqualTo(expected.centerLongitude());
		}
	}
}
