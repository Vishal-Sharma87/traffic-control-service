package com.vishal.traffic_control_service.advices.exceptions;

public class JobExpiredOrNotExistsException extends RuntimeException {

    public JobExpiredOrNotExistsException(String message){
        super(message);
    }
}
