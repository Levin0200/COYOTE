package com.coyote.moyeon.place.consensus;

import com.coyote.moyeon.place.GeoMath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 가까운 장소끼리 묶는다. 군집의 중심(득표 가중 평균) 사이 거리가 기준 이하인 두 군집을 가장 가까운 것부터 합친다.
 * 단순히 "하나라도 가까우면 같은 군집"으로 잇는 방식은 장소가 줄줄이 이어질 때 한 군집이 길게 늘어나므로 쓰지 않는다.
 * 같은 거리면 앞선 군집(제안 순서가 빠른 쪽)부터 합쳐서 입력 순서와 상관없이 결과가 같다.
 */
public final class PlaceClusterer {

	private static final double TIE_EPSILON_METERS = 1e-6;

	private PlaceClusterer() {
	}

	public static List<Cluster> cluster(List<PlaceVote> votes, double thresholdMeters) {
		List<List<PlaceVote>> groups = new ArrayList<>();
		votes.stream().sorted(Comparator.comparingInt(PlaceVote::order).thenComparing(vote -> vote.place().id()))
				.forEach(vote -> groups.add(new ArrayList<>(List.of(vote))));

		while (groups.size() > 1) {
			List<Cluster> clusters = groups.stream().map(Cluster::of).toList();
			int bestI = -1;
			int bestJ = -1;
			double best = Double.MAX_VALUE;
			for (int i = 0; i < clusters.size(); i++) {
				for (int j = i + 1; j < clusters.size(); j++) {
					double distance = GeoMath.distanceMeters(clusters.get(i).centerLatitude(),
							clusters.get(i).centerLongitude(), clusters.get(j).centerLatitude(),
							clusters.get(j).centerLongitude());
					if (distance <= thresholdMeters && distance < best - TIE_EPSILON_METERS) {
						best = distance;
						bestI = i;
						bestJ = j;
					}
				}
			}
			if (bestI < 0) {
				break;
			}
			groups.get(bestI).addAll(groups.remove(bestJ));
		}
		return groups.stream().map(Cluster::of).toList();
	}
}
