package com.coyote.moyeon.meeting;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
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
class MeetingParticipationTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final String GUEST_ID_HEADER = "X-Guest-Id";
	private static final AtomicInteger SEQ = new AtomicInteger();

	private static final String OPEN_POLICY = "{\"mode\":\"OPEN\"}";
	private static final String RECOMMEND_POLICY = """
			{"mode":"RECOMMEND","region":"송도","radius":3,
			 "options":[{"name":"송도 카페 A","distance":"0.5km"},{"name":"송도 카페 B","distance":"1.0km"}]}
			""";

	@Autowired MockMvc mockMvc;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired JwtTokenService jwtTokenService;
	@Autowired JdbcTemplate jdbc;

	private record Created(long id, String inviteToken) {
	}

	private String newHostToken() {
		User user = userRepository.save(new User("participation-host-" + SEQ.incrementAndGet() + "@example.com",
				passwordEncoder.encode("password123"), "호스트", null));
		return jwtTokenService.issue(user.getId()).accessToken();
	}

	private static String day(int plusDays) {
		return LocalDate.now(KST).plusDays(plusDays).toString();
	}

	private static String q(Object value) {
		return value == null ? "null" : "\"" + value + "\"";
	}

	private static Instant validDue() {
		Instant later = Instant.now().plus(2, ChronoUnit.DAYS);
		return Instant.ofEpochSecond((later.getEpochSecond() / 600 + 1) * 600);
	}

	private Created createMeeting(String token, String title, String policy, String mode, String place) throws Exception {
		String body = """
				{"title":%s,"description":"설명","responseDeadline":"%s","linkExpirationDate":null,
				 "candidateStartDate":"%s","candidateEndDate":"%s","placePolicy":%s,
				 "selectedDates":["%s"],"placeMode":"%s","region":"송도","placePreference":%s}
				""".formatted(q(title), validDue(), day(1), day(10), policy, day(2), mode, q(place));
		String response = mockMvc.perform(post("/api/meetings").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return new Created(((Number) JsonPath.read(response, "$.meetingId")).longValue(), JsonPath.read(response, "$.inviteToken"));
	}

	private Created createOpenMeeting(String token, String title) throws Exception {
		return createMeeting(token, title, OPEN_POLICY, "OPEN", null);
	}

	private ResultActions join(String inviteToken, String guestId, String nickname) throws Exception {
		return mockMvc.perform(post("/api/invites/" + inviteToken + "/join").header(GUEST_ID_HEADER, guestId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":" + q(nickname) + "}"));
	}

	private ResultActions respond(String inviteToken, String guestId, String mode, String place) throws Exception {
		return mockMvc.perform(put("/api/invites/" + inviteToken + "/response").header(GUEST_ID_HEADER, guestId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"게스트\",\"selectedDates\":[\"" + day(2) + "\"],\"placeMode\":\"" + mode
						+ "\",\"placePreference\":" + q(place) + "}"));
	}

	private ResultActions invite(String inviteToken) throws Exception {
		return mockMvc.perform(get("/api/invites/" + inviteToken));
	}

	private ResultActions hostDetail(String token, long id) throws Exception {
		return mockMvc.perform(get("/api/meetings/" + id).header("Authorization", "Bearer " + token));
	}

	private ResultActions list(String token) throws Exception {
		return mockMvc.perform(get("/api/meetings").header("Authorization", "Bearer " + token));
	}

	private void makeOverdue(long meetingId) {
		jdbc.update("update meeting set response_due_date = ? where id = ?",
				OffsetDateTime.ofInstant(Instant.now().minusSeconds(60), ZoneOffset.UTC), meetingId);
	}

	// ---- A3: 참여 등록 ----

	@Test
	void join_registersPendingParticipants_thatAreCountedButNotInResults() throws Exception {
		String host = newHostToken();
		Created meeting = createOpenMeeting(host, "참여 테스트");
		invite(meeting.inviteToken()).andExpect(jsonPath("$.participantCount").value(1))
				.andExpect(jsonPath("$.responseCount").value(1));

		String guest1 = UUID.randomUUID().toString();
		join(meeting.inviteToken(), guest1, "게스트1").andExpect(status().isCreated())
				.andExpect(jsonPath("$.nickname").value("게스트1")).andExpect(jsonPath("$.responded").value(false));
		invite(meeting.inviteToken()).andExpect(jsonPath("$.participantCount").value(2))
				.andExpect(jsonPath("$.responseCount").value(1));
		hostDetail(host, meeting.id()).andExpect(jsonPath("$.participantCount").value(2))
				.andExpect(jsonPath("$.responseCount").value(1));

		// 같은 guestId로 다시 등록하면 닉네임만 바뀌고 인원은 그대로다(대소문자 무관).
		join(meeting.inviteToken(), guest1.toUpperCase(), " 바뀐닉네임 ").andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("바뀐닉네임")).andExpect(jsonPath("$.responded").value(false));
		invite(meeting.inviteToken()).andExpect(jsonPath("$.participantCount").value(2));

		// 먼저 등록한 참여자가 처음 응답하면 응답이 새로 만들어진다(201). 닉네임은 응답의 닉네임으로 갱신된다.
		respond(meeting.inviteToken(), guest1, "OPEN", null).andExpect(status().isCreated())
				.andExpect(jsonPath("$.nickname").value("게스트"));
		invite(meeting.inviteToken()).andExpect(jsonPath("$.participantCount").value(2))
				.andExpect(jsonPath("$.responseCount").value(2));
		join(meeting.inviteToken(), guest1, "게스트").andExpect(status().isOk())
				.andExpect(jsonPath("$.responded").value(true));
		// 응답 수정은 이전처럼 200이다.
		respond(meeting.inviteToken(), guest1, "OPEN", null).andExpect(status().isOk());

		// 등록만 하고 응답하지 않은 참여자는 인원에는 포함되지만 결과에서는 제외된다.
		join(meeting.inviteToken(), UUID.randomUUID().toString(), "응답안함").andExpect(status().isCreated());
		hostDetail(host, meeting.id()).andExpect(jsonPath("$.participantCount").value(3))
				.andExpect(jsonPath("$.responseCount").value(2));
		mockMvc.perform(post("/api/meetings/" + meeting.id() + "/close").header("Authorization", "Bearer " + host))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/meetings/" + meeting.id() + "/result").header("Authorization", "Bearer " + host))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalResponses").value(2));
	}

	@Test
	void join_rejectsInvalidRequests() throws Exception {
		String host = newHostToken();
		Created meeting = createOpenMeeting(host, "검증 테스트");
		String guest = UUID.randomUUID().toString();

		join(meeting.inviteToken(), "not-a-uuid", "닉네임")
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_GUEST_ID"));
		mockMvc.perform(post("/api/invites/" + meeting.inviteToken() + "/join")
				.contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"닉네임\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_GUEST_ID"));
		join(meeting.inviteToken(), guest, "   ").andExpect(status().isBadRequest());
		join(meeting.inviteToken(), guest, "가".repeat(21)).andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/invites/" + meeting.inviteToken() + "/join").header(GUEST_ID_HEADER, guest)
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
		join("does-not-exist", guest, "닉네임")
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MEETING_NOT_FOUND"));
		invite(meeting.inviteToken()).andExpect(jsonPath("$.participantCount").value(1));
	}

	@Test
	void join_isRejectedAfterTheMeetingClosesOrTheLinkExpires() throws Exception {
		String host = newHostToken();
		Created overdue = createOpenMeeting(host, "마감 시각 경과");
		makeOverdue(overdue.id());
		join(overdue.inviteToken(), UUID.randomUUID().toString(), "늦은참여")
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEETING_CLOSED"));

		Created closed = createOpenMeeting(host, "수동 마감");
		mockMvc.perform(post("/api/meetings/" + closed.id() + "/close").header("Authorization", "Bearer " + host))
				.andExpect(status().isOk());
		join(closed.inviteToken(), UUID.randomUUID().toString(), "늦은참여")
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEETING_CLOSED"));

		Created expired = createOpenMeeting(host, "링크 만료");
		jdbc.update("update meeting set link_expiration_date = ? where id = ?",
				OffsetDateTime.ofInstant(Instant.now().minusSeconds(60), ZoneOffset.UTC), expired.id());
		join(expired.inviteToken(), UUID.randomUUID().toString(), "만료참여")
				.andExpect(status().isGone()).andExpect(jsonPath("$.code").value("INVITE_EXPIRED"));
	}

	// ---- A5: 내 모임 목록 ----

	@Test
	void list_returnsOnlyMyMeetings_newestFirst_withCountsAndResults() throws Exception {
		String host = newHostToken();
		String other = newHostToken();
		list(host).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

		Created first = createOpenMeeting(host, "첫 번째 모임");
		Created second = createMeeting(host, "두 번째 모임", RECOMMEND_POLICY, "RECOMMEND", "송도 카페 A");
		createOpenMeeting(other, "다른 사람의 모임");

		join(first.inviteToken(), UUID.randomUUID().toString(), "대기자").andExpect(status().isCreated());
		respond(second.inviteToken(), UUID.randomUUID().toString(), "RECOMMEND", "송도 카페 B").andExpect(status().isCreated());

		list(host).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].meetingId").value(second.id()))
				.andExpect(jsonPath("$[0].title").value("두 번째 모임"))
				.andExpect(jsonPath("$[0].inviteToken").value(second.inviteToken()))
				.andExpect(jsonPath("$[0].description").value("설명"))
				.andExpect(jsonPath("$[0].responseDeadline", notNullValue()))
				.andExpect(jsonPath("$[0].status").value("COLLECTING"))
				.andExpect(jsonPath("$[0].closed").value(false))
				.andExpect(jsonPath("$[0].participantCount").value(2))
				.andExpect(jsonPath("$[0].responseCount").value(2))
				.andExpect(jsonPath("$[0].result").doesNotExist())
				.andExpect(jsonPath("$[1].meetingId").value(first.id()))
				.andExpect(jsonPath("$[1].participantCount").value(2))
				.andExpect(jsonPath("$[1].responseCount").value(1));

		// 마감하면 결과 요약이 붙는다(장소 응답이 없으면 추후 협의).
		mockMvc.perform(post("/api/meetings/" + first.id() + "/close").header("Authorization", "Bearer " + host))
				.andExpect(status().isOk());
		list(host).andExpect(jsonPath("$[1].status").value("COMPLETED"))
				.andExpect(jsonPath("$[1].closed").value(true))
				.andExpect(jsonPath("$[1].result.selectedDate").value(day(2)))
				.andExpect(jsonPath("$[1].result.selectedPlaces", hasSize(0)))
				.andExpect(jsonPath("$[1].result.selectedPlace").value("추후 협의"))
				.andExpect(jsonPath("$[1].result.decisionType").value("MAJORITY"))
				.andExpect(jsonPath("$[0].result").doesNotExist());

		// 다른 사용자의 목록에는 내 모임이 보이지 않는다.
		list(other).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].title").value("다른 사람의 모임"));
	}

	@Test
	void list_marksOverdueMeetingsAsClosedBeforeTheSchedulerRuns() throws Exception {
		String host = newHostToken();
		Created meeting = createMeeting(host, "시각 경과", RECOMMEND_POLICY, "RECOMMEND", "송도 카페 A");
		makeOverdue(meeting.id());

		list(host).andExpect(jsonPath("$[0].status").value("COLLECTING"))
				.andExpect(jsonPath("$[0].closed").value(true))
				.andExpect(jsonPath("$[0].result").doesNotExist());
	}

	@Test
	void list_requiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/meetings")).andExpect(status().isUnauthorized());
	}
}
