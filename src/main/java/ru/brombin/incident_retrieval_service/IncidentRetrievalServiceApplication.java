package ru.brombin.incident_retrieval_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class IncidentRetrievalServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IncidentRetrievalServiceApplication.class, args);
	}

}
