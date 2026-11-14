package com.coyote.moyeon.meeting;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeetingResultTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final String GUEST_ID_HEADER = "X-Guest-Id";
	private static final AtomicInteger SEQ = new AtomicInteger();

	private static final String OPEN_POLICY = "{\"mode\":\"OPEN\"}";
	private static final String RECOMMEND_POLICY = """
			{"mode":"RECOMMEND","region":"송도","radius":3,"keyword":"카페",
			 "options":[{"name":"송도 카페 A","distance":"0.5km"},{"name":"송도 카페 B","distance":"1.0km"}]}
			""";

	@Autowired MockMvc mockMvc;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired JwtTokenService jwtTokenService;
	@Autowired JdbcTemplate jdbc;
	@Autowired MeetingCloseScheduler scheduler;

	private record Created(long id, String inviteToken) {
	}

	private String newHostToken() {
		User user = userRepository.save(new User("result-host-" + SEQ.incrementAndGet() + "@example.com",
				passwordEncoder.encode("password123"), "호스트", null));
		return jwtTokenService.issue(user.getId()).accessToken();
	}

	private static String day(int plusDays) {
		return LocalDate.now(KST).plusDays(plusDays).toString();
	}

	private static String dates(int... plusDays) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < plusDays.length; i++) {
			sb.append(i == 0 ? "" : ",").append('"').append(day(plusDays[i])).append('"');
		}
		return sb.toString();
	}

	private static String q(Object value) {
		return value == null ? "null" : "\"" + value + "\"";
	}

	private static Instant validDue() {
		Instant later = Instant.now().plus(2, ChronoUnit.DAYS);
		return Instant.ofEpochSecond((later.getEpochSecond() / 600 + 1) * 600);
	}

	private Created createMeeting(String token, String policy, String datesJson, String mode, String place) throws Exception {
		String body = """
				{"title":"결과 테스트","description":"설명","responseDeadline":"%s","linkExpirationDate":null,
				 "candidateStartDate":"%s","candidateEndDate":"%s","placePolicy":%s,
				 "selectedDates":[%s],"placeMode":"%s","region":"송도","placePreference":%s}
				""".formatted(validDue(), day(1), day(10), policy, datesJson, mode, q(place));
		String response = mockMvc.perform(post("/api/meetings").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return new Created(((Number) JsonPath.read(response, "$.meetingId")).longValue(), JsonPath.read(response, "$.inviteToken"));
	}

	private void guestRespond(String inviteToken, String datesJson, String mode, String place) throws Exception {
		String body = """
				{"nickname":"게스트","selectedDates":[%s],"placeMode":"%s","region":"송도","placePreference":%s}
				""".formatted(datesJson, mode, q(place));
		mockMvc.perform(put("/api/invites/" + inviteToken + "/response").header(GUEST_ID_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
	}

	private ResultActions close(String token, long id) throws Exception {
		return mockMvc.perform(post("/api/meetings/" + id + "/close").header("Authorization", "Bearer " + token));
	}

	private ResultActions hostResult(String token, long id) throws Exception {
		return mockMvc.perform(get("/api/meetings/" + id + "/result").header("Authorization", "Bearer " + token));
	}

	private ResultActions guestResult(String inviteToken) throws Exception {
		return mockMvc.perform(get("/api/invites/" + inviteToken + "/result"));
	}

	private void makeOverdue(long meetingId) {
		jdbc.update("update meeting set response_due_date = ? where id = ?",
				OffsetDateTime.ofInstant(Instant.now().minusSeconds(60), ZoneOffset.UTC), meetingId);
	}

	private int resultCount(long meetingId) {
		return jdbc.queryForObject("select count(*) from final_result where meeting_id = ?", Integer.class, meetingId);
	}

	@Test
	void manualClose_computesMajorityResult_andLocksTheMeeting() throws Exception {
		String host = newHostToken();
		Created meeting = createMeeting(host, RECOMMEND_POLICY, dates(2, 3), "RECOMMEND", "송도 카페 A");
		guestRespond(meeting.inviteToken(), dates(2), "RECOMMEND", "송도 카페 A");
		guestRespond(meeting.inviteToken(), dates(2, 4), "RECOMMEND", "송도 카페 B");

		// 마감 전에는 결과를 볼 수 없다.
		hostResult(host, meeting.id()).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETING_NOT_CLOSED"));
		guestResult(meeting.inviteToken()).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETING_NOT_CLOSED"));

		// 호스트만 마감할 수 있다.
		mockMvc.perform(post("/api/meetings/" + meeting.id() + "/close")).andExpect(status().isUnauthorized());
		close(newHostToken(), meeting.id()).andExpect(status().isForbidden());

		close(host, meeting.id()).andExpect(status().isOk())
				.andExpect(jsonPath("$.meetingId").value(meeting.id()))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.closed").value(true));
		assertTrue(jdbc.queryForObject("select manually_closed from meeting where id = ?", Boolean.class, meeting.id()));

		for (ResultActions result : new ResultActions[] { hostResult(host, meeting.id()), guestResult(meeting.inviteToken()) }) {
			result.andExpect(status().isOk())
					.andExpect(jsonPath("$.selectedDate").value(day(2)))
					.andExpect(jsonPath("$.selectedPlaces", hasSize(1)))
					.andExpect(jsonPath("$.selectedPlaces[0]").value("송도 카페 A"))
					.andExpect(jsonPath("$.selectedPlace").value("송도 카페 A"))
					.andExpect(jsonPath("$.decisionType").value("MAJORITY"))
					.andExpect(jsonPath("$.dateVoteCount").value(3))
					.andExpect(jsonPath("$.placeVoteCount").value(2))
					.andExpect(jsonPath("$.totalResponses").value(3))
					.andExpect(jsonPath("$.reason").value(containsString("가장 많은 참여자가 선택한 날짜")));
		}

		// 멱등: 다시 마감해도 결과는 하나뿐이다.
		close(host, meeting.id()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
		assertEquals(1, resultCount(meeting.id()));

		// 마감 후에는 응답을 바꿀 수 없다.
		mockMvc.perform(put("/api/invites/" + meeting.inviteToken() + "/response").header(GUEST_ID_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"늦은 게스트\",\"selectedDates\":[\"" + day(2) + "\"],\"placeMode\":\"RECOMMEND\",\"placePreference\":\"송도 카페 A\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEETING_CLOSED"));
		mockMvc.perform(put("/api/meetings/" + meeting.id() + "/response").header("Authorization", "Bearer " + host)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"placePolicy\":" + RECOMMEND_POLICY + ",\"selectedDates\":[\"" + day(2) + "\"],\"placeMode\":\"RECOMMEND\",\"placePreference\":\"송도 카페 A\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEETING_CLOSED"));
	}

	@Test
	void ties_pickNearestDate_andLeaveTheTiedPlacesToTheHost() throws Exception {
		String host = newHostToken();
		Created meeting = createMeeting(host, RECOMMEND_POLICY, dates(5), "RECOMMEND", "송도 카페 A");
		guestRespond(meeting.inviteToken(), dates(2), "RECOMMEND", "송도 카페 B");

		close(host, meeting.id()).andExpect(status().isOk());
		hostResult(host, meeting.id()).andExpect(status().isOk())
				.andExpect(jsonPath("$.selectedDate").value(day(2)))
				// 장소 공동 1위는 호스트가 고르므로 아직 확정된 장소가 없다.
				.andExpect(jsonPath("$.placeStatus").value("AWAITING_HOST"))
				.andExpect(jsonPath("$.selectedPlaces", hasSize(0)))
				.andExpect(jsonPath("$.selectedPlace").value("추후 협의"))
				.andExpect(jsonPath("$.hostDecisionDeadline").doesNotExist())
				.andExpect(jsonPath("$.placeCandidates", hasSize(2)))
				.andExpect(jsonPath("$.placeCandidates[0].name").value("송도 카페 A"))
				.andExpect(jsonPath("$.placeCandidates[1].name").value("송도 카페 B"))
				.andExpect(jsonPath("$.decisionType").value("TIE_BREAK"))
				.andExpect(jsonPath("$.dateVoteCount").value(1))
				.andExpect(jsonPath("$.placeVoteCount").value(1))
				.andExpect(jsonPath("$.totalResponses").value(2))
				.andExpect(jsonPath("$.reason").value(containsString("날짜 공동 1위")))
				.andExpect(jsonPath("$.reason").value(containsString("장소 공동 1위")));

		// 후보 목록은 호스트에게만 보인다.
		guestResult(meeting.inviteToken()).andExpect(status().isOk())
				.andExpect(jsonPath("$.placeStatus").value("AWAITING_HOST"))
				.andExpect(jsonPath("$.placeCandidates", hasSize(0)));
	}

	@Test
	void openPolicy_ignoresSkippedPlaces_andLetsTheHostChooseWhenEveryoneSkips() throws Exception {
		String host = newHostToken();
		Created proposed = createMeeting(host, OPEN_POLICY, dates(2), "OPEN", null);
		guestRespond(proposed.inviteToken(), dates(2), "PROPOSE", "강남 파스타집");
		guestRespond(proposed.inviteToken(), dates(2), "OPEN", null);
		close(host, proposed.id()).andExpect(status().isOk());
		hostResult(host, proposed.id())
				.andExpect(jsonPath("$.selectedPlaces", hasSize(1)))
				.andExpect(jsonPath("$.selectedPlace").value("강남 파스타집"))
				.andExpect(jsonPath("$.placeVoteCount").value(1))
				.andExpect(jsonPath("$.decisionType").value("MAJORITY"));

		Created none = createMeeting(host, OPEN_POLICY, dates(2), "OPEN", null);
		guestRespond(none.inviteToken(), dates(2), "OPEN", null);
		close(host, none.id()).andExpect(status().isOk());
		hostResult(host, none.id())
				.andExpect(jsonPath("$.selectedPlaces", hasSize(0)))
				.andExpect(jsonPath("$.selectedPlace").value("추후 협의"))
				.andExpect(jsonPath("$.placeStatus").value("AWAITING_HOST"))
				.andExpect(jsonPath("$.placeCandidates", hasSize(0)))
				.andExpect(jsonPath("$.placeVoteCount").value(0))
				.andExpect(jsonPath("$.reason").value(containsString("호스트가 최종 장소를 직접 선택")));
	}

	@Test
	void overdueMeeting_isClosedLazilyWhenTheResultIsRead() throws Exception {
		String host = newHostToken();
		Created byHost = createMeeting(host, RECOMMEND_POLICY, dates(2), "RECOMMEND", "송도 카페 A");
		makeOverdue(byHost.id());
		mockMvc.perform(get("/api/meetings/" + byHost.id()).header("Authorization", "Bearer " + host))
				.andExpect(jsonPath("$.status").value("COLLECTING")).andExpect(jsonPath("$.closed").value(true));

		hostResult(host, byHost.id()).andExpect(status().isOk()).andExpect(jsonPath("$.selectedDate").value(day(2)));
		mockMvc.perform(get("/api/meetings/" + byHost.id()).header("Authorization", "Bearer " + host))
				.andExpect(jsonPath("$.status").value("COMPLETED"));
		assertEquals(false, jdbc.queryForObject("select manually_closed from meeting where id = ?", Boolean.class, byHost.id()));

		Created byGuest = createMeeting(host, RECOMMEND_POLICY, dates(3), "RECOMMEND", "송도 카페 B");
		makeOverdue(byGuest.id());
		guestResult(byGuest.inviteToken()).andExpect(status().isOk()).andExpect(jsonPath("$.selectedDate").value(day(3)));
		assertEquals(1, resultCount(byGuest.id()));
	}

	@Test
	void scheduler_closesOnlyOverdueMeetings() throws Exception {
		String host = newHostToken();
		Created overdueA = createMeeting(host, RECOMMEND_POLICY, dates(2), "RECOMMEND", "송도 카페 A");
		Created overdueB = createMeeting(host, OPEN_POLICY, dates(3), "OPEN", null);
		Created open = createMeeting(host, RECOMMEND_POLICY, dates(4), "RECOMMEND", "송도 카페 A");
		makeOverdue(overdueA.id());
		makeOverdue(overdueB.id());

		assertTrue(scheduler.closeDueMeetings() >= 2);

		for (Created closed : new Created[] { overdueA, overdueB }) {
			mockMvc.perform(get("/api/meetings/" + closed.id()).header("Authorization", "Bearer " + host))
					.andExpect(jsonPath("$.status").value("COMPLETED"));
			assertEquals(1, resultCount(closed.id()));
		}
		mockMvc.perform(get("/api/meetings/" + open.id()).header("Authorization", "Bearer " + host))
				.andExpect(jsonPath("$.status").value("COLLECTING"));
		assertEquals(0, resultCount(open.id()));
		assertEquals(0, scheduler.closeDueMeetings());
	}

	@Test
	void resultAccess_isRestricted() throws Exception {
		String host = newHostToken();
		Created meeting = createMeeting(host, RECOMMEND_POLICY, dates(2), "RECOMMEND", "송도 카페 A");
		close(host, meeting.id()).andExpect(status().isOk());

		mockMvc.perform(get("/api/meetings/" + meeting.id() + "/result")).andExpect(status().isUnauthorized());
		hostResult(newHostToken(), meeting.id()).andExpect(status().isForbidden());
		hostResult(host, 999999).andExpect(status().isNotFound());
		close(host, 999999).andExpect(status().isNotFound());
		guestResult("does-not-exist").andExpect(status().isNotFound());

		// 링크가 만료되면 게스트는 볼 수 없지만 호스트는 볼 수 있다.
		jdbc.update("update meeting set link_expiration_date = ? where id = ?",
				OffsetDateTime.ofInstant(Instant.now().minusSeconds(60), ZoneOffset.UTC), meeting.id());
		guestResult(meeting.inviteToken()).andExpect(status().isGone()).andExpect(jsonPath("$.code").value("INVITE_EXPIRED"));
		hostResult(host, meeting.id()).andExpect(status().isOk());

		// 결과 없이 CLOSED로만 표시된 모임은 아직 준비되지 않은 것으로 본다.
		Created pending = createMeeting(host, RECOMMEND_POLICY, dates(2), "RECOMMEND", "송도 카페 A");
		jdbc.update("update meeting set status = 'CLOSED' where id = ?", pending.id());
		hostResult(host, pending.id()).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RESULT_NOT_READY"));
		close(host, pending.id()).andExpect(status().isOk());
		assertEquals(0, resultCount(pending.id()));
	}
}
