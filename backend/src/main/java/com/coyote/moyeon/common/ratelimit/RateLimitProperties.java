package com.coyote.moyeon.common.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 요청 제한 값. 기본값은 운영 기준이고, 테스트 프로필에서만 크게 풀어 쓴다. */
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
public class RateLimitProperties {

	private final EmailSend emailSend = new EmailSend();
	private final Login login = new Login();
	private final Ip ip = new Ip();

	@Getter
	@Setter
	public static class EmailSend {
		/** 같은 이메일로 다시 보내려면 기다려야 하는 시간(초). 0 이하면 쿨다운을 쓰지 않는다. */
		private int cooldownSeconds = 60;
		/** 같은 이메일로 1시간 동안 보낼 수 있는 최대 횟수 */
		private int hourlyMax = 5;
		/** 같은 IP에서 1시간 동안 보낼 수 있는 최대 횟수(여러 이메일로 보내는 메일 폭탄 방지) */
		private int ipHourlyMax = 20;
	}

	@Getter
	@Setter
	public static class Login {
		/** 같은 이메일+IP에서 연속 실패를 허용하는 횟수. 넘으면 lockMinutes 동안 잠근다. */
		private int maxFailures = 5;
		/** 같은 IP에서 실패를 허용하는 횟수(이메일을 바꿔 가며 시도하는 경우) */
		private int ipMaxFailures = 30;
		private int lockMinutes = 15;
	}

	@Getter
	@Setter
	public static class Ip {
		/** /api/auth/** 에 대해 IP당 분당 허용 요청 수 */
		private int authPerMinute = 30;
		/** /api/invites/** 에 대해 IP당 분당 허용 요청 수 */
		private int invitePerMinute = 120;
	}
}
