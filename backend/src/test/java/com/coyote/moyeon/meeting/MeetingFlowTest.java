package com.coyote.moyeon.meeting;

import static org.hamcrest.Matchers.hasSize;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
class MeetingFlowTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final String GUEST_ID_HEADER = "X-Guest-Id";
	private static final AtomicInteger SEQ = new AtomicInteger();

	@Autowired MockMvc mockMvc;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired JwtTokenService jwtTokenService;
	@Autowired JdbcTemplate jdbc;

	private String newHostToken(String name) {
		User user = userRepository.save(new User("meeting-host-" + SEQ.incrementAndGet() + "@example.com",
				passwordEncoder.encode("password123"), name, null));
		return jwtTokenService.issue(user.getId()).accessToken();
	}

	private static Instant tenMinuteAligned(Instant instant) {
		return Instant.ofEpochSecond((instant.getEpochSecond() / 600 + 1) * 600);
	}

	private static Instant validDue() {
		return tenMinuteAligned(Instant.now().plus(2, ChronoUnit.DAYS));
	}

	private static String day(int plusDays) {
		return "\"" + LocalDate.now(KST).plusDays(plusDays) + "\"";
	}

	private static String q(Object value) {
		return value == null ? "null" : "\"" + value + "\"";
	}

	private static String meetingJson(String title, Instant due, Instant linkExp, String datesJson, String mode, String place) {
		return """
				{"title":%s,"description":"모임 설명","responseDueDate":%s,"linkExpirationDate":%s,
				 "selectedDates":[%s],"placeMode":"%s","region":"송도","placePreference":%s}
				""".formatted(q(title), q(due), q(linkExp), datesJson, mode, q(place));
	}

	private static String defaultMeetingJson() {
		return meetingJson("캡스톤 회식", validDue(), null, day(3) + "," + day(1) + "," + day(1), "MANUAL", "송도 카페");
	}

	private static String guestJson(String nickname, String datesJson, String mode, String place) {
		return """
				{"nickname":%s,"selectedDates":[%s],"placeMode":"%s","region":"송도","placePreference":%s}
				""".formatted(q(nickname), datesJson, mode, q(place));
	}

	private ResultActions createMeeting(String token, String body) throws Exception {
		return mockMvc.perform(post("/api/meetings").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private record Created(long id, String inviteToken) {
	}

	private Created createDefaultMeeting(String token) throws Exception {
		String body = createMeeting(token, defaultMeetingJson()).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return new Created(((Number) JsonPath.read(body, "$.id")).longValue(), JsonPath.read(body, "$.inviteToken"));
	}

	private ResultActions submit(String inviteToken, String guestId, String body) throws Exception {
		return mockMvc.perform(put("/api/invites/" + inviteToken + "/response").header(GUEST_ID_HEADER, guestId)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private void setDueDate(long meetingId, Instant due) {
		jdbc.update("update meeting set response_due_date = ? where id = ?", OffsetDateTime.ofInstant(due, ZoneOffset.UTC), meetingId);
	}

	@Test
	void hostCreatesMeeting_andReadsIt() throws Exception {
		String token = newHostToken("김호스트");
		Instant due = validDue();
		String body = createMeeting(token, meetingJson("캡스톤 회식", due, null, day(3) + "," + day(1) + "," + day(1), "MANUAL", "송도 카페"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("COLLECTING"))
				.andExpect(jsonPath("$.closed").value(false))
				.andExpect(jsonPath("$.participantCount").value(1))
				.andExpect(jsonPath("$.responseCount").value(1))
				.andExpect(jsonPath("$.responseDueDate").value(due.toString()))
				.andExpect(jsonPath("$.myResponse.nickname").value("김호스트"))
				.andExpect(jsonPath("$.myResponse.selectedDates", hasSize(2)))
				.andExpect(jsonPath("$.myResponse.selectedDates[0]").value(LocalDate.now(KST).plusDays(1).toString()))
				.andExpect(jsonPath("$.myResponse.selectedDates[1]").value(LocalDate.now(KST).plusDays(3).toString()))
				.andReturn().getResponse().getContentAsString();
		String inviteToken = JsonPath.read(body, "$.inviteToken");
		long id = ((Number) JsonPath.read(body, "$.id")).longValue();
		org.junit.jupiter.api.Assertions.assertTrue(inviteToken.length() >= 32);

		mockMvc.perform(get("/api/meetings/" + id).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("캡스톤 회식"))
				.andExpect(jsonPath("$.inviteToken").value(inviteToken))
				.andExpect(jsonPath("$.myResponse.placePreference").value("송도 카페"));
	}

	@Test
	void hostNickname_isTruncatedToTwentyCharacters() throws Exception {
		String token = newHostToken("가".repeat(25));
		createMeeting(token, defaultMeetingJson()).andExpect(status().isCreated())
				.andExpect(jsonPath("$.myResponse.nickname").value("가".repeat(20)));
	}

	@Test
	void skipMode_dropsPlaceFields() throws Exception {
		String token = newHostToken("호스트");
		createMeeting(token, meetingJson("제목", validDue(), null, day(1), "SKIP", "무시될 값"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.myResponse.placeMode").value("SKIP"))
				.andExpect(jsonPath("$.myResponse.placePreference").isEmpty())
				.andExpect(jsonPath("$.myResponse.region").isEmpty());
	}

	@Test
	void createMeeting_rejectsInvalidInput() throws Exception {
		String token = newHostToken("호스트");
		Instant due = validDue();
		String yesterday = "\"" + LocalDate.now(KST).minusDays(1) + "\"";
		String tooMany = IntStream.rangeClosed(1, 63).mapToObj(MeetingFlowTest::day).collect(Collectors.joining(","));
		String[] badBodies = {
				meetingJson("제목", Instant.now().minusSeconds(3600), null, day(1), "MANUAL", "장소"),
				meetingJson("제목", due.plusSeconds(7 * 60), null, day(1), "MANUAL", "장소"),
				meetingJson("제목", due.plusSeconds(5), null, day(1), "MANUAL", "장소"),
				meetingJson("제목", due, due, day(1), "MANUAL", "장소"),
				meetingJson("제목", due, due.minusSeconds(600), day(1), "MANUAL", "장소"),
				meetingJson("제목", due, null, "", "MANUAL", "장소"),
				meetingJson("제목", due, null, yesterday, "MANUAL", "장소"),
				meetingJson("제목", due, null, tooMany, "MANUAL", "장소"),
				meetingJson("제목", due, null, day(1), "MANUAL", null),
				meetingJson("제목", due, null, day(1), "RECOMMEND", "  "),
				meetingJson("제목", due, null, day(1), "UNKNOWN", "장소"),
				meetingJson("   ", due, null, day(1), "MANUAL", "장소"),
		};
		for (String body : badBodies) {
			createMeeting(token, body).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}

	@Test
	void dueDateMustBeTenMinuteAligned_inKst() throws Exception {
		String token = newHostToken("호스트");
		createMeeting(token, meetingJson("제목", validDue().plusSeconds(7 * 60), null, day(1), "MANUAL", "장소"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("응답 마감일은 10분 단위로 설정해주세요."));
		createMeeting(token, meetingJson("제목", validDue().plusSeconds(20 * 60), null, day(1), "MANUAL", "장소"))
				.andExpect(status().isCreated());
	}

	@Test
	void hostEndpoints_requireAuthAndOwnership() throws Exception {
		mockMvc.perform(post("/api/meetings").contentType(MediaType.APPLICATION_JSON).content(defaultMeetingJson()))
				.andExpect(status().isUnauthorized());

		String owner = newHostToken("주인");
		String other = newHostToken("타인");
		Created meeting = createDefaultMeeting(owner);

		mockMvc.perform(get("/api/meetings/" + meeting.id()).header("Authorization", "Bearer " + other))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		mockMvc.perform(get("/api/meetings/999999").header("Authorization", "Bearer " + owner))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/meetings/abc").header("Authorization", "Bearer " + owner))
				.andExpect(status().isBadRequest());
	}

	@Test
	void guestJoinsUpdatesAndSeesOnlyOwnResponse() throws Exception {
		String host = newHostToken("호스트");
		Created meeting = createDefaultMeeting(host);
		String guestId = UUID.randomUUID().toString();

		mockMvc.perform(get("/api/invites/" + meeting.inviteToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("캡스톤 회식"))
				.andExpect(jsonPath("$.participantCount").value(1))
				.andExpect(jsonPath("$.closed").value(false));

		submit(meeting.inviteToken(), guestId, guestJson("게스트A", day(2), "RECOMMEND", "센트럴파크 카페"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nickname").value("게스트A"));

		submit(meeting.inviteToken(), guestId.toUpperCase(), guestJson("바뀐닉네임", day(4) + "," + day(5), "SKIP", null))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("바뀐닉네임"))
				.andExpect(jsonPath("$.selectedDates", hasSize(2)))
				.andExpect(jsonPath("$.placeMode").value("SKIP"));

		mockMvc.perform(get("/api/invites/" + meeting.inviteToken() + "/response/me").header(GUEST_ID_HEADER, guestId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("바뀐닉네임"))
				.andExpect(jsonPath("$.selectedDates[0]").value(LocalDate.now(KST).plusDays(4).toString()));

		mockMvc.perform(get("/api/invites/" + meeting.inviteToken()))
				.andExpect(jsonPath("$.participantCount").value(2))
				.andExpect(jsonPath("$.responseCount").value(2));

		submit(meeting.inviteToken(), UUID.randomUUID().toString(), guestJson("게스트A", day(2), "SKIP", null))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/invites/" + meeting.inviteToken()))
				.andExpect(jsonPath("$.participantCount").value(3));

		mockMvc.perform(get("/api/invites/" + meeting.inviteToken() + "/response/me")
				.header(GUEST_ID_HEADER, UUID.randomUUID().toString()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESPONSE_NOT_FOUND"));
	}

	@Test
	void guestRequests_validateHeaderAndBody() throws Exception {
		Created meeting = createDefaultMeeting(newHostToken("호스트"));
		String validBody = guestJson("닉네임", day(2), "SKIP", null);

		submit(meeting.inviteToken(), "not-a-uuid", validBody)
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_GUEST_ID"));
		mockMvc.perform(put("/api/invites/" + meeting.inviteToken() + "/response")
				.contentType(MediaType.APPLICATION_JSON).content(validBody))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_GUEST_ID"));
		mockMvc.perform(get("/api/invites/" + meeting.inviteToken() + "/response/me"))
				.andExpect(status().isBadRequest());

		String guestId = UUID.randomUUID().toString();
		String[] badBodies = {
				guestJson("   ", day(2), "SKIP", null),
				guestJson("가".repeat(21), day(2), "SKIP", null),
				guestJson("닉네임", "", "SKIP", null),
				guestJson("닉네임", "\"" + LocalDate.now(KST).minusDays(1) + "\"", "SKIP", null),
				guestJson("닉네임", day(2), "MANUAL", null),
		};
		for (String body : badBodies) {
			submit(meeting.inviteToken(), guestId, body).andExpect(status().isBadRequest());
		}
	}

	@Test
	void unknownInviteToken_isNotFound() throws Exception {
		mockMvc.perform(get("/api/invites/does-not-exist"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MEETING_NOT_FOUND"));
		submit("does-not-exist", UUID.randomUUID().toString(), guestJson("닉네임", day(2), "SKIP", null))
				.andExpect(status().isNotFound());
	}

	@Test
	void afterDeadline_submissionsAreRejected_butViewingStillWorks() throws Exception {
		String host = newHostToken("호스트");
		Created meeting = createDefaultMeeting(host);
		String guestId = UUID.randomUUID().toString();
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "SKIP", null)).andExpect(status().isCreated());

		setDueDate(meeting.id(), Instant.now().minusSeconds(60));

		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(3), "SKIP", null))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEETING_CLOSED"));
		submit(meeting.inviteToken(), UUID.randomUUID().toString(), guestJson("신규", day(3), "SKIP", null))
				.andExpect(status().isConflict());
		mockMvc.perform(get("/api/invites/" + meeting.inviteToken()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.closed").value(true));
		mockMvc.perform(get("/api/invites/" + meeting.inviteToken() + "/response/me").header(GUEST_ID_HEADER, guestId))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/meetings/" + meeting.id()).header("Authorization", "Bearer " + host))
				.andExpect(jsonPath("$.closed").value(true));
	}

	@Test
	void closedStatus_blocksSubmissionsEvenBeforeDeadline() throws Exception {
		Created meeting = createDefaultMeeting(newHostToken("호스트"));
		jdbc.update("update meeting set status = 'CLOSED' where id = ?", meeting.id());
		submit(meeting.inviteToken(), UUID.randomUUID().toString(), guestJson("게스트", day(2), "SKIP", null))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEETING_CLOSED"));
	}

	@Test
	void expiredInviteLink_isGone() throws Exception {
		Created meeting = createDefaultMeeting(newHostToken("호스트"));
		jdbc.update("update meeting set link_expiration_date = ? where id = ?",
				OffsetDateTime.ofInstant(Instant.now().minusSeconds(60), ZoneOffset.UTC), meeting.id());

		mockMvc.perform(get("/api/invites/" + meeting.inviteToken()))
				.andExpect(status().isGone()).andExpect(jsonPath("$.code").value("INVITE_EXPIRED"));
		submit(meeting.inviteToken(), UUID.randomUUID().toString(), guestJson("게스트", day(2), "SKIP", null))
				.andExpect(status().isGone());
	}
}
