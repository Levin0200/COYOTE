package com.coyote.moyeon.place;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coyote.moyeon.auth.JwtTokenService;
import com.coyote.moyeon.user.User;
import com.coyote.moyeon.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlaceControllerTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final String GUEST_ID_HEADER = "X-Guest-Id";
	private static final AtomicInteger SEQ = new AtomicInteger();

	@Autowired MockMvc mockMvc;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired JwtTokenService jwtTokenService;

	private String newHostToken() {
		User user = userRepository.save(new User("place-host-" + SEQ.incrementAndGet() + "@example.com",
				passwordEncoder.encode("password123"), "호스트", null));
		return jwtTokenService.issue(user.getId()).accessToken();
	}

	private static String day(int plusDays) {
		return LocalDate.now(KST).plusDays(plusDays).toString();
	}

	private record Created(long id, String inviteToken) {
	}

	private Created createMeeting(String token, String policy, String placeMode, String placePreference) throws Exception {
		Instant later = Instant.now().plus(2, ChronoUnit.DAYS);
		Instant due = Instant.ofEpochSecond((later.getEpochSecond() / 600 + 1) * 600);
		String body = """
				{"title":"장소 검색","description":"설명","responseDeadline":"%s","linkExpirationDate":null,
				 "candidateStartDate":"%s","candidateEndDate":"%s","placePolicy":%s,
				 "selectedDates":["%s"],"placeMode":"%s","region":"송도","placePreference":%s}
				""".formatted(due, day(1), day(10), policy, day(2), placeMode,
				placePreference == null ? "null" : "\"" + placePreference + "\"");
		String response = mockMvc.perform(post("/api/meetings").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return new Created(((Number) JsonPath.read(response, "$.meetingId")).longValue(), JsonPath.read(response, "$.inviteToken"));
	}

	private Created createOpenMeeting(String token) throws Exception {
		return createMeeting(token, "{\"mode\":\"OPEN\"}", "OPEN", null);
	}

	private static MockHttpServletRequestBuilder withSearchParams(MockHttpServletRequestBuilder request) {
		return request.param("latitude", "37.4979").param("longitude", "127.0276").param("radiusMeters", "1000");
	}

	private ResultActions hostSearch(String token, String keyword) throws Exception {
		MockHttpServletRequestBuilder request = withSearchParams(get("/api/places/search")).param("limit", "5");
		if (keyword != null) {
			request.param("keyword", keyword);
		}
		if (token != null) {
			request.header("Authorization", "Bearer " + token);
		}
		return mockMvc.perform(request);
	}

	private ResultActions guestSearch(String inviteToken, String guestId) throws Exception {
		MockHttpServletRequestBuilder request = withSearchParams(get("/api/invites/" + inviteToken + "/places/search"))
				.param("limit", "5");
		if (guestId != null) {
			request.header(GUEST_ID_HEADER, guestId);
		}
		return mockMvc.perform(request);
	}

	@Test
	void hostGetsNearbyPlacesSortedByDistance() throws Exception {
		// 테스트용 가짜 데이터에는 반경 1km 안에 카페가 3곳뿐이라 limit(5)보다 적게 나온다.
		hostSearch(newHostToken(), "카페").andExpect(status().isOk())
				.andExpect(jsonPath("$.places", hasSize(3)))
				.andExpect(jsonPath("$.places[0].id").isNotEmpty())
				.andExpect(jsonPath("$.places[0].name", containsString("카페")))
				.andExpect(jsonPath("$.places[0].category", containsString("카페")))
				.andExpect(jsonPath("$.places[0].address").isNotEmpty())
				.andExpect(jsonPath("$.places[0].latitude").isNumber())
				.andExpect(jsonPath("$.places[0].longitude").isNumber())
				.andExpect(jsonPath("$.places[0].distanceMeters", lessThanOrEqualTo(1000)))
				.andExpect(jsonPath("$.places[1].distanceMeters", greaterThan(0)))
				.andExpect(jsonPath("$.appliedCategories", contains("카페")))
				.andExpect(jsonPath("$.ignoredKeywords", hasSize(0)));
	}

	@Test
	void unknownWordsAreReportedAsIgnored() throws Exception {
		hostSearch(newHostToken(), "조용한 카페").andExpect(status().isOk())
				.andExpect(jsonPath("$.appliedCategories", contains("카페")))
				.andExpect(jsonPath("$.ignoredKeywords", contains("조용한")));
	}

	@Test
	void searchRequiresLogin() throws Exception {
		hostSearch(null, "카페").andExpect(status().isUnauthorized());
		hostSearch("not.a.token", "카페").andExpect(status().isUnauthorized());
	}

	@Test
	void missingOrInvalidParametersAreBadRequests() throws Exception {
		String token = newHostToken();
		mockMvc.perform(get("/api/places/search").header("Authorization", "Bearer " + token).param("longitude", "127.0"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		mockMvc.perform(get("/api/places/search").header("Authorization", "Bearer " + token)
				.param("latitude", "abc").param("longitude", "127.0"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		mockMvc.perform(get("/api/places/search").header("Authorization", "Bearer " + token)
				.param("latitude", "48.85").param("longitude", "2.35"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void guestCanSearchWhenTheHostLeftPlacesOpen() throws Exception {
		Created meeting = createOpenMeeting(newHostToken());

		guestSearch(meeting.inviteToken(), UUID.randomUUID().toString()).andExpect(status().isOk())
				.andExpect(jsonPath("$.places", hasSize(5)));
	}

	@Test
	void guestSearchNeedsAValidGuestId() throws Exception {
		Created meeting = createOpenMeeting(newHostToken());

		guestSearch(meeting.inviteToken(), null).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_GUEST_ID"));
		guestSearch(meeting.inviteToken(), "not-a-uuid").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_GUEST_ID"));
	}

	@Test
	void unknownInviteTokenIsNotFound() throws Exception {
		guestSearch("no-such-token", UUID.randomUUID().toString()).andExpect(status().isNotFound());
	}

	@Test
	void guestCannotSearchWhenThePlaceIsFixedOrRecommended() throws Exception {
		String host = newHostToken();
		Created recommended = createMeeting(host, """
				{"mode":"RECOMMEND","region":"송도","radius":3,"options":[{"name":"카페 A","distance":"0.5km"}]}
				""", "RECOMMEND", "카페 A");
		Created fixed = createMeeting(host, "{\"mode\":\"FIXED\",\"fixedPlace\":\"강남역 3번 출구\"}", "FIXED", "강남역 3번 출구");

		for (Created meeting : new Created[] { recommended, fixed }) {
			guestSearch(meeting.inviteToken(), UUID.randomUUID().toString()).andExpect(status().isConflict())
					.andExpect(jsonPath("$.code").value("PLACE_SEARCH_NOT_ALLOWED"));
		}
	}

	@Test
	void guestCannotSearchAfterTheMeetingIsClosed() throws Exception {
		String host = newHostToken();
		Created meeting = createOpenMeeting(host);
		mockMvc.perform(post("/api/meetings/" + meeting.id() + "/close").header("Authorization", "Bearer " + host))
				.andExpect(status().isOk());

		guestSearch(meeting.inviteToken(), UUID.randomUUID().toString()).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETING_CLOSED"));
	}
}
