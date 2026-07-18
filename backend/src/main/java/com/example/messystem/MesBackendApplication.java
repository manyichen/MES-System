/*
 * 答辩定位：MES 应用基础 模块的 MesBackendApplication。
 * 分层职责：运行基础设施：负责应用注册、服务器启动、配置读取或数据库连接，是业务模块共享的外部依赖边界。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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
import com.example.messystem.common.DbConfig;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.servlet.ServletContainer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 可执行后端入口：在同一个内嵌 Tomcat 中挂载 Jersey REST API 和 Vite 静态前端。
 * 本地可直接打开完整系统；生产通常仍由 Nginx 提供 dist 并反代 /api 到本进程。
 */
public class MesBackendApplication {
    /**
     * 启动流程：解析 host/port -> 定位 frontend/dist -> 初始化 Tomcat -> 注册 /api/* Jersey ->
     * 注册静态文件 Servlet -> 启动监听 -> 本地环境可自动打开浏览器 -> 阻塞等待关闭信号。
     */
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : intEnv("MES_PORT", 8080);
        String host = args.length > 1 ? args[1] : stringEnv("MES_HOST", "127.0.0.1");
        Path webRoot = findWebRoot();

        // 显式创建 Connector，否则仅设置端口并不一定触发 Tomcat 初始化监听器。
        Tomcat tomcat = new Tomcat();
        tomcat.setHostname(host);
        tomcat.setPort(port);
        tomcat.getConnector();

        // 追溯二维码优先使用 MES_PUBLIC_URL；未配置时使用本机可访问地址。
        String localUrl = "http://" + displayHost(host) + ":" + port;
        String configuredPublicUrl = DbConfig.getValue("MES_PUBLIC_URL", "");
        System.setProperty("MES_RUNTIME_PUBLIC_URL",
                configuredPublicUrl == null || configuredPublicUrl.isBlank() ? localUrl : configuredPublicUrl);

        // 根 Context 同时服务 API 和前端，浏览器请求同源，因此不需要额外 CORS 放行。
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

        System.out.println("MES backend started: " + localUrl + "/");
        System.out.println("Web root: " + webRoot);
        System.out.println("Listening on " + host + ":" + port);
        String publicUrl = DbConfig.getValue("MES_PUBLIC_URL", "");
        if (publicUrl != null && !publicUrl.isBlank()) {
            System.out.println("Public URL: " + publicUrl);
        }
        System.out.println("Press Ctrl+C to stop.");

        if (isLoopback(host) && !"false".equalsIgnoreCase(configValue("MES_OPEN_BROWSER"))) {
            openBrowser(localUrl + "/");
        }

        tomcat.getServer().await();
    }

    /**
     * 兼容从 backend、仓库根目录或标准 Web 工程目录启动；必须存在 index.html 才算有效构建。
     * 找不到时明确提示先运行前端 npm build，避免启动一个只有 API 的“空白站点”。
     */
    private static Path findWebRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        Path[] candidates = {
                current.resolve("../frontend/dist"),
                current.resolve("frontend/dist"),
                current.resolve("src/main/webapp")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate.resolve("index.html"))) {
                return candidate.normalize();
            }
        }
        throw new IllegalStateException("Cannot find the built Vue frontend. Run npm run build in frontend/: " + current);
    }

    /**
     * 内部实现步骤：执行 intEnv 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static int intEnv(String name, int fallback) {
        String value = configValue(name);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
    }

    /**
     * 内部实现步骤：执行 stringEnv 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static String stringEnv(String name, String fallback) {
        String value = configValue(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 内部实现步骤：执行 configValue 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static String configValue(String name) {
        String value = System.getProperty(name);
        return value == null || value.isBlank() ? System.getenv(name) : value;
    }

    /**
     * 内部实现步骤：执行 displayHost 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static String displayHost(String host) {
        return "0.0.0.0".equals(host) ? "127.0.0.1" : host;
    }

    /**
     * 内部实现步骤：执行 isLoopback 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static boolean isLoopback(String host) {
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (Exception ex) {
            return "localhost".equalsIgnoreCase(host);
        }
    }

    /**
     * 内部实现步骤：执行 openBrowser 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {
            // 无法自动打开浏览器时，可使用上方打印的地址手动访问。
        }
    }

    /**
     * 最小静态文件 Servlet。真实文件直接返回；Vue history 路由（无文件扩展名）回退 index.html；
     * normalize + startsWith(webRoot) 阻止 ../ 路径穿越读取项目外文件。
     */
    private static final class StaticFileServlet extends HttpServlet {
        private final Path webRoot;

        /**
         * 内部实现步骤：执行 StaticFileServlet 对应的业务步骤。
         * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
         */
        private StaticFileServlet(Path webRoot) {
            this.webRoot = webRoot.toAbsolutePath().normalize();
        }

        /** 解析并校验请求路径，设置 MIME/UTF-8/nosniff/长度后以流方式返回静态文件。 */
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            String requestPath = URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8);
            if (requestPath == null || requestPath.isBlank() || "/".equals(requestPath)) {
                requestPath = "/index.html";
            }
            Path file = webRoot.resolve(requestPath.substring(1)).normalize();
            // Vue Router 使用 history 模式，前端应用路由统一回退到 index.html。
            if (!Files.isRegularFile(file) && !requestPath.substring(requestPath.lastIndexOf('/') + 1).contains(".")) {
                file = webRoot.resolve("index.html");
            }
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

        /**
         * 内部实现步骤：执行 contentType 对应的业务步骤。
         * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
         */
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
