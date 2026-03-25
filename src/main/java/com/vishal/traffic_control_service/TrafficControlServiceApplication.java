package com.vishal.traffic_control_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.support.ExecutorServiceAdapter;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class TrafficControlServiceApplication {
	@Value("${threads.processing-thread.threadCount}") int THREAD_COUNT;

	public static void main(String[] args) {

		SpringApplication.run(TrafficControlServiceApplication.class, args);

	}

	@Bean
	public ExecutorService getWorker(){

		return Executors.newFixedThreadPool(THREAD_COUNT);
	}
}
