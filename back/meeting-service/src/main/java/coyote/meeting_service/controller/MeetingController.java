package coyote.meeting_service.controller;

import coyote.meeting_service.dto.MeetingCreateResponse;
import coyote.meeting_service.dto.MeetingRequest;
import coyote.meeting_service.service.MeetingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings")

// http://localhost:8080/api/meetings

//프론트에서 보낸 JSON 데이터를 MeetingController가 받은 뒤 MeetingRequest에 담음
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping
    public MeetingCreateResponse createMeeting(
            @RequestBody MeetingRequest request
    ) {
        return meetingService.createMeeting(request);
    }
}