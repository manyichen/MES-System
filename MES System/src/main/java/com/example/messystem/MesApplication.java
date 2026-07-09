package com.example.messystem;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api")
public class MesApplication extends ResourceConfig {
    public MesApplication() {
        packages("com.example.messystem");
    }
}
