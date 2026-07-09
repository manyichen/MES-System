package com.example.messystem.common;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class BadRequestException extends WebApplicationException {

    public BadRequestException(String message) {
        super(Response.status(Response.Status.BAD_REQUEST)
                .entity(ApiResponse.fail(message))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }
}
