package com.vishal.traffic_control_service.advices.exceptions;

public class MainQueueFullException extends RuntimeException {

    public MainQueueFullException(String message){
        super(message);
    }
}
