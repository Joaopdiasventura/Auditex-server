package dev.joaopdias.auditex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AuditexApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuditexApplication.class, args);
	}

}
