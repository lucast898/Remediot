package br.edu.remediot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RemedIoTApplication {

	public static void main(String[] args) {
		SpringApplication.run(RemedIoTApplication.class, args);
	}

}
