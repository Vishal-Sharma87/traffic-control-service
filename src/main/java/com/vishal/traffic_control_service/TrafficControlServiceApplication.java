package com.vishal.traffic_control_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class TrafficControlServiceApplication {


	public static void main(String[] args) {

		SpringApplication.run(TrafficControlServiceApplication.class, args);

	}
}
