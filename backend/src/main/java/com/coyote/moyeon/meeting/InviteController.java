package com.coyote.moyeon.meeting;

import com.coyote.moyeon.meeting.dto.GuestResponseRequest;
import com.coyote.moyeon.meeting.dto.InviteInfoResponse;
import com.coyote.moyeon.meeting.dto.MeetingResultResponse;
import com.coyote.moyeon.meeting.dto.MyResponseView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invites/{inviteToken}")
public class InviteController {

	private static final String GUEST_ID_HEADER = "X-Guest-Id";

	private final MeetingService meetingService;

	public InviteController(MeetingService meetingService) {
		this.meetingService = meetingService;
	}

	@GetMapping
	public InviteInfoResponse info(@PathVariable String inviteToken) {
		return meetingService.getInvite(inviteToken);
	}

	@GetMapping("/result")
	public MeetingResultResponse result(@PathVariable String inviteToken) {
		return meetingService.getResultForGuest(inviteToken);
	}

	@PutMapping("/response")
	public ResponseEntity<MyResponseView> submit(@PathVariable String inviteToken,
			@RequestHeader(GUEST_ID_HEADER) String guestId, @Valid @RequestBody GuestResponseRequest request) {
		MeetingService.SubmitResult result = meetingService.submitGuestResponse(inviteToken, guestId, request);
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.response());
	}

	@GetMapping("/response/me")
	public MyResponseView myResponse(@PathVariable String inviteToken, @RequestHeader(GUEST_ID_HEADER) String guestId) {
		return meetingService.getMyResponse(inviteToken, guestId);
	}
}
