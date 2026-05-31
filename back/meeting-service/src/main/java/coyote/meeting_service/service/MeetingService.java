package coyote.meeting_service.service;

import coyote.meeting_service.dto.MeetingCreateResponse;
import coyote.meeting_service.dto.MeetingRequest;
import coyote.meeting_service.entity.Meeting;
import coyote.meeting_service.repository.MeetingRepository;
import org.springframework.stereotype.Service;


//@Service의 의미는 이 클래스는 비즈니스 로직 담당이라는걸 스프링에게 알려줌
@Service
public class MeetingService {

    //final == const 상수 선언
    private final MeetingRepository meetingRepository;

    //생성자 주입 Spring이 MeetingRepository를 넣어줌
    public MeetingService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    
    //실제 모임 생성 로직
    public MeetingCreateResponse createMeeting(MeetingRequest request) {

        //Entity 객체 생성
        Meeting meeting = new Meeting();


        //dto -> Entity 데이터 이동
        meeting.setHostId(request.getHostId());
        meeting.setTitle(request.getTitle());
        meeting.setCategory(request.getCategory());
        meeting.setDescription(request.getDescription());

        //데이터 저장
        Meeting savedMeeting = meetingRepository.save(meeting);

        //응답 dto 생성 (프론트로 데이터 보낼 준비)
        return new MeetingCreateResponse(
                savedMeeting.getMeetingId(),
                savedMeeting.getTitle(),
                "모임이 생성되었습니다."
        );
    }
}