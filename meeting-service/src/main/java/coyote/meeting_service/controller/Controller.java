package coyote.meeting_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class Controller {

    //localhost:8080/test

    @GetMapping("/test")
    public String test() {
        return "server ok";
    }
}