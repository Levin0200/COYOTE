package com.coyote.moyeon.meeting;

import com.coyote.moyeon.meeting.dto.CreateMeetingRequest;
import com.coyote.moyeon.meeting.dto.MeetingDetailResponse;
import com.coyote.moyeon.meeting.dto.MeetingResultResponse;
import com.coyote.moyeon.meeting.dto.MeetingSummaryResponse;
import com.coyote.moyeon.meeting.dto.MeetingVotesResponse;
import com.coyote.moyeon.meeting.dto.UpdateHostResponseRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

	private final MeetingService meetingService;

	public MeetingController(MeetingService meetingService) {
		this.meetingService = meetingService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MeetingDetailResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateMeetingRequest request) {
		return meetingService.create(Long.valueOf(jwt.getSubject()), request);
	}

	@GetMapping
	public List<MeetingSummaryResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
		return meetingService.listMine(Long.valueOf(jwt.getSubject()));
	}

	@GetMapping("/{id}")
	public MeetingDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
		return meetingService.getForHost(Long.valueOf(jwt.getSubject()), id);
	}

	@PostMapping("/{id}/close")
	public MeetingDetailResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
		return meetingService.closeMeeting(Long.valueOf(jwt.getSubject()), id);
	}

	@GetMapping("/{id}/result")
	public MeetingResultResponse result(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
		return meetingService.getResultForHost(Long.valueOf(jwt.getSubject()), id);
	}

	@GetMapping("/{id}/votes")
	public MeetingVotesResponse votes(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
		return meetingService.getVotesForHost(Long.valueOf(jwt.getSubject()), id);
	}

	@PutMapping("/{id}/response")
	public MeetingDetailResponse updateHostResponse(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id,
			@Valid @RequestBody UpdateHostResponseRequest request) {
		return meetingService.updateHostResponse(Long.valueOf(jwt.getSubject()), id, request);
	}
}
