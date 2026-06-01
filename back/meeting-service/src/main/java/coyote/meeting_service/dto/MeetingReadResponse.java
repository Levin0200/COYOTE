package coyote.meeting_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeetingReadResponse {

    private Long meetingId;

    private Long hostId;

    private String title;

    private String category;

    private String description;

    private String status;
}