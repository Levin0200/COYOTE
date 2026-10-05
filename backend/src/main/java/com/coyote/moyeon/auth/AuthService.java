package com.coyote.moyeon.auth;

import com.coyote.moyeon.auth.dto.ConfirmCodeRequest;
import com.coyote.moyeon.auth.dto.LoginRequest;
import com.coyote.moyeon.auth.dto.SendCodeRequest;
import com.coyote.moyeon.auth.dto.SignupRequest;
import com.coyote.moyeon.auth.dto.TokenResponse;
import com.coyote.moyeon.common.exception.BusinessException;
import com.coyote.moyeon.user.User;
import com.coyote.moyeon.user.UserRepository;
import com.coyote.moyeon.user.dto.UserResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final Duration CODE_VALID_FOR = Duration.ofMinutes(5);
	private static final Duration SIGNUP_WINDOW = Duration.ofMinutes(30);
	private static final int MAX_FAILED_ATTEMPTS = 5;

	private final SecureRandom random = new SecureRandom();
	private final UserRepository userRepository;
	private final EmailVerificationRepository verificationRepository;
	private final EmailSender emailSender;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;

	public AuthService(UserRepository userRepository, EmailVerificationRepository verificationRepository,
			EmailSender emailSender, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
		this.userRepository = userRepository;
		this.verificationRepository = verificationRepository;
		this.emailSender = emailSender;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
	}

	@Transactional
	public void sendVerificationCode(SendCodeRequest request) {
		String email = normalize(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");
		}
		String code = String.format("%06d", random.nextInt(1_000_000));
		EmailVerification verification = verificationRepository.findByEmail(email)
				.orElseGet(() -> new EmailVerification(email));
		verification.issue(code, CODE_VALID_FOR);
		verificationRepository.save(verification);
		emailSender.sendVerificationCode(email, code);
	}

	@Transactional(noRollbackFor = BusinessException.class)
	public void confirmVerificationCode(ConfirmCodeRequest request) {
		String email = normalize(request.email());
		EmailVerification verification = verificationRepository.findByEmail(email)
				.orElseThrow(() -> invalidCode());
		if (verification.isVerified() || verification.isExpired()
				|| verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
			throw invalidCode();
		}
		boolean matches = MessageDigest.isEqual(
				verification.getCode().getBytes(StandardCharsets.UTF_8),
				request.code().getBytes(StandardCharsets.UTF_8));
		if (!matches) {
			verification.recordFailure();
			throw invalidCode();
		}
		verification.markVerified(SIGNUP_WINDOW);
	}

	@Transactional
	public UserResponse signup(SignupRequest request) {
		String email = normalize(request.email());
		EmailVerification verification = verificationRepository.findByEmail(email)
				.filter(v -> v.isVerified() && !v.isExpired())
				.orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_VERIFIED",
						"이메일 인증이 필요합니다."));
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");
		}
		String address = request.address() == null || request.address().isBlank() ? null : request.address().trim();
		User user = new User(email, passwordEncoder.encode(request.password()), request.name().trim(), address);
		try {
			userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");
		}
		verificationRepository.delete(verification);
		return UserResponse.from(user);
	}

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(normalize(request.email()))
				.filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
						"이메일 또는 비밀번호가 일치하지 않습니다."));
		return jwtTokenService.issue(user.getId());
	}

	private BusinessException invalidCode() {
		return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE",
				"인증번호가 올바르지 않거나 만료되었습니다.");
	}

	private String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
