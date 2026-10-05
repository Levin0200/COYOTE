package com.coyote.moyeon.meeting;

import com.coyote.moyeon.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MeetingStatus status;

	@Column(nullable = false)
	private boolean manuallyClosed;

	@Column(nullable = false)
	private Instant createdAt;

	public Meeting(String inviteToken, User host, String title, String description, Instant responseDueDate,
			Instant linkExpirationDate) {
		this.inviteToken = inviteToken;
		this.host = host;
		this.title = title;
		this.description = description;
		this.responseDueDate = responseDueDate;
		this.linkExpirationDate = linkExpirationDate;
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
