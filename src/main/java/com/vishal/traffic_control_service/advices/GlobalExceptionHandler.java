package com.vishal.traffic_control_service.advices;

import com.vishal.traffic_control_service.advices.exceptions.JobExpiredOrNotExistsException;
import com.vishal.traffic_control_service.advices.exceptions.MainQueueFullException;
import com.vishal.traffic_control_service.enums.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MainQueueFullException.class)
    public ResponseEntity<ApiError> handleMainQueueIsFullException(MainQueueFullException e){
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.builder()
                        .message(e.getMessage())
                        .timestamp(Instant.now())
                        .errorCode(ErrorCode.QUEUE_FULL.toString())
                        .build()
                );
    }

    @ExceptionHandler(JobExpiredOrNotExistsException.class)
    public ResponseEntity<ApiError> handleJobExpiredOrNotExistsException(JobExpiredOrNotExistsException e){
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.builder()
                        .message(e.getMessage())
                        .timestamp(Instant.now())
                        .errorCode(ErrorCode.JOB_EXPIRED_OR_NOT_EXISTS.toString())
                        .build()
                );
    }
}
