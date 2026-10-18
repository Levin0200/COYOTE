package com.coyote.moyeon.auth;

import com.coyote.moyeon.auth.dto.ConfirmCodeRequest;
import com.coyote.moyeon.auth.dto.LoginRequest;
import com.coyote.moyeon.auth.dto.SendCodeRequest;
import com.coyote.moyeon.auth.dto.SignupRequest;
import com.coyote.moyeon.auth.dto.TokenResponse;
import com.coyote.moyeon.common.exception.BusinessException;
import com.coyote.moyeon.common.ratelimit.RateLimitGuard;
import com.coyote.moyeon.common.ratelimit.RateLimitProperties;
import com.coyote.moyeon.common.ratelimit.RateLimiter;
import com.coyote.moyeon.user.User;
import com.coyote.moyeon.user.UserRepository;
import com.coyote.moyeon.user.dto.UserResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);
	/** 인증번호 유효 시간. 메일 본문의 안내 문구도 이 값을 쓴다. */
	static final Duration CODE_VALID_FOR = Duration.ofMinutes(5);
	private static final Duration SIGNUP_WINDOW = Duration.ofMinutes(30);
	private static final int MAX_FAILED_ATTEMPTS = 5;

	private final SecureRandom random = new SecureRandom();
	private final UserRepository userRepository;
	private final EmailVerificationRepository verificationRepository;
	private final EmailSender emailSender;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final RateLimiter rateLimiter;
	private final RateLimitGuard guard;
	private final RateLimitProperties limits;

	public AuthService(UserRepository userRepository, EmailVerificationRepository verificationRepository,
			EmailSender emailSender, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService,
			RateLimiter rateLimiter, RateLimitGuard guard, RateLimitProperties limits) {
		this.userRepository = userRepository;
		this.verificationRepository = verificationRepository;
		this.emailSender = emailSender;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.rateLimiter = rateLimiter;
		this.guard = guard;
		this.limits = limits;
	}

	@Transactional
	public void sendVerificationCode(SendCodeRequest request, String clientIp) {
		String email = normalize(request.email());
		limitVerificationSends(email, clientIp);
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");
		}
		String code = String.format("%06d", random.nextInt(1_000_000));
		EmailVerification verification = verificationRepository.findByEmail(email)
				.orElseGet(() -> new EmailVerification(email));
		verification.issue(code, CODE_VALID_FOR);
		verificationRepository.save(verification);
		try {
			emailSender.sendVerificationCode(email, code);
		} catch (EmailSendException e) {
			// 발송에 실패하면 예외로 트랜잭션이 되돌아가 저장한 인증번호도 남지 않는다. 번호와 전체 주소는 로그에 남기지 않는다.
			log.warn("인증 메일 발송 실패: to={}, cause={}", EmailSendException.mask(email), e.getMessage());
			throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_SEND_FAILED",
					"인증 메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.");
		}
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
	public TokenResponse login(LoginRequest request, String clientIp) {
		String email = normalize(request.email());
		String pairKey = "login-fail:" + email + "|" + clientIp;
		String ipKey = "login-fail-ip:" + clientIp;
		Duration lock = Duration.ofMinutes(limits.getLogin().getLockMinutes());
		guard.assertNotLocked(pairKey, limits.getLogin().getMaxFailures(), lock);
		guard.assertNotLocked(ipKey, limits.getLogin().getIpMaxFailures(), lock);

		User user = userRepository.findByEmail(email)
				.filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
				.orElse(null);
		if (user == null) {
			guard.recordFailure(pairKey, limits.getLogin().getMaxFailures(), lock);
			guard.recordFailure(ipKey, limits.getLogin().getIpMaxFailures(), lock);
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
					"이메일 또는 비밀번호가 일치하지 않습니다.");
		}
		guard.clear(pairKey);
		return jwtTokenService.issue(user.getId());
	}

	/** 같은 이메일 쿨다운, 이메일별·IP별 시간당 발송 횟수를 확인한다. 메일 폭탄과 인증번호 무차별 시도를 막는다. */
	private void limitVerificationSends(String email, String clientIp) {
		int cooldown = limits.getEmailSend().getCooldownSeconds();
		if (cooldown > 0) {
			guard.assertAllowed(rateLimiter.tryAcquire("email-send-cooldown:" + email, 1, Duration.ofSeconds(cooldown)));
		}
		guard.assertAllowed(rateLimiter.tryAcquire("email-send-hourly:" + email, limits.getEmailSend().getHourlyMax(),
				Duration.ofHours(1)));
		guard.assertAllowed(rateLimiter.tryAcquire("email-send-ip:" + clientIp, limits.getEmailSend().getIpHourlyMax(),
				Duration.ofHours(1)));
	}

	private BusinessException invalidCode() {
		return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE",
				"인증번호가 올바르지 않거나 만료되었습니다.");
	}

	private String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
