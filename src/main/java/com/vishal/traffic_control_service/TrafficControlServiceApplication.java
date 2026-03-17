package com.vishal.traffic_control_service;

import com.vishal.traffic_control_service.service.ResultService;
import com.vishal.traffic_control_service.service.QueueService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TrafficControlServiceApplication {

	public static void main(String[] args) {

		SpringApplication.run(TrafficControlServiceApplication.class, args);

	}

	@Bean
	public QueueService getQueue(){
		return new QueueService();
	}

	@Bean
	public ResultService getJobResponseService(){
		return new ResultService();
	}
}
