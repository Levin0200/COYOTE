package com.coyote.moyeon.place.consensus;

/** 게스트 장소 제안을 종합한 결과. */
public sealed interface ConsensusOutcome {

	/** 제안한 게스트가 없다(모두 건너뜀). 결과의 장소는 "미정"이다. */
	record NoProposals() implements ConsensusOutcome {
	}

	/** 가장 많이 제안된 장소가 하나로 정해졌다(단독 1위). */
	record Majority(PlaceVote winner, int totalVotes) implements ConsensusOutcome {
	}

	/**
	 * 단독 1위가 없어서 공간적으로 합의한 지역. cluster는 유일한 최대 군집이거나, 최대 군집까지 동률이면
	 * 동률 군집의 제안을 모두 담은 절충 묶음이다. center는 해당 제안들의 대표 중심점이다.
	 * clusterCount는 만들어진 군집 수, totalVotes는 전체 제안 수다.
	 */
	record Spatial(String regionLabel, double centerLatitude, double centerLongitude, Cluster cluster, int clusterCount,
			int totalVotes) implements ConsensusOutcome {
	}
}
