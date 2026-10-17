package com.coyote.moyeon.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(nullable = false, length = 6)
	private String code;

	@Column(nullable = false)
	private boolean verified;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private int failedAttempts;

	public EmailVerification(String email) {
		this.email = email;
	}

	public void issue(String code, Duration validFor) {
		this.code = code;
		this.verified = false;
		this.failedAttempts = 0;
		this.expiresAt = Instant.now().plus(validFor);
	}

	public void recordFailure() {
		this.failedAttempts++;
	}

	public boolean isExpired() {
		return Instant.now().isAfter(expiresAt);
	}

	public void markVerified(Duration signupWindow) {
		this.verified = true;
		this.expiresAt = Instant.now().plus(signupWindow);
	}
}
