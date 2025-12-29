package com.apporte.infrastructure.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class NotificationExceptionMapper implements ExceptionMapper<Exception> {
    
    private static final Logger LOG = LoggerFactory.getLogger(NotificationExceptionMapper.class);
    
    @Override
    public Response toResponse(Exception exception) {
        LOG.error("Unhandled exception: ", exception);
        
        ErrorResponse error = new ErrorResponse(
            "NOTIFICATION_ERROR",
            exception.getMessage(),
            System.currentTimeMillis()
        );
        
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                      .entity(error)
                      .build();
    }
    
    public static class ErrorResponse {
        public String code;
        public String message;
        public long timestamp;
        
        public ErrorResponse(String code, String message, long timestamp) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}