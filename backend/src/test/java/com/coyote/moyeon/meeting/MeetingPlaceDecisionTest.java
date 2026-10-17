package com.coyote.moyeon.meeting;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 마감 이후의 장소 결정: 공동 1위와 흩어진 제안은 호스트가 후보 중에서 고르고, 시한이 지나면 자동으로 정한다.
 * 테스트에서는 fake 장소 제공자를 쓰며 "fake-N"은 N번째 가짜 장소다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeetingPlaceDecisionTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final String GUEST_ID_HEADER = "X-Guest-Id";
	private static final AtomicInteger SEQ = new AtomicInteger();

	private static final String RECOMMEND_POLICY = """
			{"mode":"RECOMMEND","region":"송도","radius":3,"keyword":"카페",
			 "options":[{"name":"a","distance":"0.5km","placeId":"fake-1"},
			            {"name":"b","distance":"1.0km","placeId":"fake-2"}]}
			""";

	@Autowired MockMvc mockMvc;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired JwtTokenService jwtTokenService;
	@Autowired JdbcTemplate jdbc;
	@Autowired PlaceDecisionScheduler scheduler;

	private record Created(long id, String inviteToken, String hostToken) {
	}

	private String newHostToken() {
		User user = userRepository.save(new User("decision-host-" + SEQ.incrementAndGet() + "@example.com",
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

	private Created createMeeting(String policy, String placeMode, String placeFields) throws Exception {
		String host = newHostToken();
		String body = """
				{"title":"장소 결정","description":"설명","responseDeadline":"%s","linkExpirationDate":null,
				 "candidateStartDate":"%s","candidateEndDate":"%s","placePolicy":%s,
				 "selectedDates":["%s"],"placeMode":"%s"%s}
				""".formatted(validDue(), day(1), day(10), policy, day(2), placeMode, placeFields);
		String response = mockMvc.perform(post("/api/meetings").header("Authorization", "Bearer " + host)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return new Created(((Number) JsonPath.read(response, "$.meetingId")).longValue(), JsonPath.read(response, "$.inviteToken"),
				host);
	}

	private Created createRecommendTie() throws Exception {
		Created meeting = createMeeting(RECOMMEND_POLICY, "RECOMMEND", ",\"placeId\":\"fake-1\"");
		guestRespond(meeting, "RECOMMEND", "fake-2");
		return meeting;
	}

	private Created createOpenMeeting() throws Exception {
		return createMeeting("{\"mode\":\"OPEN\"}", "OPEN", "");
	}

	private void guestRespond(Created meeting, String mode, String placeId) throws Exception {
		mockMvc.perform(put("/api/invites/" + meeting.inviteToken() + "/response")
				.header(GUEST_ID_HEADER, UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"게스트\",\"selectedDates\":[\"" + day(2) + "\"],\"placeMode\":\"" + mode
						+ "\",\"placeId\":\"" + placeId + "\"}"))
				.andExpect(status().isCreated());
	}

	private void close(Created meeting) throws Exception {
		mockMvc.perform(post("/api/meetings/" + meeting.id() + "/close").header("Authorization", "Bearer " + meeting.hostToken()))
				.andExpect(status().isOk());
	}

	private ResultActions hostResult(Created meeting) throws Exception {
		return mockMvc.perform(get("/api/meetings/" + meeting.id() + "/result").header("Authorization", "Bearer " + meeting.hostToken()));
	}

	private ResultActions guestResult(Created meeting) throws Exception {
		return mockMvc.perform(get("/api/invites/" + meeting.inviteToken() + "/result"));
	}

	private ResultActions choose(String token, long meetingId, String body) throws Exception {
		return mockMvc.perform(post("/api/meetings/" + meetingId + "/result/place").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private void passDeadline(long meetingId) {
		jdbc.update("update final_result set host_decision_deadline = ? where meeting_id = ?",
				OffsetDateTime.ofInstant(Instant.now().minusSeconds(60), ZoneOffset.UTC), meetingId);
	}

	@Test
	void hostChoosesOneOfTheTiedPlaces() throws Exception {
		Created meeting = createRecommendTie();
		close(meeting);

		hostResult(meeting).andExpect(jsonPath("$.placeStatus").value("AWAITING_HOST"))
				.andExpect(jsonPath("$.placeCandidates", hasSize(2)));

		choose(meeting.hostToken(), meeting.id(), "{\"placeId\":\"fake-2\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.placeStatus").value("DECIDED"))
				.andExpect(jsonPath("$.decisionType").value("HOST_CHOICE"))
				.andExpect(jsonPath("$.selectedPlaces[0]").value("테스트치킨집 2"))
				.andExpect(jsonPath("$.selectedPlaceDetails[0].id").value("fake-2"))
				.andExpect(jsonPath("$.placeCandidates", hasSize(0)))
				.andExpect(jsonPath("$.hostDecisionDeadline").doesNotExist())
				.andExpect(jsonPath("$.reason").value(containsString("호스트가")));

		// 게스트에게도 확정된 장소가 보인다. 이미 정해졌으므로 다시 고를 수 없다.
		guestResult(meeting).andExpect(jsonPath("$.placeStatus").value("DECIDED"))
				.andExpect(jsonPath("$.selectedPlaceDetails[0].id").value("fake-2"));
		choose(meeting.hostToken(), meeting.id(), "{\"placeId\":\"fake-1\"}").andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PLACE_NOT_AWAITING"));
	}

	@Test
	void choosingIsRestrictedToTheHostAndToRealCandidates() throws Exception {
		Created meeting = createRecommendTie();

		choose(meeting.hostToken(), meeting.id(), "{\"placeId\":\"fake-1\"}").andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETING_NOT_CLOSED"));
		close(meeting);

		choose(meeting.hostToken(), meeting.id(), "{\"placeId\":\"fake-3\"}").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CANDIDATE"));
		choose(meeting.hostToken(), meeting.id(), "{}").andExpect(status().isBadRequest());
		choose(newHostToken(), meeting.id(), "{\"placeId\":\"fake-1\"}").andExpect(status().isForbidden());
		mockMvc.perform(post("/api/meetings/" + meeting.id() + "/result/place").contentType(MediaType.APPLICATION_JSON)
				.content("{\"placeId\":\"fake-1\"}")).andExpect(status().isUnauthorized());

		hostResult(meeting).andExpect(jsonPath("$.placeStatus").value("AWAITING_HOST"));
	}

	@Test
	void theFirstCandidateIsPickedAutomaticallyAfterTheDeadline() throws Exception {
		Created meeting = createRecommendTie();
		close(meeting);

		// 시한 전에는 아무것도 하지 않는다.
		assertEquals(0, scheduler.run());
		hostResult(meeting).andExpect(jsonPath("$.placeStatus").value("AWAITING_HOST"));

		passDeadline(meeting.id());
		assertEquals(1, scheduler.run());
		hostResult(meeting).andExpect(jsonPath("$.placeStatus").value("DECIDED"))
				.andExpect(jsonPath("$.decisionType").value("AUTO_PICK"))
				.andExpect(jsonPath("$.selectedPlaceDetails[0].id").value("fake-1"))
				.andExpect(jsonPath("$.reason").value(containsString("자동")));

		// 이미 정해진 모임은 다시 처리하지 않는다.
		assertEquals(0, scheduler.run());
	}

	@Test
	void anOpenMeetingWithoutProposalsEndsUndecided() throws Exception {
		Created meeting = createOpenMeeting();
		close(meeting);

		hostResult(meeting).andExpect(jsonPath("$.placeStatus").value("NONE"))
				.andExpect(jsonPath("$.selectedPlace").value("추후 협의"));
	}

	@Test
	void aSingleWinningProposalIsDecidedImmediately() throws Exception {
		Created meeting = createOpenMeeting();
		guestRespond(meeting, "PROPOSE", "fake-1");
		guestRespond(meeting, "PROPOSE", "fake-1");
		guestRespond(meeting, "PROPOSE", "fake-2");
		close(meeting);

		hostResult(meeting).andExpect(jsonPath("$.placeStatus").value("DECIDED"))
				.andExpect(jsonPath("$.decisionType").value("MAJORITY"))
				.andExpect(jsonPath("$.selectedPlaceDetails[0].id").value("fake-1"))
				.andExpect(jsonPath("$.placeVoteCount").value(2));
		mockMvc.perform(get("/api/meetings/" + meeting.id()).header("Authorization", "Bearer " + meeting.hostToken()))
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	@Test
	void scatteredProposalsBecomeThreeCandidatesForTheHost() throws Exception {
		Created meeting = createOpenMeeting();
		guestRespond(meeting, "PROPOSE", "fake-1");
		guestRespond(meeting, "PROPOSE", "fake-2");
		close(meeting);

		hostResult(meeting).andExpect(jsonPath("$.placeStatus").value("AWAITING_HOST"))
				.andExpect(jsonPath("$.decisionType").value("SPATIAL"))
				.andExpect(jsonPath("$.selectedPlaces", hasSize(0)))
				.andExpect(jsonPath("$.placeCandidates", hasSize(3)))
				.andExpect(jsonPath("$.hostDecisionDeadline").isNotEmpty())
				.andExpect(jsonPath("$.reason").value(containsString("호스트가 최종 장소를 고릅니다")));
		mockMvc.perform(get("/api/meetings/" + meeting.id()).header("Authorization", "Bearer " + meeting.hostToken()))
				.andExpect(jsonPath("$.status").value("COMPLETED"));
		guestResult(meeting).andExpect(jsonPath("$.placeStatus").value("AWAITING_HOST"))
				.andExpect(jsonPath("$.placeCandidates", hasSize(0)));

		String result = hostResult(meeting).andReturn().getResponse().getContentAsString();
		String firstId = JsonPath.read(result, "$.placeCandidates[0].id");
		choose(meeting.hostToken(), meeting.id(), "{\"placeId\":\"" + firstId + "\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.placeStatus").value("DECIDED"))
				.andExpect(jsonPath("$.decisionType").value("HOST_CHOICE"))
				.andExpect(jsonPath("$.selectedPlaceDetails[0].id").value(firstId));
	}

	@Test
	void aMeetingLeftPendingIsPickedUpAgainByTheScheduler() throws Exception {
		Created meeting = createOpenMeeting();
		guestRespond(meeting, "PROPOSE", "fake-1");
		guestRespond(meeting, "PROPOSE", "fake-2");
		close(meeting);

		// 후보를 만들던 중 서버가 멈춘 상태를 흉내 낸다.
		jdbc.update("delete from final_result_candidate where final_result_id in (select id from final_result where meeting_id = ?)", meeting.id());
		jdbc.update("update final_result set place_status = 'PENDING', host_decision_deadline = null, created_at = ? where meeting_id = ?",
				OffsetDateTime.ofInstant(Instant.now().minusSeconds(600), ZoneOffset.UTC), meeting.id());
		jdbc.update("update meeting set status = 'CLOSED' where id = ?", meeting.id());

		hostResult(meeting).andExpect(jsonPath("$.placeStatus").value("PENDING"))
				.andExpect(jsonPath("$.placeCandidates", hasSize(0)))
				.andExpect(jsonPath("$.selectedPlace").value("추후 협의"));

		assertEquals(1, scheduler.run());
		hostResult(meeting).andExpect(jsonPath("$.placeStatus").value("AWAITING_HOST"))
				.andExpect(jsonPath("$.placeCandidates", hasSize(3)));
		mockMvc.perform(get("/api/meetings/" + meeting.id()).header("Authorization", "Bearer " + meeting.hostToken()))
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	@Test
	void theMeetingListShowsThePlaceStatus() throws Exception {
		Created meeting = createRecommendTie();
		close(meeting);

		mockMvc.perform(get("/api/meetings").header("Authorization", "Bearer " + meeting.hostToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].result.placeStatus").value("AWAITING_HOST"))
				.andExpect(jsonPath("$[0].result.selectedPlace").value("추후 협의"));
	}
}
