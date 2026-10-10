package com.coyote.moyeon.meeting;

import com.coyote.moyeon.user.User;
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
import jakarta.persistence.ManyToOne;
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
@Table(name = "meeting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 64)
	private String inviteToken;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "host_id")
	private User host;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(length = 1000)
	private String description;

	@Column(nullable = false)
	private Instant responseDueDate;

	private Instant linkExpirationDate;

	@Column(nullable = false)
	private LocalDate candidateStartDate;

	@Column(nullable = false)
	private LocalDate candidateEndDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PlacePolicyMode placePolicyMode;

	@Column(length = 100)
	private String policyRegion;

	private Integer policyRadiusKm;

	@Column(length = 100)
	private String policyKeyword;

	@Column(length = 100)
	private String fixedPlace;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "meeting_place_option", joinColumns = @JoinColumn(name = "meeting_id"))
	@OrderColumn(name = "sort_order")
	private List<PlaceOption> placeOptions = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MeetingStatus status;

	@Column(nullable = false)
	private boolean manuallyClosed;

	@Column(nullable = false)
	private Instant createdAt;

	public Meeting(String inviteToken, User host, String title, String description, Instant responseDueDate,
			Instant linkExpirationDate, LocalDate candidateStartDate, LocalDate candidateEndDate,
			PlacePolicyMode placePolicyMode, String policyRegion, Integer policyRadiusKm, String policyKeyword,
			String fixedPlace, List<PlaceOption> placeOptions) {
		this.inviteToken = inviteToken;
		this.host = host;
		this.title = title;
		this.description = description;
		this.responseDueDate = responseDueDate;
		this.linkExpirationDate = linkExpirationDate;
		this.candidateStartDate = candidateStartDate;
		this.candidateEndDate = candidateEndDate;
		this.placePolicyMode = placePolicyMode;
		this.policyRegion = policyRegion;
		this.policyRadiusKm = policyRadiusKm;
		this.policyKeyword = policyKeyword;
		this.fixedPlace = fixedPlace;
		this.placeOptions = new ArrayList<>(placeOptions);
		this.status = MeetingStatus.COLLECTING;
		this.createdAt = Instant.now();
	}

	public boolean isLinkExpired(Instant now) {
		return linkExpirationDate != null && !now.isBefore(linkExpirationDate);
	}

	public boolean isClosed(Instant now) {
		return status != MeetingStatus.COLLECTING || !now.isBefore(responseDueDate);
	}
}
