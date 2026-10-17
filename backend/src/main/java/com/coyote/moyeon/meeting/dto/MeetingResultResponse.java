package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.DecisionType;
import com.coyote.moyeon.meeting.FinalResult;
import com.coyote.moyeon.meeting.FinalResultCandidate;
import com.coyote.moyeon.meeting.PlaceStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 마감 후 공개되는 집계 결과. 개별 응답과 닉네임은 포함하지 않는다.
 *
 * <p>placeStatus가 DECIDED가 아니면 selectedPlaces는 비어 있다. PENDING(후보 분석 중)·AWAITING_HOST(호스트 선택 대기)에는
 * hostDecisionDeadline(AWAITING_HOST일 때만)까지 호스트가 고르지 않으면 자동으로 정해진다. 후보 목록(placeCandidates)은
 * 호스트에게만 내려주고 게스트에게는 빈 목록이다.</p>
 */
public record MeetingResultResponse(
		LocalDate selectedDate,
		List<String> selectedPlaces,
		List<PlaceDetailView> selectedPlaceDetails,
		String selectedPlace,
		DecisionType decisionType,
		String reason,
		int dateVoteCount,
		int placeVoteCount,
		int totalResponses,
		Instant createdAt,
		PlaceStatus placeStatus,
		Instant hostDecisionDeadline,
		List<CandidateView> placeCandidates) {

	private static final String UNDECIDED_PLACE = "추후 협의";

	/** 후보 한 곳. votes는 이 장소를 고르거나 제안한 응답 수, reason은 AI가 붙인 짧은 이유(없으면 null)다. */
	public record CandidateView(String id, String name, String address, String category, Double latitude,
			Double longitude, int votes, String reason) {

		static CandidateView from(FinalResultCandidate candidate) {
			return new CandidateView(candidate.placeId(), candidate.name(), candidate.address(), candidate.category(),
					candidate.latitude(), candidate.longitude(), candidate.votes(), candidate.reason());
		}
	}

	/** 게스트와 목록 요약용. 후보 목록은 포함하지 않는다. */
	public static MeetingResultResponse from(FinalResult result) {
		return of(result, false);
	}

	/** 호스트용. 호스트가 고를 후보 목록을 포함한다. */
	public static MeetingResultResponse forHost(FinalResult result) {
		return of(result, true);
	}

	private static MeetingResultResponse of(FinalResult result, boolean includeCandidates) {
		List<String> places = result.getSelectedPlaceNames();
		List<PlaceDetailView> details = result.getSelectedPlaces().stream().map(PlaceDetailView::from).toList();
		List<CandidateView> candidates = includeCandidates && result.getPlaceStatus() == PlaceStatus.AWAITING_HOST
				? result.getCandidates().stream().map(CandidateView::from).toList()
				: List.of();
		return new MeetingResultResponse(result.getSelectedDate(), places, details,
				places.isEmpty() ? UNDECIDED_PLACE : String.join(" · ", places), result.getDecisionType(),
				result.getReason(), result.getDateVoteCount(), result.getPlaceVoteCount(),
				result.getTotalResponses(), result.getCreatedAt(), result.getPlaceStatus(),
				result.getPlaceStatus() == PlaceStatus.AWAITING_HOST ? result.getHostDecisionDeadline() : null,
				candidates);
	}
}
