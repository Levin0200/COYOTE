package com.coyote.moyeon.meeting;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeetingVotesTest {

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

	private String newHostToken(String name) {
		User user = userRepository.save(new User("votes-host-" + SEQ.incrementAndGet() + "@example.com",
				passwordEncoder.encode("password123"), name, null));
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
				{"title":"투표 테스트","description":"설명","responseDeadline":"%s","linkExpirationDate":null,
				 "candidateStartDate":"%s","candidateEndDate":"%s","placePolicy":%s,
				 "selectedDates":[%s],"placeMode":"%s","region":"송도","placePreference":%s}
				""".formatted(validDue(), day(1), day(10), policy, datesJson, mode, q(place));
		String response = mockMvc.perform(post("/api/meetings").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return new Created(((Number) JsonPath.read(response, "$.meetingId")).longValue(), JsonPath.read(response, "$.inviteToken"));
	}

	private String join(String inviteToken, String nickname) throws Exception {
		String guestId = UUID.randomUUID().toString();
		mockMvc.perform(post("/api/invites/" + inviteToken + "/join").header(GUEST_ID_HEADER, guestId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":" + q(nickname) + "}"))
				.andExpect(status().isCreated());
		return guestId;
	}

	private void respond(String inviteToken, String guestId, String nickname, String datesJson, String mode, String place) throws Exception {
		mockMvc.perform(put("/api/invites/" + inviteToken + "/response").header(GUEST_ID_HEADER, guestId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":" + q(nickname) + ",\"selectedDates\":[" + datesJson + "],\"placeMode\":\"" + mode
						+ "\",\"placePreference\":" + q(place) + "}"))
				.andExpect(status().is2xxSuccessful());
	}

	private ResultActions votes(String token, long id) throws Exception {
		return mockMvc.perform(get("/api/meetings/" + id + "/votes").header("Authorization", "Bearer " + token));
	}

	private ResultActions close(String token, long id) throws Exception {
		return mockMvc.perform(post("/api/meetings/" + id + "/close").header("Authorization", "Bearer " + token));
	}

	@Test
	void votes_listDateCountsAndMembers_afterTheMeetingCloses() throws Exception {
		String host = newHostToken("김호스트");
		Created meeting = createMeeting(host, RECOMMEND_POLICY, dates(2, 3), "RECOMMEND", "송도 카페 A");
		String guest1 = join(meeting.inviteToken(), "게스트1");
		respond(meeting.inviteToken(), guest1, "게스트1", dates(2), "RECOMMEND", "송도 카페 B");
		join(meeting.inviteToken(), "대기자"); // 응답하지 않은 멤버

		close(host, meeting.id()).andExpect(status().isOk());

		votes(host, meeting.id()).andExpect(status().isOk())
				.andExpect(jsonPath("$.participantCount").value(3))
				.andExpect(jsonPath("$.responseCount").value(2))
				.andExpect(jsonPath("$.dateCounts", hasSize(2)))
				.andExpect(jsonPath("$.dateCounts[0].date").value(day(2)))
				.andExpect(jsonPath("$.dateCounts[0].count").value(2))
				.andExpect(jsonPath("$.dateCounts[1].date").value(day(3)))
				.andExpect(jsonPath("$.dateCounts[1].count").value(1))
				.andExpect(jsonPath("$.members", hasSize(3)))
				.andExpect(jsonPath("$.members[0].nickname").value("김호스트"))
				.andExpect(jsonPath("$.members[0].role").value("HOST"))
				.andExpect(jsonPath("$.members[0].dates", hasSize(2)))
				.andExpect(jsonPath("$.members[0].dates[0]").value(day(2)))
				.andExpect(jsonPath("$.members[0].dates[1]").value(day(3)))
				.andExpect(jsonPath("$.members[1].nickname").value("게스트1"))
				.andExpect(jsonPath("$.members[1].role").value("GUEST"))
				.andExpect(jsonPath("$.members[1].dates", hasSize(1)))
				.andExpect(jsonPath("$.members[2].nickname").value("대기자"))
				.andExpect(jsonPath("$.members[2].dates", hasSize(0)));
	}

	@Test
	void votes_areOnlyAvailableToTheHost_afterClose() throws Exception {
		String host = newHostToken("호스트");
		Created meeting = createMeeting(host, OPEN_POLICY, dates(2), "OPEN", null);

		votes(host, meeting.id()).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEETING_NOT_CLOSED"));
		close(host, meeting.id()).andExpect(status().isOk());

		mockMvc.perform(get("/api/meetings/" + meeting.id() + "/votes")).andExpect(status().isUnauthorized());
		votes(newHostToken("타인"), meeting.id()).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		votes(host, 999999).andExpect(status().isNotFound());
		votes(host, meeting.id()).andExpect(status().isOk());
	}

	@Test
	void votes_closeAnOverdueMeetingOnRead() throws Exception {
		String host = newHostToken("호스트");
		Created meeting = createMeeting(host, OPEN_POLICY, dates(2), "OPEN", null);
		jdbc.update("update meeting set response_due_date = ? where id = ?",
				OffsetDateTime.ofInstant(Instant.now().minusSeconds(60), ZoneOffset.UTC), meeting.id());

		votes(host, meeting.id()).andExpect(status().isOk()).andExpect(jsonPath("$.responseCount").value(1));
		mockMvc.perform(get("/api/meetings/" + meeting.id()).header("Authorization", "Bearer " + host))
				.andExpect(jsonPath("$.status").value("COMPLETED"));
		assertEquals(1, jdbc.queryForObject("select count(*) from final_result where meeting_id = ?", Integer.class, meeting.id()));
	}

	@Test
	void list_includesThePolicyRegion() throws Exception {
		String host = newHostToken("호스트");
		Created open = createMeeting(host, OPEN_POLICY, dates(2), "OPEN", null);
		Created recommend = createMeeting(host, RECOMMEND_POLICY, dates(2), "RECOMMEND", "송도 카페 A");

		mockMvc.perform(get("/api/meetings").header("Authorization", "Bearer " + host))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].meetingId").value(recommend.id()))
				.andExpect(jsonPath("$[0].placeRegion").value("송도"))
				.andExpect(jsonPath("$[1].meetingId").value(open.id()))
				.andExpect(jsonPath("$[1].placeRegion").isEmpty());
	}
}
