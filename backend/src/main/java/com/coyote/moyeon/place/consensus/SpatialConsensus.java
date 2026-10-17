package com.coyote.moyeon.place.consensus;

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
 * <li>득표 합이 가장 큰 군집이 하나면 그 군집을 고른다.</li>
 * <li>가장 큰 군집도 동률이면 어느 한쪽을 임의로 고르지 않고, 동률 군집의 제안을 모두 합쳐 절충 중심을 만든다.</li>
 * <li>선정된 제안들의 대표 중심점(득표 가중 기하 중앙값)과 지역 이름을 만든다.</li>
 * </ol>
 * 이후 중심점 주변의 후보를 만드는 일은 {@link ConsensusCandidateService}가 한다.
 */
public final class SpatialConsensus {

	public static final double CLUSTER_THRESHOLD_METERS = 1000;

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
		int largestVotes = clusters.stream().mapToInt(Cluster::totalVotes).max().orElseThrow();
		List<Cluster> largest = clusters.stream().filter(cluster -> cluster.totalVotes() == largestVotes).toList();
		Cluster chosen;
		if (largest.size() == 1) {
			chosen = largest.get(0);
		} else {
			// 같은 인원의 군집 중 먼저 응답한 쪽을 고르던 편향을 없앤다. 동률 군집의 모든 제안을 보존해야
			// 후보 풀에도 양쪽 제안이 함께 들어가고, 호스트가 최종적으로 공정하게 선택할 수 있다.
			List<PlaceVote> tiedMembers = largest.stream().flatMap(cluster -> cluster.members().stream()).toList();
			chosen = Cluster.of(tiedMembers);
		}

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
