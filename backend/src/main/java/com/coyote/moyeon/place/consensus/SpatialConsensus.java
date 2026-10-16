package com.coyote.moyeon.place.consensus;

import com.coyote.moyeon.place.GeoMath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 게스트들이 제안한 장소를 종합해 결과를 정한다.
 * <ol>
 * <li>같은 장소의 제안을 합쳐 득표를 센다. 단독 1위(1위가 둘 이상이 아님)가 있으면 그 장소로 확정한다.</li>
 * <li>단독 1위가 없으면 가까운 장소끼리 군집으로 묶는다(군집 중심 거리 1km 이내).</li>
 * <li>득표 합이 가장 큰 군집을 고른다. 동점이면 더 촘촘한 군집, 전체 제안의 중심에 더 가까운 군집,
 * 제안 순서가 빠른 군집 순으로 정한다.</li>
 * <li>그 군집의 대표 중심점(득표 가중 기하 중앙값)과 지역 이름을 만든다.</li>
 * </ol>
 * 이후 중심점 주변의 후보를 만드는 일은 {@link ConsensusCandidateService}가 한다.
 */
public final class SpatialConsensus {

	public static final double CLUSTER_THRESHOLD_METERS = 1000;
	private static final double SPREAD_BUCKET_METERS = 10;
	private static final double CENTER_BUCKET_METERS = 100;

	private SpatialConsensus() {
	}

	public static ConsensusOutcome decide(List<Proposal> proposals) {
		if (proposals.isEmpty()) {
			return new ConsensusOutcome.NoProposals();
		}
		List<PlaceVote> votes = tally(proposals);
		int totalVotes = proposals.size();

		List<PlaceVote> byVotes = votes.stream().sorted(Comparator.comparingInt(PlaceVote::votes).reversed()).toList();
		if (byVotes.size() == 1 || byVotes.get(0).votes() > byVotes.get(1).votes()) {
			return new ConsensusOutcome.Majority(byVotes.get(0), totalVotes);
		}

		List<Cluster> clusters = PlaceClusterer.cluster(votes, CLUSTER_THRESHOLD_METERS);
		double[] overall = CenterFinder.geometricMedian(votes.stream()
				.map(vote -> new CenterFinder.Point(vote.place().latitude(), vote.place().longitude(), vote.votes()))
				.toList());

		// 거리는 구간(촘촘함 10m, 중심까지 100m)으로 묶어 비교한다. 좌표 근사 때문에 몇 미터 차이로 사실상 같은 거리의
		// 순서가 갈리지 않게 하고, 비교가 일관되도록(추이적이도록) 하기 위해서다.
		Comparator<Cluster> best = Comparator.comparingInt(Cluster::totalVotes).reversed()
				.thenComparingLong(cluster -> Math.round(cluster.spreadMeters() / SPREAD_BUCKET_METERS))
				.thenComparingLong(cluster -> Math.round(GeoMath.distanceMeters(overall[0], overall[1],
						cluster.centerLatitude(), cluster.centerLongitude()) / CENTER_BUCKET_METERS))
				.thenComparingInt(Cluster::firstOrder);
		Cluster chosen = clusters.stream().min(best).orElseThrow();

		double[] center = CenterFinder.geometricMedian(chosen.members().stream()
				.map(vote -> new CenterFinder.Point(vote.place().latitude(), vote.place().longitude(), vote.votes()))
				.toList());
		return new ConsensusOutcome.Spatial(RegionLabeler.label(chosen.members()), center[0], center[1], chosen,
				clusters.size(), totalVotes);
	}

	/** 같은 장소(id)의 제안을 합친다. 순서는 입력에서 먼저 나온 제안 순서를 따른다. */
	static List<PlaceVote> tally(List<Proposal> proposals) {
		Map<String, int[]> counts = new LinkedHashMap<>();
		Map<String, Proposal> first = new LinkedHashMap<>();
		for (Proposal proposal : proposals.stream().sorted(Comparator.comparingInt(Proposal::order)).toList()) {
			String id = proposal.place().id();
			counts.computeIfAbsent(id, key -> new int[1])[0]++;
			first.putIfAbsent(id, proposal);
		}
		List<PlaceVote> votes = new ArrayList<>();
		for (Map.Entry<String, Proposal> entry : first.entrySet()) {
			votes.add(new PlaceVote(entry.getValue().place(), counts.get(entry.getKey())[0], entry.getValue().order()));
		}
		return votes;
	}
}
