package com.vishal.traffic_control_service.advices.exceptions;

public class MainQueueIsFullException extends RuntimeException {

    public MainQueueIsFullException(String message){
        super(message);
    }
}
