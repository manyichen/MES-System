package com.example.messystem.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.messystem.common.ApiResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AuthFilterTest {
    @Test
    void unauthenticatedBinaryEndpointStillReturnsJsonUnauthorizedResponse() throws Exception {
        AtomicReference<Response> aborted = new AtomicReference<>();
        UriInfo uriInfo = (UriInfo) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { UriInfo.class }, (proxy, method, args) -> {
                    if ("getPath".equals(method.getName())) return "tire-labels/1/qrcode";
                    if ("getRequestUri".equals(method.getName())) return URI.create("http://localhost/api/tire-labels/1/qrcode");
                    return defaultValue(method.getReturnType());
                });
        ContainerRequestContext context = (ContainerRequestContext) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { ContainerRequestContext.class }, (proxy, method, args) -> {
                    if ("getUriInfo".equals(method.getName())) return uriInfo;
                    if ("getMethod".equals(method.getName())) return "GET";
                    if ("getHeaderString".equals(method.getName())) return null;
                    if ("abortWith".equals(method.getName())) {
                        aborted.set((Response) args[0]);
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                });

        new AuthFilter().filter(context);

        Response response = aborted.get();
        assertNotNull(response);
        assertEquals(401, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertInstanceOf(ApiResponse.class, response.getEntity());
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return 0;
    }
}
