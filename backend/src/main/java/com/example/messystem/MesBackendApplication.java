package com.example.messystem;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.servlet.ServletContainer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MesBackendApplication {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : intEnv("MES_PORT", 8080);
        String host = args.length > 1 ? args[1] : stringEnv("MES_HOST", "127.0.0.1");
        Path webRoot = findWebRoot();

        Tomcat tomcat = new Tomcat();
        tomcat.setHostname(host);
        tomcat.setPort(port);
        tomcat.getConnector();

        Context context = tomcat.addContext("", webRoot.toString());
        context.addMimeMapping("html", "text/html;charset=UTF-8");
        context.addMimeMapping("css", "text/css;charset=UTF-8");
        context.addMimeMapping("js", "application/javascript;charset=UTF-8");
        Wrapper jersey = Tomcat.addServlet(context, "Jersey", new ServletContainer(new MesApplication()));
        jersey.setLoadOnStartup(1);
        context.addServletMappingDecoded("/api/*", "Jersey");
        context.addWelcomeFile("index.html");

        Wrapper staticFiles = Tomcat.addServlet(context, "staticFiles", new StaticFileServlet(webRoot));
        staticFiles.setLoadOnStartup(2);
        context.addServletMappingDecoded("/", "staticFiles");

        tomcat.start();

        String localUrl = "http://" + displayHost(host) + ":" + port + "/";
        System.out.println("MES backend started: " + localUrl);
        System.out.println("Web root: " + webRoot);
        System.out.println("Listening on " + host + ":" + port);
        String publicUrl = configValue("MES_PUBLIC_URL");
        if (publicUrl != null && !publicUrl.isBlank()) {
            System.out.println("Public URL: " + publicUrl);
        }
        System.out.println("Press Ctrl+C to stop.");

        if (isLoopback(host) && !"false".equalsIgnoreCase(configValue("MES_OPEN_BROWSER"))) {
            openBrowser(localUrl);
        }

        tomcat.getServer().await();
    }

    private static Path findWebRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        Path[] candidates = {
                current.resolve("../frontend"),
                current.resolve("frontend"),
                current.resolve("src/main/webapp")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate.resolve("index.html"))) {
                return candidate.normalize();
            }
        }
        throw new IllegalStateException("Cannot find src/main/webapp/index.html from " + current);
    }

    private static int intEnv(String name, int fallback) {
        String value = configValue(name);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
    }

    private static String stringEnv(String name, String fallback) {
        String value = configValue(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String configValue(String name) {
        String value = System.getProperty(name);
        return value == null || value.isBlank() ? System.getenv(name) : value;
    }

    private static String displayHost(String host) {
        return "0.0.0.0".equals(host) ? "127.0.0.1" : host;
    }

    private static boolean isLoopback(String host) {
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (Exception ex) {
            return "localhost".equalsIgnoreCase(host);
        }
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {
            // The URL is printed above for environments that cannot open a browser.
        }
    }

    private static final class StaticFileServlet extends HttpServlet {
        private final Path webRoot;

        private StaticFileServlet(Path webRoot) {
            this.webRoot = webRoot.toAbsolutePath().normalize();
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            String requestPath = URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8);
            if (requestPath == null || requestPath.isBlank() || "/".equals(requestPath)) {
                requestPath = "/index.html";
            }
            Path file = webRoot.resolve(requestPath.substring(1)).normalize();
            if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(contentType(file));
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setContentLengthLong(Files.size(file));
            try (OutputStream out = response.getOutputStream()) {
                Files.copy(file, out);
            }
        }

        private String contentType(Path file) throws IOException {
            String name = file.getFileName().toString().toLowerCase();
            if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html;charset=UTF-8";
            if (name.endsWith(".css")) return "text/css;charset=UTF-8";
            if (name.endsWith(".js")) return "application/javascript;charset=UTF-8";
            if (name.endsWith(".json")) return "application/json;charset=UTF-8";
            if (name.endsWith(".svg")) return "image/svg+xml";
            if (name.endsWith(".png")) return "image/png";
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
            String probed = Files.probeContentType(file);
            return probed == null ? "application/octet-stream" : probed;
        }
    }
}
