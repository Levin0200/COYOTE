package com.coyote.moyeon.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRequest(@NotBlank @Size(max = 20) String nickname) {
}
