package coyote.meeting_service.controller;

import coyote.meeting_service.dto.MeetingCreateResponse;
import coyote.meeting_service.dto.MeetingReadResponse;
import coyote.meeting_service.dto.MeetingCreateRequest;
import coyote.meeting_service.dto.MeetingUpdateRequest;
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

    //Create (얘 주소 아이디는 AUTO_INCREMENT하게 부여됨)
    @PostMapping
    public MeetingCreateResponse createMeeting(
            @RequestBody MeetingCreateRequest request
    ) {
        return meetingService.createMeeting(request);
    }

    //Read
    @GetMapping("/{meetingId}")
    public MeetingReadResponse readMeeting (
            @PathVariable Long meetingId
    ) {
        return meetingService.readMeeting(meetingId);
    }

    //Update
    @PutMapping("/{meetingId}")
    public MeetingReadResponse updateMeeting(
            @PathVariable Long meetingId,
            @RequestBody MeetingUpdateRequest request
    ) {
        return meetingService.updateMeeting(
                meetingId,
                request
        );
    }

    //Delete
    @DeleteMapping("/{meetingId}")
    public String deleteMeeting(
            @PathVariable Long meetingId
    ) {
        return meetingService.deleteMeeting(meetingId);
    }
}