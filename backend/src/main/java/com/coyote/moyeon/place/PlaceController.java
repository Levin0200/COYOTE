package com.coyote.moyeon.place;

import com.coyote.moyeon.ai.AiProperties;
import com.coyote.moyeon.common.ratelimit.RateLimitGuard;
import com.coyote.moyeon.common.ratelimit.RateLimitProperties;
import com.coyote.moyeon.common.ratelimit.RateLimiter;
import com.coyote.moyeon.meeting.MeetingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 장소 검색. 호스트는 로그인(JWT)으로, 게스트는 초대 토큰과 X-Guest-Id로 호출한다.
 * 게스트는 호스트가 장소 결정을 게스트 제안에 열어 둔 모임(건너뛰기)에서 마감 전까지만 검색할 수 있다.
 * 외부 API 호출량을 지키려고 사용자(호스트)·IP(게스트)별로 호출 횟수를 제한한다.
 */
@RestController
public class PlaceController {

	private static final int RECOMMEND_POOL_SIZE = 20;
	private static final int RECOMMEND_COUNT = 6;

	private final PlaceSearchService placeSearchService;
	private final PlaceRankingService rankingService;
	private final AiProperties aiProperties;
	private final MeetingService meetingService;
	private final RateLimiter rateLimiter;
	private final RateLimitGuard guard;
	private final RateLimitProperties limits;

	public PlaceController(PlaceSearchService placeSearchService, PlaceRankingService rankingService,
			AiProperties aiProperties, MeetingService meetingService, RateLimiter rateLimiter, RateLimitGuard guard,
			RateLimitProperties limits) {
		this.placeSearchService = placeSearchService;
		this.rankingService = rankingService;
		this.aiProperties = aiProperties;
		this.meetingService = meetingService;
		this.rateLimiter = rateLimiter;
		this.guard = guard;
		this.limits = limits;
	}

	@GetMapping("/api/places/search")
	public PlaceSearchResponse searchAsHost(@AuthenticationPrincipal Jwt jwt,
			@RequestParam("latitude") double latitude, @RequestParam("longitude") double longitude,
			@RequestParam(name = "radiusMeters", defaultValue = "1000") int radiusMeters,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "limit", defaultValue = "" + PlaceSearchService.DEFAULT_LIMIT) int limit) {
		limitSearches("place-search:user:" + jwt.getSubject());
		return search(latitude, longitude, radiusMeters, keyword, limit);
	}

	@GetMapping("/api/invites/{inviteToken}/places/search")
	public PlaceSearchResponse searchAsGuest(@PathVariable("inviteToken") String inviteToken,
			@RequestHeader("X-Guest-Id") String guestId, HttpServletRequest request,
			@RequestParam("latitude") double latitude, @RequestParam("longitude") double longitude,
			@RequestParam(name = "radiusMeters", defaultValue = "1000") int radiusMeters,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "limit", defaultValue = "" + PlaceSearchService.DEFAULT_LIMIT) int limit) {
		meetingService.assertGuestMayProposePlaces(inviteToken, guestId);
		limitSearches("place-search:ip:" + request.getRemoteAddr());
		return search(latitude, longitude, radiusMeters, keyword, limit);
	}

	/**
	 * 장소 추천(호스트). 근처 후보 20곳을 찾은 뒤 AI가 모임 성격에 맞게 6곳을 골라 순서대로 돌려준다.
	 * AI를 쓸 수 없거나 실패하면 규칙(가까운 순서·업종 다양성)으로 고른다. 호출 횟수는 사용자별로 제한한다.
	 */
	@PostMapping("/api/places/recommend")
	public RecommendationResponse recommend(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RecommendRequest request) {
		guard.assertAllowed(rateLimiter.tryAcquire("ai-recommend:user:" + jwt.getSubject(),
				aiProperties.getUserHourlyLimit(), Duration.ofHours(1)));
		PlaceSearchService.Result pool = placeSearchService.search(request.latitude(), request.longitude(),
				request.radiusOrDefault(), request.keyword(), RECOMMEND_POOL_SIZE);
		Ranking ranking = rankingService.rank(new RankRequest(request.title(), request.description(),
				request.participantCountOrDefault(), pool.places(), RECOMMEND_COUNT));
		return RecommendationResponse.from(pool, ranking);
	}

	private PlaceSearchResponse search(double latitude, double longitude, int radiusMeters, String keyword, int limit) {
		return PlaceSearchResponse.from(placeSearchService.search(latitude, longitude, radiusMeters, keyword, limit));
	}

	private void limitSearches(String key) {
		RateLimitProperties.PlaceSearch config = limits.getPlaceSearch();
		guard.assertAllowed(rateLimiter.tryAcquire(key, config.getMax(), Duration.ofMinutes(config.getWindowMinutes())));
	}
}
