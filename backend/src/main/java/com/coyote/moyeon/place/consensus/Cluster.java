package com.coyote.moyeon.place.consensus;

import com.coyote.moyeon.place.GeoMath;
import java.util.List;

/**
 * 서로 가까운 제안 장소의 묶음. 중심은 득표로 가중한 평균 좌표이고, spreadMeters는 득표로 가중한
 * 중심까지의 평균 거리(작을수록 촘촘하다)다.
 */
public record Cluster(List<PlaceVote> members, int totalVotes, double centerLatitude, double centerLongitude,
		double spreadMeters, int firstOrder) {

	public static Cluster of(List<PlaceVote> members) {
		int total = 0;
		double lat = 0;
		double lon = 0;
		int firstOrder = Integer.MAX_VALUE;
		for (PlaceVote vote : members) {
			total += vote.votes();
			lat += vote.place().latitude() * vote.votes();
			lon += vote.place().longitude() * vote.votes();
			firstOrder = Math.min(firstOrder, vote.order());
		}
		lat /= total;
		lon /= total;
		double spread = 0;
		for (PlaceVote vote : members) {
			spread += vote.votes() * GeoMath.distanceMeters(lat, lon, vote.place().latitude(), vote.place().longitude());
		}
		return new Cluster(List.copyOf(members), total, lat, lon, spread / total, firstOrder);
	}
}
