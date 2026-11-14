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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "participant", uniqueConstraints = @UniqueConstraint(columnNames = { "meeting_id", "guest_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "meeting_id")
	private Meeting meeting;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false, length = 20)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ParticipantRole role;

	@Column(name = "guest_id", length = 64)
	private String guestId;

	@Column(nullable = false)
	private Instant createdAt;

	public static Participant host(Meeting meeting, User user, String nickname) {
		Participant p = new Participant();
		p.meeting = meeting;
		p.user = user;
		p.nickname = nickname;
		p.role = ParticipantRole.HOST;
		p.createdAt = Instant.now();
		return p;
	}

	public static Participant guest(Meeting meeting, String guestId, String nickname) {
		Participant p = new Participant();
		p.meeting = meeting;
		p.guestId = guestId;
		p.nickname = nickname;
		p.role = ParticipantRole.GUEST;
		p.createdAt = Instant.now();
		return p;
	}

	public void changeNickname(String nickname) {
		this.nickname = nickname;
	}
}
