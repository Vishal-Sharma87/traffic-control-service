package com.vishal.traffic_control_service.advices.exceptions;

public class SystemUnhealthyJobRejectedException extends RuntimeException {

    public SystemUnhealthyJobRejectedException(String message){
        super(message);
    }
}
