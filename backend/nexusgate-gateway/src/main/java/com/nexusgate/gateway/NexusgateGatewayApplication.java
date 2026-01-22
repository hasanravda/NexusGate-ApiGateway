package com.nexusgate.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NexusgateGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(NexusgateGatewayApplication.class, args);
	}

}
