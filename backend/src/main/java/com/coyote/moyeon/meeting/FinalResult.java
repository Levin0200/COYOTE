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
		this.createdAt = Instant.now();
	}

	public List<String> getSelectedPlaceNames() {
		return selectedPlaces.stream().map(SelectedPlace::name).toList();
	}
}
