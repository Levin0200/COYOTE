package com.coyote.moyeon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoyeonApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoyeonApplication.class, args);
	}

}
