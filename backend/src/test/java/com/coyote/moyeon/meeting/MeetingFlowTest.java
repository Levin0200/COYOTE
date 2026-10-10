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

	private static final String OPEN_POLICY = "{\"mode\":\"OPEN\"}";
	private static final String FIXED_POLICY = "{\"mode\":\"FIXED\",\"fixedPlace\":\"홍대 스페이스 룸\"}";
	private static final String RECOMMEND_POLICY = """
			{"mode":"RECOMMEND","region":"송도","radius":3,"keyword":"카페",
			 "options":[{"id":1,"name":"송도 카페 A","distance":"0.5km"},
			            {"id":2,"name":"송도 카페 B","distance":"1.0km"},
			            {"id":3,"name":"송도 카페 C","distance":"1.5km"}]}
			""";

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

	/** 기본 후보 범위(내일~10일 뒤)로 모임 생성 요청 본문을 만든다. */
	private static String meetingJson(String title, Instant due, Instant linkExp, String policy, String datesJson,
			String mode, String place) {
		return meetingJson(title, due, linkExp, day(1), day(10), policy, datesJson, mode, place);
	}

	private static String meetingJson(String title, Instant due, Instant linkExp, String start, String end,
			String policy, String datesJson, String mode, String place) {
		return """
				{"title":%s,"description":"모임 설명","responseDeadline":%s,"linkExpirationDate":%s,
				 "candidateStartDate":%s,"candidateEndDate":%s,"placePolicy":%s,
				 "selectedDates":[%s],"placeMode":"%s","region":"송도","placePreference":%s}
				""".formatted(q(title), q(due), q(linkExp), start, end, policy, datesJson, mode, q(place));
	}

	private static String defaultMeetingJson() {
		return meetingJson("캡스톤 회식", validDue(), null, RECOMMEND_POLICY, day(3) + "," + day(1) + "," + day(1),
				"RECOMMEND", "송도 카페 A");
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

	private Created createMeetingFrom(String token, String body) throws Exception {
		String response = createMeeting(token, body).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return new Created(((Number) JsonPath.read(response, "$.meetingId")).longValue(), JsonPath.read(response, "$.inviteToken"));
	}

	private Created createDefaultMeeting(String token) throws Exception {
		return createMeetingFrom(token, defaultMeetingJson());
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
		String body = createMeeting(token, meetingJson("캡스톤 회식", due, null, RECOMMEND_POLICY,
				day(3) + "," + day(1) + "," + day(1), "RECOMMEND", "송도 카페 A"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("COLLECTING"))
				.andExpect(jsonPath("$.closed").value(false))
				.andExpect(jsonPath("$.participantCount").value(1))
				.andExpect(jsonPath("$.responseCount").value(1))
				.andExpect(jsonPath("$.responseDeadline").value(due.toString()))
				.andExpect(jsonPath("$.candidateStartDate").value(LocalDate.now(KST).plusDays(1).toString()))
				.andExpect(jsonPath("$.candidateEndDate").value(LocalDate.now(KST).plusDays(10).toString()))
				.andExpect(jsonPath("$.placePolicy.mode").value("RECOMMEND"))
				.andExpect(jsonPath("$.placePolicy.region").value("송도"))
				.andExpect(jsonPath("$.placePolicy.radius").value(3))
				.andExpect(jsonPath("$.placePolicy.keyword").value("카페"))
				.andExpect(jsonPath("$.placePolicy.fixedPlace").isEmpty())
				.andExpect(jsonPath("$.placePolicy.options", hasSize(3)))
				.andExpect(jsonPath("$.placePolicy.options[0].id").value(1))
				.andExpect(jsonPath("$.placePolicy.options[0].name").value("송도 카페 A"))
				.andExpect(jsonPath("$.placePolicy.options[2].id").value(3))
				.andExpect(jsonPath("$.placePolicy.options[2].distance").value("1.5km"))
				.andExpect(jsonPath("$.myResponse.nickname").value("김호스트"))
				.andExpect(jsonPath("$.myResponse.placeMode").value("RECOMMEND"))
				.andExpect(jsonPath("$.myResponse.selectedDates", hasSize(2)))
				.andExpect(jsonPath("$.myResponse.selectedDates[0]").value(LocalDate.now(KST).plusDays(1).toString()))
				.andExpect(jsonPath("$.myResponse.selectedDates[1]").value(LocalDate.now(KST).plusDays(3).toString()))
				.andReturn().getResponse().getContentAsString();
		String inviteToken = JsonPath.read(body, "$.inviteToken");
		long id = ((Number) JsonPath.read(body, "$.meetingId")).longValue();
		org.junit.jupiter.api.Assertions.assertTrue(inviteToken.length() >= 32);

		mockMvc.perform(get("/api/meetings/" + id).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("캡스톤 회식"))
				.andExpect(jsonPath("$.inviteToken").value(inviteToken))
				.andExpect(jsonPath("$.placePolicy.options", hasSize(3)))
				.andExpect(jsonPath("$.myResponse.placePreference").value("송도 카페 A"))
				.andExpect(jsonPath("$.myResponse.recommendationRadius").value(3))
				.andExpect(jsonPath("$.myResponse.recommendationKeyword").value("카페"));
	}

	@Test
	void hostNickname_isTruncatedToTwentyCharacters() throws Exception {
		String token = newHostToken("가".repeat(25));
		createMeeting(token, defaultMeetingJson()).andExpect(status().isCreated())
				.andExpect(jsonPath("$.myResponse.nickname").value("가".repeat(20)));
	}

	@Test
	void openPolicy_hostOpen_dropsPlaceFields() throws Exception {
		String token = newHostToken("호스트");
		createMeeting(token, meetingJson("제목", validDue(), null, "{\"mode\":\"OPEN\",\"region\":\"무시\",\"fixedPlace\":\"무시\"}",
				day(1), "OPEN", "무시될 값"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.placePolicy.mode").value("OPEN"))
				.andExpect(jsonPath("$.placePolicy.region").isEmpty())
				.andExpect(jsonPath("$.placePolicy.fixedPlace").isEmpty())
				.andExpect(jsonPath("$.placePolicy.options", hasSize(0)))
				.andExpect(jsonPath("$.myResponse.placeMode").value("OPEN"))
				.andExpect(jsonPath("$.myResponse.placePreference").isEmpty())
				.andExpect(jsonPath("$.myResponse.region").isEmpty());
	}

	@Test
	void fixedPolicy_forcesTheFixedPlace() throws Exception {
		String host = newHostToken("호스트");
		// 장소를 생략해도 서버가 확정 장소로 채운다.
		Created meeting = createMeetingFrom(host, meetingJson("제목", validDue(), null, FIXED_POLICY, day(1), "FIXED", null));
		mockMvc.perform(get("/api/meetings/" + meeting.id()).header("Authorization", "Bearer " + host))
				.andExpect(jsonPath("$.placePolicy.mode").value("FIXED"))
				.andExpect(jsonPath("$.placePolicy.fixedPlace").value("홍대 스페이스 룸"))
				.andExpect(jsonPath("$.myResponse.placeMode").value("FIXED"))
				.andExpect(jsonPath("$.myResponse.placePreference").value("홍대 스페이스 룸"));

		mockMvc.perform(get("/api/invites/" + meeting.inviteToken()))
				.andExpect(jsonPath("$.placePolicy.mode").value("FIXED"))
				.andExpect(jsonPath("$.placePolicy.fixedPlace").value("홍대 스페이스 룸"));

		String guestId = UUID.randomUUID().toString();
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "FIXED", "강남역 카페"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "PROPOSE", "강남역 카페"))
				.andExpect(status().isBadRequest());
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "OPEN", null))
				.andExpect(status().isBadRequest());
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "FIXED", "홍대 스페이스 룸"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.placePreference").value("홍대 스페이스 룸"));
	}

	@Test
	void recommendPolicy_onlyAllowsListedOptions() throws Exception {
		String host = newHostToken("호스트");
		Created meeting = createDefaultMeeting(host);
		String guestId = UUID.randomUUID().toString();

		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "RECOMMEND", "목록에 없는 카페"))
				.andExpect(status().isBadRequest());
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "RECOMMEND", null))
				.andExpect(status().isBadRequest());
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "OPEN", null))
				.andExpect(status().isBadRequest());
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "PROPOSE", "송도 카페 A"))
				.andExpect(status().isBadRequest());
		// 지역은 정책 값으로 저장된다.
		submit(meeting.inviteToken(), guestId, "{\"nickname\":\"게스트\",\"selectedDates\":[" + day(2)
				+ "],\"placeMode\":\"RECOMMEND\",\"region\":\"다른지역\",\"placePreference\":\"송도 카페 B\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.region").value("송도"))
				.andExpect(jsonPath("$.recommendationRadius").value(3));
	}

	@Test
	void openPolicy_guestsMayProposeOrSkip_hostMayNotPropose() throws Exception {
		String host = newHostToken("호스트");
		createMeeting(host, meetingJson("제목", validDue(), null, OPEN_POLICY, day(1), "PROPOSE", "호스트 제안"))
				.andExpect(status().isBadRequest());
		Created meeting = createMeetingFrom(host, meetingJson("제목", validDue(), null, OPEN_POLICY, day(1), "OPEN", null));

		String guestId = UUID.randomUUID().toString();
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "PROPOSE", null))
				.andExpect(status().isBadRequest());
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "RECOMMEND", "송도 카페 A"))
				.andExpect(status().isBadRequest());
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "FIXED", "아무데나"))
				.andExpect(status().isBadRequest());
		submit(meeting.inviteToken(), guestId, "{\"nickname\":\"게스트\",\"selectedDates\":[" + day(2)
				+ "],\"placeMode\":\"PROPOSE\",\"region\":\"강남\",\"placePreference\":\"강남 파스타집\","
				+ "\"recommendationRadius\":5,\"recommendationKeyword\":\"파스타\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.placeMode").value("PROPOSE"))
				.andExpect(jsonPath("$.placePreference").value("강남 파스타집"))
				.andExpect(jsonPath("$.recommendationRadius").value(5))
				.andExpect(jsonPath("$.recommendationKeyword").value("파스타"));
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "OPEN", "무시될 값"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.placeMode").value("OPEN"))
				.andExpect(jsonPath("$.placePreference").isEmpty())
				.andExpect(jsonPath("$.region").isEmpty())
				.andExpect(jsonPath("$.recommendationRadius").isEmpty());
	}

	@Test
	void createMeeting_rejectsInvalidInput() throws Exception {
		String token = newHostToken("호스트");
		Instant due = validDue();
		String yesterday = "\"" + LocalDate.now(KST).minusDays(1) + "\"";
		String tooMany = IntStream.rangeClosed(1, 63).mapToObj(MeetingFlowTest::day).collect(Collectors.joining(","));
		String elevenOptions = IntStream.rangeClosed(1, 11).mapToObj(i -> "{\"name\":\"후보" + i + "\"}")
				.collect(Collectors.joining(","));
		String[] badBodies = {
				meetingJson("제목", Instant.now().minusSeconds(3600), null, RECOMMEND_POLICY, day(1), "RECOMMEND", "송도 카페 A"),
				meetingJson("제목", due.plusSeconds(7 * 60), null, RECOMMEND_POLICY, day(1), "RECOMMEND", "송도 카페 A"),
				meetingJson("제목", due.plusSeconds(5), null, RECOMMEND_POLICY, day(1), "RECOMMEND", "송도 카페 A"),
				meetingJson("제목", due, due, RECOMMEND_POLICY, day(1), "RECOMMEND", "송도 카페 A"),
				meetingJson("제목", due, due.minusSeconds(600), RECOMMEND_POLICY, day(1), "RECOMMEND", "송도 카페 A"),
				meetingJson("제목", due, null, RECOMMEND_POLICY, "", "RECOMMEND", "송도 카페 A"),
				meetingJson("제목", due, null, RECOMMEND_POLICY, yesterday, "RECOMMEND", "송도 카페 A"),
				meetingJson("제목", due, null, RECOMMEND_POLICY, tooMany, "RECOMMEND", "송도 카페 A"),
				meetingJson("제목", due, null, RECOMMEND_POLICY, day(1), "RECOMMEND", null),
				meetingJson("제목", due, null, RECOMMEND_POLICY, day(1), "RECOMMEND", "  "),
				meetingJson("제목", due, null, RECOMMEND_POLICY, day(1), "UNKNOWN", "송도 카페 A"),
				meetingJson("   ", due, null, RECOMMEND_POLICY, day(1), "RECOMMEND", "송도 카페 A"),
				// 정책 검증
				meetingJson("제목", due, null, "{\"mode\":\"FIXED\"}", day(1), "FIXED", null),
				meetingJson("제목", due, null, "{\"mode\":\"FIXED\",\"fixedPlace\":\"  \"}", day(1), "FIXED", null),
				meetingJson("제목", due, null, "{\"mode\":\"RECOMMEND\",\"region\":\"송도\",\"radius\":3,\"options\":[]}", day(1), "RECOMMEND", "a"),
				meetingJson("제목", due, null, "{\"mode\":\"RECOMMEND\",\"radius\":3,\"options\":[{\"name\":\"a\"}]}", day(1), "RECOMMEND", "a"),
				meetingJson("제목", due, null, "{\"mode\":\"RECOMMEND\",\"region\":\"송도\",\"options\":[{\"name\":\"a\"}]}", day(1), "RECOMMEND", "a"),
				meetingJson("제목", due, null, "{\"mode\":\"RECOMMEND\",\"region\":\"송도\",\"radius\":3,\"options\":[{\"name\":\"a\"},{\"name\":\" a \"}]}", day(1), "RECOMMEND", "a"),
				meetingJson("제목", due, null, "{\"mode\":\"RECOMMEND\",\"region\":\"송도\",\"radius\":3,\"options\":[" + elevenOptions + "]}", day(1), "RECOMMEND", "후보1"),
				meetingJson("제목", due, null, "{\"mode\":\"RECOMMEND\",\"region\":\"송도\",\"radius\":0,\"options\":[{\"name\":\"a\"}]}", day(1), "RECOMMEND", "a"),
				meetingJson("제목", due, null, "{\"mode\":\"NOPE\"}", day(1), "OPEN", null),
				meetingJson("제목", due, null, "null", day(1), "OPEN", null),
				// 호스트 응답이 정책과 어긋남
				meetingJson("제목", due, null, FIXED_POLICY, day(1), "FIXED", "다른 장소"),
				meetingJson("제목", due, null, RECOMMEND_POLICY, day(1), "RECOMMEND", "목록에 없음"),
				meetingJson("제목", due, null, RECOMMEND_POLICY, day(1), "OPEN", null),
		};
		for (String body : badBodies) {
			createMeeting(token, body).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
	}

	@Test
	void candidateRange_isValidated() throws Exception {
		String token = newHostToken("호스트");
		Instant due = validDue();
		String yesterday = "\"" + LocalDate.now(KST).minusDays(1) + "\"";

		// 시작일이 과거
		createMeeting(token, meetingJson("제목", due, null, yesterday, day(5), OPEN_POLICY, day(1), "OPEN", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("후보 시작일은 오늘 이후로 설정해주세요."));
		// 시작일이 종료일보다 늦음
		createMeeting(token, meetingJson("제목", due, null, day(5), day(4), OPEN_POLICY, day(5), "OPEN", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("후보 시작일은 종료일보다 늦을 수 없습니다."));
		// 63일은 거절, 62일은 허용
		createMeeting(token, meetingJson("제목", due, null, day(1), day(63), OPEN_POLICY, day(1), "OPEN", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("후보 날짜 범위는 최대 62일까지 설정할 수 있습니다."));
		createMeeting(token, meetingJson("제목", due, null, day(1), day(62), OPEN_POLICY, day(62), "OPEN", null))
				.andExpect(status().isCreated());
		// 시작일과 종료일이 같은 하루짜리 범위, 오늘부터 시작
		createMeeting(token, meetingJson("제목", due, null, day(0), day(0), OPEN_POLICY, day(0), "OPEN", null))
				.andExpect(status().isCreated());
		// 필수값
		createMeeting(token, defaultMeetingJson().replace("\"candidateStartDate\":" + day(1) + ",", ""))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void selectedDates_mustBeInsideCandidateRange() throws Exception {
		String host = newHostToken("호스트");
		// 범위: 내일 ~ 10일 뒤
		createMeeting(host, meetingJson("제목", validDue(), null, OPEN_POLICY, day(11), "OPEN", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("선택한 날짜가 후보 날짜 범위를 벗어났습니다."));
		createMeeting(host, meetingJson("제목", validDue(), null, day(2), day(10), OPEN_POLICY, day(1) + "," + day(3), "OPEN", null))
				.andExpect(status().isBadRequest());
		createMeeting(host, meetingJson("제목", validDue(), null, OPEN_POLICY, day(1) + "," + day(10), "OPEN", null))
				.andExpect(status().isCreated());

		Created meeting = createDefaultMeeting(host);
		String guestId = UUID.randomUUID().toString();
		mockMvc.perform(get("/api/invites/" + meeting.inviteToken()))
				.andExpect(jsonPath("$.candidateStartDate").value(LocalDate.now(KST).plusDays(1).toString()))
				.andExpect(jsonPath("$.candidateEndDate").value(LocalDate.now(KST).plusDays(10).toString()));
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(11), "RECOMMEND", "송도 카페 A"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("선택한 날짜가 후보 날짜 범위를 벗어났습니다."));
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(0), "RECOMMEND", "송도 카페 A"))
				.andExpect(status().isBadRequest());
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(1) + "," + day(10), "RECOMMEND", "송도 카페 A"))
				.andExpect(status().isCreated());
	}

	@Test
	void dueDateMustBeTenMinuteAligned_inKst() throws Exception {
		String token = newHostToken("호스트");
		createMeeting(token, meetingJson("제목", validDue().plusSeconds(7 * 60), null, OPEN_POLICY, day(1), "OPEN", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("응답 마감일은 10분 단위로 설정해주세요."));
		createMeeting(token, meetingJson("제목", validDue().plusSeconds(20 * 60), null, OPEN_POLICY, day(1), "OPEN", null))
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
				.andExpect(jsonPath("$.placePolicy.mode").value("RECOMMEND"))
				.andExpect(jsonPath("$.placePolicy.options", hasSize(3)))
				.andExpect(jsonPath("$.placePolicy.options[1].id").value(2))
				.andExpect(jsonPath("$.placePolicy.options[1].name").value("송도 카페 B"))
				.andExpect(jsonPath("$.participantCount").value(1))
				.andExpect(jsonPath("$.closed").value(false));

		submit(meeting.inviteToken(), guestId, guestJson("게스트A", day(2), "RECOMMEND", "송도 카페 B"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nickname").value("게스트A"));

		submit(meeting.inviteToken(), guestId.toUpperCase(), guestJson("바뀐닉네임", day(4) + "," + day(5), "RECOMMEND", "송도 카페 C"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("바뀐닉네임"))
				.andExpect(jsonPath("$.selectedDates", hasSize(2)))
				.andExpect(jsonPath("$.placePreference").value("송도 카페 C"));

		mockMvc.perform(get("/api/invites/" + meeting.inviteToken() + "/response/me").header(GUEST_ID_HEADER, guestId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("바뀐닉네임"))
				.andExpect(jsonPath("$.selectedDates[0]").value(LocalDate.now(KST).plusDays(4).toString()));

		mockMvc.perform(get("/api/invites/" + meeting.inviteToken()))
				.andExpect(jsonPath("$.participantCount").value(2))
				.andExpect(jsonPath("$.responseCount").value(2));

		submit(meeting.inviteToken(), UUID.randomUUID().toString(), guestJson("게스트A", day(2), "RECOMMEND", "송도 카페 A"))
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
		String validBody = guestJson("닉네임", day(2), "RECOMMEND", "송도 카페 A");

		submit(meeting.inviteToken(), "not-a-uuid", validBody)
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_GUEST_ID"));
		mockMvc.perform(put("/api/invites/" + meeting.inviteToken() + "/response")
				.contentType(MediaType.APPLICATION_JSON).content(validBody))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_GUEST_ID"));
		mockMvc.perform(get("/api/invites/" + meeting.inviteToken() + "/response/me"))
				.andExpect(status().isBadRequest());

		String guestId = UUID.randomUUID().toString();
		String[] badBodies = {
				guestJson("   ", day(2), "RECOMMEND", "송도 카페 A"),
				guestJson("가".repeat(21), day(2), "RECOMMEND", "송도 카페 A"),
				guestJson("닉네임", "", "RECOMMEND", "송도 카페 A"),
				guestJson("닉네임", "\"" + LocalDate.now(KST).minusDays(1) + "\"", "RECOMMEND", "송도 카페 A"),
				guestJson("닉네임", day(2), "RECOMMEND", null),
				guestJson("닉네임", day(2), "UNKNOWN", "송도 카페 A"),
		};
		for (String body : badBodies) {
			submit(meeting.inviteToken(), guestId, body).andExpect(status().isBadRequest());
		}
	}

	@Test
	void unknownInviteToken_isNotFound() throws Exception {
		mockMvc.perform(get("/api/invites/does-not-exist"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MEETING_NOT_FOUND"));
		submit("does-not-exist", UUID.randomUUID().toString(), guestJson("닉네임", day(2), "OPEN", null))
				.andExpect(status().isNotFound());
	}

	@Test
	void afterDeadline_submissionsAreRejected_butViewingStillWorks() throws Exception {
		String host = newHostToken("호스트");
		Created meeting = createDefaultMeeting(host);
		String guestId = UUID.randomUUID().toString();
		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(2), "RECOMMEND", "송도 카페 A")).andExpect(status().isCreated());

		setDueDate(meeting.id(), Instant.now().minusSeconds(60));

		submit(meeting.inviteToken(), guestId, guestJson("게스트", day(3), "RECOMMEND", "송도 카페 A"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEETING_CLOSED"));
		submit(meeting.inviteToken(), UUID.randomUUID().toString(), guestJson("신규", day(3), "RECOMMEND", "송도 카페 A"))
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
		submit(meeting.inviteToken(), UUID.randomUUID().toString(), guestJson("게스트", day(2), "RECOMMEND", "송도 카페 A"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEETING_CLOSED"));
	}

	@Test
	void expiredInviteLink_isGone() throws Exception {
		Created meeting = createDefaultMeeting(newHostToken("호스트"));
		jdbc.update("update meeting set link_expiration_date = ? where id = ?",
				OffsetDateTime.ofInstant(Instant.now().minusSeconds(60), ZoneOffset.UTC), meeting.id());

		mockMvc.perform(get("/api/invites/" + meeting.inviteToken()))
				.andExpect(status().isGone()).andExpect(jsonPath("$.code").value("INVITE_EXPIRED"));
		submit(meeting.inviteToken(), UUID.randomUUID().toString(), guestJson("게스트", day(2), "RECOMMEND", "송도 카페 A"))
				.andExpect(status().isGone());
	}

	// ---- A2: 호스트 응답/정책 수정 (PUT /api/meetings/{id}/response) ----

	private static String hostUpdateJson(String policy, String datesJson, String mode, String place) {
		return """
				{"placePolicy":%s,"selectedDates":[%s],"placeMode":"%s","region":"송도","placePreference":%s}
				""".formatted(policy, datesJson, mode, q(place));
	}

	private ResultActions updateHost(String token, long meetingId, String body) throws Exception {
		return mockMvc.perform(put("/api/meetings/" + meetingId + "/response")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void hostUpdatesOwnResponse_whenPolicyIsUnchanged() throws Exception {
		String host = newHostToken("호스트");
		Created meeting = createDefaultMeeting(host);

		updateHost(host, meeting.id(), hostUpdateJson(RECOMMEND_POLICY, day(5) + "," + day(6), "RECOMMEND", "송도 카페 C"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meetingId").value(meeting.id()))
				.andExpect(jsonPath("$.participantCount").value(1))
				.andExpect(jsonPath("$.responseCount").value(1))
				.andExpect(jsonPath("$.placePolicy.mode").value("RECOMMEND"))
				.andExpect(jsonPath("$.placePolicy.options", hasSize(3)))
				.andExpect(jsonPath("$.myResponse.nickname").value("호스트"))
				.andExpect(jsonPath("$.myResponse.selectedDates", hasSize(2)))
				.andExpect(jsonPath("$.myResponse.selectedDates[0]").value(LocalDate.now(KST).plusDays(5).toString()))
				.andExpect(jsonPath("$.myResponse.placePreference").value("송도 카페 C"));

		mockMvc.perform(get("/api/meetings/" + meeting.id()).header("Authorization", "Bearer " + host))
				.andExpect(jsonPath("$.myResponse.placePreference").value("송도 카페 C"))
				.andExpect(jsonPath("$.myResponse.selectedDates[1]").value(LocalDate.now(KST).plusDays(6).toString()));
	}

	@Test
	void hostMayChangePolicy_untilTheFirstGuestResponds() throws Exception {
		String host = newHostToken("호스트");
		Created meeting = createDefaultMeeting(host);

		// 게스트가 없으면 정책을 바꿀 수 있다.
		updateHost(host, meeting.id(), hostUpdateJson(FIXED_POLICY, day(2), "FIXED", null))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.placePolicy.mode").value("FIXED"))
				.andExpect(jsonPath("$.placePolicy.fixedPlace").value("홍대 스페이스 룸"))
				.andExpect(jsonPath("$.placePolicy.region").isEmpty())
				.andExpect(jsonPath("$.placePolicy.options", hasSize(0)))
				.andExpect(jsonPath("$.myResponse.placeMode").value("FIXED"))
				.andExpect(jsonPath("$.myResponse.placePreference").value("홍대 스페이스 룸"));
		mockMvc.perform(get("/api/invites/" + meeting.inviteToken()))
				.andExpect(jsonPath("$.placePolicy.mode").value("FIXED"));

		// 게스트가 새 정책으로 응답하면 정책은 잠긴다.
		submit(meeting.inviteToken(), UUID.randomUUID().toString(), guestJson("게스트", day(3), "FIXED", "홍대 스페이스 룸"))
				.andExpect(status().isCreated());

		updateHost(host, meeting.id(), hostUpdateJson(RECOMMEND_POLICY, day(2), "RECOMMEND", "송도 카페 A"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("POLICY_LOCKED"));
		updateHost(host, meeting.id(), hostUpdateJson("{\"mode\":\"FIXED\",\"fixedPlace\":\"다른 장소\"}", day(2), "FIXED", null))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("POLICY_LOCKED"));
		mockMvc.perform(get("/api/meetings/" + meeting.id()).header("Authorization", "Bearer " + host))
				.andExpect(jsonPath("$.placePolicy.mode").value("FIXED"))
				.andExpect(jsonPath("$.placePolicy.fixedPlace").value("홍대 스페이스 룸"));

		// 정책이 같으면 게스트가 있어도 호스트 본인의 응답은 수정할 수 있다.
		updateHost(host, meeting.id(), hostUpdateJson(FIXED_POLICY, day(7) + "," + day(8), "FIXED", null))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.myResponse.selectedDates", hasSize(2)))
				.andExpect(jsonPath("$.responseCount").value(2));
	}

	@Test
	void recommendOptions_reorderOrEdit_countsAsPolicyChange() throws Exception {
		String host = newHostToken("호스트");
		Created meeting = createDefaultMeeting(host);
		submit(meeting.inviteToken(), UUID.randomUUID().toString(), guestJson("게스트", day(3), "RECOMMEND", "송도 카페 B"))
				.andExpect(status().isCreated());

		String fewerOptions = """
				{"mode":"RECOMMEND","region":"송도","radius":3,"keyword":"카페",
				 "options":[{"name":"송도 카페 A","distance":"0.5km"},{"name":"송도 카페 B","distance":"1.0km"}]}
				""";
		updateHost(host, meeting.id(), hostUpdateJson(fewerOptions, day(2), "RECOMMEND", "송도 카페 A"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("POLICY_LOCKED"));
		// 후보의 id 유무와 상관없이 같은 정책으로 인식한다(id는 저장하지 않음).
		updateHost(host, meeting.id(), hostUpdateJson(RECOMMEND_POLICY.replace("\"id\":1,", ""), day(2), "RECOMMEND", "송도 카페 A"))
				.andExpect(status().isOk());
	}

	@Test
	void hostUpdate_requiresAuthOwnershipAndOpenMeeting() throws Exception {
		String owner = newHostToken("주인");
		String other = newHostToken("타인");
		Created meeting = createDefaultMeeting(owner);
		String body = hostUpdateJson(RECOMMEND_POLICY, day(2), "RECOMMEND", "송도 카페 A");

		mockMvc.perform(put("/api/meetings/" + meeting.id() + "/response")
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
		updateHost(other, meeting.id(), body)
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
		updateHost(owner, 999999, body)
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MEETING_NOT_FOUND"));

		setDueDate(meeting.id(), Instant.now().minusSeconds(60));
		updateHost(owner, meeting.id(), body)
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEETING_CLOSED"));

		Created closed = createDefaultMeeting(owner);
		jdbc.update("update meeting set status = 'CLOSED' where id = ?", closed.id());
		updateHost(owner, closed.id(), body)
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEETING_CLOSED"));
	}

	@Test
	void hostUpdate_rejectsInvalidInput() throws Exception {
		String host = newHostToken("호스트");
		Created meeting = createDefaultMeeting(host);
		String[] badBodies = {
				// 후보 범위(내일~10일 뒤) 밖 날짜
				hostUpdateJson(RECOMMEND_POLICY, day(11), "RECOMMEND", "송도 카페 A"),
				hostUpdateJson(RECOMMEND_POLICY, day(0), "RECOMMEND", "송도 카페 A"),
				hostUpdateJson(RECOMMEND_POLICY, "", "RECOMMEND", "송도 카페 A"),
				// 정책과 어긋난 호스트 응답
				hostUpdateJson(RECOMMEND_POLICY, day(2), "RECOMMEND", "목록에 없음"),
				hostUpdateJson(RECOMMEND_POLICY, day(2), "OPEN", null),
				hostUpdateJson(RECOMMEND_POLICY, day(2), "PROPOSE", "송도 카페 A"),
				// 잘못된 정책 입력
				hostUpdateJson("{\"mode\":\"FIXED\"}", day(2), "FIXED", null),
				hostUpdateJson("{\"mode\":\"RECOMMEND\",\"radius\":3,\"options\":[{\"name\":\"a\"}]}", day(2), "RECOMMEND", "a"),
				"{\"selectedDates\":[" + day(2) + "],\"placeMode\":\"OPEN\"}",
		};
		for (String body : badBodies) {
			updateHost(host, meeting.id(), body).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}
		// OPEN 정책에서 호스트는 OPEN만 가능
		updateHost(host, meeting.id(), hostUpdateJson(OPEN_POLICY, day(2), "PROPOSE", "호스트 제안"))
				.andExpect(status().isBadRequest());
		// 실패한 요청은 기존 응답을 바꾸지 않는다.
		mockMvc.perform(get("/api/meetings/" + meeting.id()).header("Authorization", "Bearer " + host))
				.andExpect(jsonPath("$.placePolicy.mode").value("RECOMMEND"))
				.andExpect(jsonPath("$.myResponse.placePreference").value("송도 카페 A"));
	}
}
