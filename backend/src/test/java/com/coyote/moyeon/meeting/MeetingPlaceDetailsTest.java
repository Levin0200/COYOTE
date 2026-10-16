package com.coyote.moyeon.meeting;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/**
 * 장소를 이름만이 아니라 장소 ID로 주고받는 흐름. 테스트에서는 fake 제공자를 쓰며, "fake-N"은 N번째 가짜 장소다
 * (예: fake-1 = "테스트백반집 1", fake-2 = "테스트치킨집 2").
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeetingPlaceDetailsTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final String GUEST_ID_HEADER = "X-Guest-Id";
	private static final AtomicInteger SEQ = new AtomicInteger();

	private static final String RECOMMEND_POLICY = """
			{"mode":"RECOMMEND","region":"송도","radius":3,"keyword":"카페",
			 "options":[{"name":"내가 마음대로 쓴 이름","distance":"0.5km","placeId":"fake-1"},
			            {"name":"다른 이름","distance":"1.0km","placeId":"fake-2"}]}
			""";

	@Autowired MockMvc mockMvc;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired JwtTokenService jwtTokenService;

	private record Created(long id, String inviteToken, String hostToken) {
	}

	private String newHostToken() {
		User user = userRepository.save(new User("place-detail-host-" + SEQ.incrementAndGet() + "@example.com",
				passwordEncoder.encode("password123"), "호스트", null));
		return jwtTokenService.issue(user.getId()).accessToken();
	}

	private static String day(int plusDays) {
		return LocalDate.now(KST).plusDays(plusDays).toString();
	}

	private static Instant validDue() {
		Instant later = Instant.now().plus(2, ChronoUnit.DAYS);
		return Instant.ofEpochSecond((later.getEpochSecond() / 600 + 1) * 600);
	}

	private String createBody(String policy, String placeMode, String placeFields) {
		return """
				{"title":"장소 상세","description":"설명","responseDeadline":"%s","linkExpirationDate":null,
				 "candidateStartDate":"%s","candidateEndDate":"%s","placePolicy":%s,
				 "selectedDates":["%s"],"placeMode":"%s"%s}
				""".formatted(validDue(), day(1), day(10), policy, day(2), placeMode, placeFields);
	}

	private ResultActions create(String hostToken, String body) throws Exception {
		return mockMvc.perform(post("/api/meetings").header("Authorization", "Bearer " + hostToken)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private Created createMeeting(String policy, String placeMode, String placeFields) throws Exception {
		String host = newHostToken();
		String response = create(host, createBody(policy, placeMode, placeFields)).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return new Created(((Number) JsonPath.read(response, "$.meetingId")).longValue(), JsonPath.read(response, "$.inviteToken"),
				host);
	}

	private Created createRecommendMeeting() throws Exception {
		return createMeeting(RECOMMEND_POLICY, "RECOMMEND", ",\"placeId\":\"fake-1\"");
	}

	private Created createOpenMeeting() throws Exception {
		return createMeeting("{\"mode\":\"OPEN\"}", "OPEN", "");
	}

	private ResultActions respond(String inviteToken, String guestId, String mode, String placeFields) throws Exception {
		return mockMvc.perform(put("/api/invites/" + inviteToken + "/response").header(GUEST_ID_HEADER, guestId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"게스트\",\"selectedDates\":[\"" + day(2) + "\"],\"placeMode\":\"" + mode + "\"" + placeFields + "}"));
	}

	private ResultActions closeAndGetResult(Created meeting) throws Exception {
		mockMvc.perform(post("/api/meetings/" + meeting.id() + "/close").header("Authorization", "Bearer " + meeting.hostToken()))
				.andExpect(status().isOk());
		return mockMvc.perform(get("/api/meetings/" + meeting.id() + "/result").header("Authorization", "Bearer " + meeting.hostToken()));
	}

	@Test
	void recommendOptionsAreStoredWithTheOfficialDetailsInsteadOfTheClientValues() throws Exception {
		String host = newHostToken();

		create(host, createBody(RECOMMEND_POLICY, "RECOMMEND", ",\"placeId\":\"fake-1\"")).andExpect(status().isCreated())
				.andExpect(jsonPath("$.placePolicy.options", hasSize(2)))
				.andExpect(jsonPath("$.placePolicy.options[0].name").value("테스트백반집 1"))
				.andExpect(jsonPath("$.placePolicy.options[0].placeId").value("fake-1"))
				.andExpect(jsonPath("$.placePolicy.options[0].address").value("테스트시 테스트로 1"))
				.andExpect(jsonPath("$.placePolicy.options[0].category").value("음식 > 한식 > 백반/한정식"))
				.andExpect(jsonPath("$.placePolicy.options[0].latitude").isNumber())
				.andExpect(jsonPath("$.placePolicy.options[0].longitude").isNumber())
				.andExpect(jsonPath("$.placePolicy.options[0].distance").value("0.5km"))
				.andExpect(jsonPath("$.placePolicy.options[1].name").value("테스트치킨집 2"))
				// 호스트가 고른 후보의 좌표가 호스트 응답에 붙는다.
				.andExpect(jsonPath("$.myResponse.placePreference").value("테스트백반집 1"))
				.andExpect(jsonPath("$.myResponse.place.id").value("fake-1"))
				.andExpect(jsonPath("$.myResponse.place.latitude").isNumber());
	}

	@Test
	void optionsWithoutAPlaceIdKeepWorkingAsNamesOnly() throws Exception {
		Created legacy = createMeeting("""
				{"mode":"RECOMMEND","region":"송도","radius":3,"options":[{"name":"카페 A","distance":"0.5km"}]}
				""", "RECOMMEND", ",\"placePreference\":\"카페 A\"");

		mockMvc.perform(get("/api/invites/" + legacy.inviteToken())).andExpect(status().isOk())
				.andExpect(jsonPath("$.placePolicy.options[0].name").value("카페 A"))
				.andExpect(jsonPath("$.placePolicy.options[0].placeId").value(nullValue()))
				.andExpect(jsonPath("$.placePolicy.options[0].latitude").value(nullValue()));
		respond(legacy.inviteToken(), UUID.randomUUID().toString(), "RECOMMEND", ",\"placePreference\":\"카페 A\"")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.placePreference").value("카페 A"))
				.andExpect(jsonPath("$.place").value(nullValue()));
	}

	@Test
	void aGuestPicksARecommendedOptionByPlaceIdAndGetsItsCoordinates() throws Exception {
		Created meeting = createRecommendMeeting();
		String guestId = UUID.randomUUID().toString();

		respond(meeting.inviteToken(), guestId, "RECOMMEND", ",\"placeId\":\"fake-2\"").andExpect(status().isCreated())
				.andExpect(jsonPath("$.placePreference").value("테스트치킨집 2"))
				.andExpect(jsonPath("$.place.id").value("fake-2"))
				.andExpect(jsonPath("$.place.address").value("테스트시 테스트로 2"))
				.andExpect(jsonPath("$.place.latitude").isNumber());
		mockMvc.perform(get("/api/invites/" + meeting.inviteToken() + "/response/me").header(GUEST_ID_HEADER, guestId))
				.andExpect(status().isOk()).andExpect(jsonPath("$.place.id").value("fake-2"));
	}

	@Test
	void aGuestCanStillPickAnOptionByItsOfficialName() throws Exception {
		Created meeting = createRecommendMeeting();

		respond(meeting.inviteToken(), UUID.randomUUID().toString(), "RECOMMEND", ",\"placePreference\":\"테스트치킨집 2\"")
				.andExpect(status().isCreated()).andExpect(jsonPath("$.place.id").value("fake-2"));
	}

	@Test
	void aPlaceThatIsNotAnOptionCannotBeChosen() throws Exception {
		Created meeting = createRecommendMeeting();

		// fake-9는 실제로 있는 장소지만 이 모임의 추천 후보가 아니다.
		respond(meeting.inviteToken(), UUID.randomUUID().toString(), "RECOMMEND", ",\"placeId\":\"fake-9\"")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void unknownOrMalformedPlaceIdsAreRejectedBeforeAnythingIsSaved() throws Exception {
		String host = newHostToken();
		String unknownOption = RECOMMEND_POLICY.replace("fake-2", "fake-9999");
		create(host, createBody(unknownOption, "RECOMMEND", ",\"placeId\":\"fake-1\"")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));

		String malformed = RECOMMEND_POLICY.replace("fake-2", "bad id!");
		create(host, createBody(malformed, "RECOMMEND", ",\"placeId\":\"fake-1\"")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		mockMvc.perform(get("/api/meetings").header("Authorization", "Bearer " + host)).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void theSamePlaceCannotBeAnOptionTwice() throws Exception {
		String duplicated = RECOMMEND_POLICY.replace("fake-2", "fake-1");

		create(newHostToken(), createBody(duplicated, "RECOMMEND", ",\"placeId\":\"fake-1\"")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void aProposedPlaceUsesTheOfficialNameAndCoordinates() throws Exception {
		Created meeting = createOpenMeeting();

		respond(meeting.inviteToken(), UUID.randomUUID().toString(), "PROPOSE",
				",\"placePreference\":\"클라이언트가 보낸 이름\",\"placeId\":\"fake-5\"").andExpect(status().isCreated())
				.andExpect(jsonPath("$.placePreference").value("테스트파스타집 5"))
				.andExpect(jsonPath("$.place.id").value("fake-5"))
				.andExpect(jsonPath("$.place.address").value("테스트시 테스트로 5"));
	}

	@Test
	void aProposalWithOnlyAPlaceIdIsEnough() throws Exception {
		Created meeting = createOpenMeeting();

		respond(meeting.inviteToken(), UUID.randomUUID().toString(), "PROPOSE", ",\"placeId\":\"fake-5\"")
				.andExpect(status().isCreated()).andExpect(jsonPath("$.place.id").value("fake-5"));
	}

	@Test
	void aProposalWithNeitherANameNorAPlaceIdIsRejected() throws Exception {
		Created meeting = createOpenMeeting();

		respond(meeting.inviteToken(), UUID.randomUUID().toString(), "PROPOSE", "")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void theResultCountsByPlaceIdAndReturnsTheStructuredPlace() throws Exception {
		Created meeting = createOpenMeeting();
		for (int i = 0; i < 2; i++) {
			respond(meeting.inviteToken(), UUID.randomUUID().toString(), "PROPOSE", ",\"placeId\":\"fake-5\"").andExpect(status().isCreated());
		}
		respond(meeting.inviteToken(), UUID.randomUUID().toString(), "PROPOSE", ",\"placeId\":\"fake-6\"").andExpect(status().isCreated());

		closeAndGetResult(meeting).andExpect(status().isOk())
				.andExpect(jsonPath("$.decisionType").value("MAJORITY"))
				.andExpect(jsonPath("$.placeVoteCount").value(2))
				.andExpect(jsonPath("$.selectedPlaces", hasSize(1)))
				.andExpect(jsonPath("$.selectedPlaceDetails", hasSize(1)))
				.andExpect(jsonPath("$.selectedPlaceDetails[0].id").value("fake-5"))
				.andExpect(jsonPath("$.selectedPlaceDetails[0].address").value("테스트시 테스트로 5"))
				.andExpect(jsonPath("$.selectedPlaceDetails[0].latitude").isNumber())
				.andExpect(jsonPath("$.selectedPlaceDetails[0].longitude").isNumber());
	}

	@Test
	void tiedPlacesAreAllReturnedWithTheirDetails() throws Exception {
		Created meeting = createRecommendMeeting();
		respond(meeting.inviteToken(), UUID.randomUUID().toString(), "RECOMMEND", ",\"placeId\":\"fake-2\"").andExpect(status().isCreated());

		// 호스트는 fake-1, 게스트는 fake-2를 골라 1:1 동률이다.
		closeAndGetResult(meeting).andExpect(status().isOk())
				.andExpect(jsonPath("$.decisionType").value("TIE_BREAK"))
				.andExpect(jsonPath("$.selectedPlaces", contains("테스트백반집 1", "테스트치킨집 2")))
				.andExpect(jsonPath("$.selectedPlaceDetails[0].id").value("fake-1"))
				.andExpect(jsonPath("$.selectedPlaceDetails[1].id").value("fake-2"))
				.andExpect(jsonPath("$.selectedPlace").value("테스트백반집 1 · 테스트치킨집 2"));
	}

	@Test
	void resultsFromNameOnlyResponsesStillWork() throws Exception {
		Created meeting = createMeeting("{\"mode\":\"OPEN\"}", "OPEN", "");
		respond(meeting.inviteToken(), UUID.randomUUID().toString(), "PROPOSE", ",\"placePreference\":\"이름만 있는 장소\"")
				.andExpect(status().isCreated());

		closeAndGetResult(meeting).andExpect(status().isOk())
				.andExpect(jsonPath("$.selectedPlaces", contains("이름만 있는 장소")))
				.andExpect(jsonPath("$.selectedPlaceDetails[0].name").value("이름만 있는 장소"))
				.andExpect(jsonPath("$.selectedPlaceDetails[0].id").value(nullValue()))
				.andExpect(jsonPath("$.selectedPlaceDetails[0].latitude").value(nullValue()));
	}

	@Test
	void theHostCanResaveTheSameOptionsAfterGuestsRespondedButNotChangeThem() throws Exception {
		Created meeting = createRecommendMeeting();
		respond(meeting.inviteToken(), UUID.randomUUID().toString(), "RECOMMEND", ",\"placeId\":\"fake-2\"").andExpect(status().isCreated());

		String same = """
				{"placePolicy":%s,"selectedDates":["%s"],"placeMode":"RECOMMEND","placeId":"fake-1"}
				""".formatted(RECOMMEND_POLICY, day(3));
		mockMvc.perform(put("/api/meetings/" + meeting.id() + "/response").header("Authorization", "Bearer " + meeting.hostToken())
				.contentType(MediaType.APPLICATION_JSON).content(same)).andExpect(status().isOk());

		String changed = same.replace("fake-2", "fake-3");
		mockMvc.perform(put("/api/meetings/" + meeting.id() + "/response").header("Authorization", "Bearer " + meeting.hostToken())
				.contentType(MediaType.APPLICATION_JSON).content(changed)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("POLICY_LOCKED"));
	}
}
