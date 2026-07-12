package com.example.messystem.common;

import jakarta.ws.rs.core.Response;

public final class ResourceSupport {
    private ResourceSupport() {
    }

    public static Response ok(Object data) {
        return Response.ok(ApiResponse.ok(data)).build();
    }

    public static Response created(String message, Object data) {
        return Response.status(Response.Status.CREATED).entity(ApiResponse.ok(message, data)).build();
    }

    public static Response action(String message, Object data) {
        return Response.ok(ApiResponse.ok(message, data)).build();
    }

    public static Response handle(RuntimeException ex) {
        String message = ChineseMessageSupport.translate(ex.getMessage());
        if (ex instanceof NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND).entity(ApiResponse.fail(message)).build();
        }
        if (ex instanceof BadRequestException) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.fail(message)).build();
        }
        return Response.serverError().entity(ApiResponse.fail(message)).build();
    }
}
