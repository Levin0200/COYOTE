package com.coyote.moyeon.meeting;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "final_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinalResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "meeting_id", unique = true)
	private Meeting meeting;

	@Column(nullable = false)
	private LocalDate selectedDate;

	@Column(nullable = false)
	private int dateVoteCount;

	@Column(nullable = false)
	private int placeVoteCount;

	@Column(nullable = false)
	private int totalResponses;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DecisionType decisionType;

	@Column(nullable = false, length = 500)
	private String reason;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "final_result_place", joinColumns = @JoinColumn(name = "final_result_id"))
	@OrderColumn(name = "sort_order")
	private List<SelectedPlace> selectedPlaces = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private PlaceStatus placeStatus;

	private Instant hostDecisionDeadline;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "final_result_candidate", joinColumns = @JoinColumn(name = "final_result_id"))
	@OrderColumn(name = "sort_order")
	private List<FinalResultCandidate> candidates = new ArrayList<>();

	@Column(nullable = false)
	private Instant createdAt;

	public FinalResult(Meeting meeting, GeneratedResult generated) {
		this.meeting = meeting;
		this.selectedDate = generated.selectedDate();
		this.dateVoteCount = generated.dateVoteCount();
		this.placeVoteCount = generated.placeVoteCount();
		this.totalResponses = generated.totalResponses();
		this.decisionType = generated.decisionType();
		this.reason = generated.reason();
		this.selectedPlaces = new ArrayList<>(generated.selectedPlaces());
		this.placeStatus = generated.placeStatus();
		this.candidates = new ArrayList<>(generated.candidates());
		this.createdAt = Instant.now();
	}

	/** 장소 상태. 장소 상태를 저장하기 전에 만들어진 결과(값이 null)는 이미 정해진 것으로 본다. */
	public PlaceStatus getPlaceStatus() {
		return placeStatus == null ? PlaceStatus.DECIDED : placeStatus;
	}

	/** 호스트가 후보를 고르기를 기다리는 시한을 시작한다. */
	public void startHostWindow(Instant deadline) {
		this.hostDecisionDeadline = deadline;
	}

	/** 마감 직후 만든 후보로 호스트의 선택을 기다리는 상태가 된다. 후보가 없으면 장소 없이 끝낸다. */
	public void awaitHost(List<FinalResultCandidate> candidates, Instant deadline, int placeVoteCount,
			String placeReason) {
		this.candidates = new ArrayList<>(candidates);
		this.selectedPlaces = new ArrayList<>();
		this.placeVoteCount = placeVoteCount;
		this.placeStatus = PlaceStatus.AWAITING_HOST;
		this.hostDecisionDeadline = deadline;
		this.reason = cut(datePart() + placeReason);
	}

	/** 후보가 하나도 없어 장소를 정하지 못하고 추후 협의로 끝낸다. */
	public void leaveUndecided(String placeReason) {
		this.candidates = new ArrayList<>();
		this.selectedPlaces = new ArrayList<>();
		this.placeStatus = PlaceStatus.NONE;
		this.hostDecisionDeadline = null;
		this.reason = cut(datePart() + placeReason);
	}

	/** 후보 중 하나로 장소를 확정한다(호스트 선택 또는 자동 선정). */
	public void decidePlace(FinalResultCandidate chosen, DecisionType type, String placeReason) {
		this.selectedPlaces = new ArrayList<>(List.of(chosen.toSelectedPlace()));
		this.placeVoteCount = chosen.votes();
		this.placeStatus = PlaceStatus.DECIDED;
		this.decisionType = type;
		this.hostDecisionDeadline = null;
		this.reason = cut(datePart() + placeReason);
	}

	/** 선정 이유 중 날짜 설명 부분(첫 문장). 장소 설명만 바꿀 때 날짜 설명은 그대로 둔다. */
	private String datePart() {
		int end = reason == null ? -1 : reason.indexOf(". ");
		return end < 0 ? "" : reason.substring(0, end + 2);
	}

	private static String cut(String value) {
		return value.length() <= 500 ? value : value.substring(0, 500);
	}

	public List<String> getSelectedPlaceNames() {
		return selectedPlaces.stream().map(SelectedPlace::name).toList();
	}
}
