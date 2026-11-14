package com.coyote.moyeon.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(nullable = false, length = 100)
	private String password;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 255)
	private String address;

	@Column(nullable = false)
	private Instant termsAgreedAt;

	@Column(nullable = false)
	private Instant createdAt;

	public User(String email, String password, String name, String address) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.address = address;
		this.termsAgreedAt = Instant.now();
		this.createdAt = this.termsAgreedAt;
	}

	public void updateProfile(String name, String address) {
		this.name = name;
		this.address = address;
	}
}
