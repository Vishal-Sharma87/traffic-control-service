package com.vishal.traffic_control_service.advices.exceptions;

public class JobNotFinishedException extends RuntimeException {

    public JobNotFinishedException(String message){
        super(message);
    }
}
