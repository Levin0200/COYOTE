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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_response")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingResponse {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "meeting_id")
	private Meeting meeting;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "participant_id", unique = true)
	private Participant participant;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PlaceMode placeMode;

	@Column(length = 100)
	private String region;

	@Column(length = 100)
	private String placePreference;

	private Integer recommendationRadius;

	@Column(length = 100)
	private String recommendationKeyword;

	private Double latitude;

	private Double longitude;

	/** 제안·선택한 장소의 ID와 부가 정보. 장소 ID 없이 이름만 받은 응답에서는 null이다. 좌표는 latitude/longitude에 저장한다. */
	@Column(name = "place_id", length = 64)
	private String placeId;

	@Column(name = "place_address", length = 255)
	private String placeAddress;

	@Column(name = "place_category", length = 100)
	private String placeCategory;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "response_available_date", joinColumns = @JoinColumn(name = "response_id"))
	@Column(name = "available_date", nullable = false)
	private Set<LocalDate> availableDates = new HashSet<>();

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public MeetingResponse(Meeting meeting, Participant participant) {
		this.meeting = meeting;
		this.participant = participant;
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
	}

	public void update(Set<LocalDate> dates, PlaceMode placeMode, String region, String placePreference,
			Integer recommendationRadius, String recommendationKeyword, PlaceInfo place) {
		this.availableDates.clear();
		this.availableDates.addAll(dates);
		this.placeMode = placeMode;
		this.region = region;
		this.placePreference = placePreference;
		this.recommendationRadius = recommendationRadius;
		this.recommendationKeyword = recommendationKeyword;
		this.placeId = place == null ? null : place.id();
		this.placeAddress = place == null ? null : place.address();
		this.placeCategory = place == null ? null : place.category();
		this.latitude = place == null ? null : place.latitude();
		this.longitude = place == null ? null : place.longitude();
		this.updatedAt = Instant.now();
	}

	/** 저장된 장소 정보. 장소 ID와 좌표가 모두 없으면(이름만 받은 응답) null. */
	public PlaceInfo toPlaceInfo() {
		return placeId == null && latitude == null ? null
				: new PlaceInfo(placeId, placePreference, placeAddress, placeCategory, latitude, longitude);
	}
}
