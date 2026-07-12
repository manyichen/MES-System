package com.example.messystem.common;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {
    @Override
    public Response toResponse(RuntimeException exception) {
        String message = ChineseMessageSupport.translate(exception.getMessage());
        if (exception instanceof NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND).entity(ApiResponse.fail(message)).build();
        }
        if (exception instanceof BadRequestException || exception instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.fail(message)).build();
        }
        return Response.serverError().entity(ApiResponse.fail(message)).build();
    }
}
