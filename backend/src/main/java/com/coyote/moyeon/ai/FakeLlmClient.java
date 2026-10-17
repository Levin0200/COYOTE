package com.coyote.moyeon.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 개발·테스트용 고정 응답(app.ai.provider=fake). 프롬프트의 후보 번호 중 홀수 번째(1, 3, 5...)를 먼저 고르고
 * 모자라면 짝수 번째로 채운다. 실제 AI를 호출하지 않는다.
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "fake")
public class FakeLlmClient implements LlmClient {

	private static final Pattern CANDIDATE = Pattern.compile("(?m)^(\\d+)\\|");
	private static final Pattern PICK_COUNT = Pattern.compile("고를 개수: (\\d+)");

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public String complete(String system, String user, int maxOutputTokens) {
		List<Integer> numbers = new ArrayList<>();
		Matcher candidates = CANDIDATE.matcher(user);
		while (candidates.find()) {
			numbers.add(Integer.parseInt(candidates.group(1)));
		}
		Matcher count = PICK_COUNT.matcher(user);
		int pick = count.find() ? Integer.parseInt(count.group(1)) : 3;

		List<Integer> ordered = new ArrayList<>();
		for (int i = 0; i < numbers.size(); i += 2) {
			ordered.add(numbers.get(i));
		}
		for (int i = 1; i < numbers.size(); i += 2) {
			ordered.add(numbers.get(i));
		}
		StringBuilder json = new StringBuilder("{\"picks\":[");
		for (int i = 0; i < Math.min(pick, ordered.size()); i++) {
			if (i > 0) {
				json.append(',');
			}
			json.append("{\"n\":").append(ordered.get(i)).append(",\"why\":\"테스트 이유 ").append(ordered.get(i)).append("\"}");
		}
		return json.append("]}").toString();
	}
}
