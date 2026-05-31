package coyote.meeting_service.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class MeetingUpdateRequest {

    private String title;

    private String category;

    private String description;

}
