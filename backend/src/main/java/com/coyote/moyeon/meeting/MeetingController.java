package com.coyote.moyeon.meeting;

import com.coyote.moyeon.meeting.dto.CreateMeetingRequest;
import com.coyote.moyeon.meeting.dto.MeetingDetailResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

	@GetMapping("/{id}")
	public MeetingDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return meetingService.getForHost(Long.valueOf(jwt.getSubject()), id);
	}
}
