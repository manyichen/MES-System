package com.example.messystem.demo;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.dashboard.entity.MesManagementFeedback;
import com.example.messystem.dashboard.entity.MesProductTrace;
import com.example.messystem.dashboard.service.DashboardService;
import com.example.messystem.dashboard.service.ManagementFeedbackService;
import com.example.messystem.dashboard.service.ProductTraceService;
import com.example.messystem.equipment.entity.MesEquipment;
import com.example.messystem.equipment.entity.MesEquipmentRepairReport;
import com.example.messystem.equipment.entity.MesMaintenancePlan;
import com.example.messystem.equipment.service.EquipmentService;
import com.example.messystem.master.entity.MesProcessRoute;
import com.example.messystem.master.entity.MesProduct;
import com.example.messystem.master.entity.MesProductBom;
import com.example.messystem.master.entity.MesProductionLine;
import com.example.messystem.master.entity.MesUser;
import com.example.messystem.master.service.MasterDataService;
import com.example.messystem.master.service.UserService;
import com.example.messystem.planning.entity.MesCustomerOrder;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.MesWorkOrder;
import com.example.messystem.planning.service.CustomerOrderService;
import com.example.messystem.planning.service.KittingService;
import com.example.messystem.planning.service.ProductionTaskService;
import com.example.messystem.planning.service.WorkOrderService;
import com.example.messystem.production.entity.MesWorkReport;
import com.example.messystem.production.service.ProductionService;
import com.example.messystem.quality.entity.MesQualityInspection;
import com.example.messystem.quality.entity.MesQualityInspectionItem;
import com.example.messystem.quality.entity.MesReworkOrder;
import com.example.messystem.quality.service.QualityInspectionService;
import com.example.messystem.quality.service.ReworkOrderService;
import com.example.messystem.warehouse.entity.MesInventory;
import com.example.messystem.warehouse.entity.MesMaterial;
import com.example.messystem.warehouse.entity.MesMaterialRequisition;
import com.example.messystem.warehouse.entity.MesRobot;
import com.example.messystem.warehouse.entity.MesWarehouse;
import com.example.messystem.warehouse.entity.MesWarehouseLocation;
import com.example.messystem.warehouse.service.WarehouseService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class DemoServer {
    private static final UserService USER_SERVICE = new UserService();
    private static final MasterDataService MASTER_SERVICE = new MasterDataService();
    private static final CustomerOrderService ORDER_SERVICE = new CustomerOrderService();
    private static final ProductionTaskService TASK_SERVICE = new ProductionTaskService();
    private static final KittingService KITTING_SERVICE = new KittingService();
    private static final WorkOrderService WORK_ORDER_SERVICE = new WorkOrderService();
    private static final WarehouseService WAREHOUSE_SERVICE = new WarehouseService();
    private static final ProductionService PRODUCTION_SERVICE = new ProductionService();
    private static final QualityInspectionService QUALITY_SERVICE = new QualityInspectionService();
    private static final ReworkOrderService REWORK_SERVICE = new ReworkOrderService();
    private static final EquipmentService EQUIPMENT_SERVICE = new EquipmentService();
    private static final ProductTraceService PRODUCT_TRACE_SERVICE = new ProductTraceService();
    private static final DashboardService DASHBOARD_SERVICE = new DashboardService();
    private static final ManagementFeedbackService FEEDBACK_SERVICE = new ManagementFeedbackService();

    private final Path webRoot;

    public DemoServer(Path webRoot) {
        this.webRoot = webRoot;
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : intEnv("MES_PORT", 8080);
        String host = args.length > 1 ? args[1] : stringEnv("MES_HOST", "127.0.0.1");
        Path webRoot = Path.of("src", "main", "webapp").toAbsolutePath().normalize();
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        DemoServer demo = new DemoServer(webRoot);
        server.createContext("/", demo::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        String localUrl = "http://" + ("0.0.0.0".equals(host) ? "127.0.0.1" : host) + ":" + port + "/";
        System.out.println("MES demo server started: " + localUrl);
        String publicUrl = System.getenv("MES_PUBLIC_URL");
        if (publicUrl != null && !publicUrl.isBlank()) {
            System.out.println("Public URL: " + publicUrl);
        }
        System.out.println("Listening on " + host + ":" + port);
        System.out.println("Press Ctrl+C to stop.");
        if (isLoopback(host) && !"false".equalsIgnoreCase(System.getenv("MES_OPEN_BROWSER"))) {
            openBrowser(localUrl);
        }
    }

    private static int intEnv(String name, int fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
    }

    private static String stringEnv(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean isLoopback(String host) {
        return "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host);
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

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api")) {
                handleApi(exchange, path.substring(4));
            } else {
                handleStatic(exchange, path);
            }
        } catch (NotFoundException ex) {
            sendJson(exchange, 404, JsonCodec.fail(ex.getMessage()));
        } catch (BadRequestException | IllegalArgumentException ex) {
            sendJson(exchange, 400, JsonCodec.fail(ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            sendJson(exchange, 500, JsonCodec.fail(ex.getMessage()));
        } finally {
            exchange.close();
        }
    }

    private void handleStatic(HttpExchange exchange, String requestPath) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=UTF-8", JsonCodec.utf8("method not allowed"));
            return;
        }
        String cleanPath = requestPath.equals("/") ? "/index.html" : requestPath;
        Path file = webRoot.resolve(cleanPath.substring(1)).normalize();
        if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) {
            send(exchange, 404, "text/plain; charset=UTF-8", JsonCodec.utf8("not found"));
            return;
        }
        send(exchange, 200, contentType(file), Files.readAllBytes(file));
    }

    private void handleApi(HttpExchange exchange, String path) throws Exception {
        String method = exchange.getRequestMethod();
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String json = switchApi(method, path.isBlank() ? "/" : path, query, readBody(exchange));
        sendJson(exchange, 200, json);
    }

    private String switchApi(String method, String path, Map<String, String> query, String body) throws Exception {
        if (path.equals("/users")) {
            if (isGet(method)) return ok(USER_SERVICE.listUsers());
            if (isPost(method)) return created(USER_SERVICE.createUser(parse(body, MesUser.class)));
        }
        if (path.equals("/products")) {
            if (isGet(method)) return ok(MASTER_SERVICE.listProducts());
            if (isPost(method)) return created(MASTER_SERVICE.createProduct(parse(body, MesProduct.class)));
        }
        if (path.matches("/products/\\d+/bom")) {
            long id = idAt(path, 1);
            if (isGet(method)) return ok(MASTER_SERVICE.listBom(id));
            if (isPost(method)) return created(MASTER_SERVICE.createBom(id, parse(body, MesProductBom.class)));
        }
        if (path.equals("/process-routes")) {
            if (isGet(method)) return ok(MASTER_SERVICE.listProcessRoutes());
            if (isPost(method)) return created(MASTER_SERVICE.createProcessRoute(parse(body, MesProcessRoute.class)));
        }
        if (path.equals("/production-lines")) {
            if (isGet(method)) return ok(MASTER_SERVICE.listProductionLines());
            if (isPost(method)) return created(MASTER_SERVICE.createProductionLine(parse(body, MesProductionLine.class)));
        }
        if (path.equals("/sync-logs") && isGet(method)) return ok(MASTER_SERVICE.listSyncLogs());

        if (path.equals("/orders")) {
            if (isGet(method)) return ok(ORDER_SERVICE.listOrders());
            if (isPost(method)) return created(ORDER_SERVICE.createOrder(parse(body, MesCustomerOrder.class)));
        }
        if (path.matches("/orders/\\d+") && isGet(method)) return ok(ORDER_SERVICE.getOrder(idAt(path, 1)));

        if (path.equals("/production-tasks")) {
            if (isGet(method)) return ok(TASK_SERVICE.listTasks());
            if (isPost(method)) return created(TASK_SERVICE.createTask(parse(body, MesProductionTask.class)));
        }
        if (path.equals("/production-tasks/kitting-analyses") && isGet(method)) return ok(KITTING_SERVICE.listAnalyses());
        if (path.equals("/production-tasks/shortage-alerts") && isGet(method)) return ok(KITTING_SERVICE.listAlerts());
        if (path.matches("/production-tasks/\\d+/kitting") && isPost(method)) return ok(KITTING_SERVICE.analyze(idAt(path, 1)));
        if (path.matches("/production-tasks/\\d+/release") && isPost(method)) return ok(TASK_SERVICE.releaseTask(idAt(path, 1)));

        if (path.equals("/work-orders")) {
            if (isGet(method)) return ok(WORK_ORDER_SERVICE.listWorkOrders());
            if (isPost(method)) return created(WORK_ORDER_SERVICE.createWorkOrder(parse(body, MesWorkOrder.class)));
        }
        if (path.matches("/work-orders/\\d+") && isGet(method)) return ok(WORK_ORDER_SERVICE.getWorkOrder(idAt(path, 1)));
        if (path.matches("/work-orders/\\d+/dispatch") && isPost(method)) {
            return ok(WORK_ORDER_SERVICE.dispatch(idAt(path, 1), longQuery(query, "operatorId")));
        }
        if (path.matches("/work-orders/\\d+/receive") && isPost(method)) {
            return ok(WORK_ORDER_SERVICE.receive(idAt(path, 1), longQuery(query, "operatorId")));
        }
        if (path.matches("/work-orders/\\d+/logs") && isGet(method)) return ok(WORK_ORDER_SERVICE.listLogs(idAt(path, 1)));

        if (path.equals("/materials")) {
            if (isGet(method)) return ok(WAREHOUSE_SERVICE.listMaterials());
            if (isPost(method)) return created(WAREHOUSE_SERVICE.createMaterial(parse(body, MesMaterial.class)));
        }
        if (path.equals("/warehouses")) {
            if (isGet(method)) return ok(WAREHOUSE_SERVICE.listWarehouses());
            if (isPost(method)) return created(WAREHOUSE_SERVICE.createWarehouse(parse(body, MesWarehouse.class)));
        }
        if (path.equals("/warehouses/locations")) {
            if (isGet(method)) return ok(WAREHOUSE_SERVICE.listLocations());
            if (isPost(method)) return created(WAREHOUSE_SERVICE.createLocation(parse(body, MesWarehouseLocation.class)));
        }
        if (path.equals("/inventory")) {
            if (isGet(method)) return ok(WAREHOUSE_SERVICE.listInventory());
            if (isPost(method)) return created(WAREHOUSE_SERVICE.createInventory(parse(body, MesInventory.class)));
        }
        if (path.equals("/inventory/transactions") && isGet(method)) return ok(WAREHOUSE_SERVICE.listTransactions());
        if (path.equals("/requisitions")) {
            if (isGet(method)) return ok(WAREHOUSE_SERVICE.listRequisitions());
            if (isPost(method)) return created(WAREHOUSE_SERVICE.createRequisition(parse(body, MesMaterialRequisition.class)));
        }
        if (path.matches("/requisitions/\\d+/approve") && isPost(method)) {
            return ok(WAREHOUSE_SERVICE.approveRequisition(idAt(path, 1), longQuery(query, "approvedBy")));
        }
        if (path.equals("/picking-tasks") && isGet(method)) return ok(WAREHOUSE_SERVICE.listPickingTasks());
        if (path.matches("/picking-tasks/\\d+/complete") && isPost(method)) return ok(WAREHOUSE_SERVICE.completePicking(idAt(path, 1)));
        if (path.equals("/robots")) {
            if (isGet(method)) return ok(WAREHOUSE_SERVICE.listRobots());
            if (isPost(method)) return created(WAREHOUSE_SERVICE.createRobot(parse(body, MesRobot.class)));
        }
        if (path.equals("/robot-delivery-tasks") && isGet(method)) return ok(WAREHOUSE_SERVICE.listDeliveryTasks());
        if (path.matches("/robot-delivery-tasks/\\d+/arrive") && isPost(method)) return ok(WAREHOUSE_SERVICE.markDeliveryArrived(idAt(path, 1)));

        if (path.equals("/work-reports")) {
            if (isGet(method)) return ok(PRODUCTION_SERVICE.listWorkReports());
            if (isPost(method)) return created(PRODUCTION_SERVICE.createWorkReport(parse(body, MesWorkReport.class)));
        }
        if (path.matches("/work-reports/\\d+/approve") && isPost(method)) return ok(PRODUCTION_SERVICE.approveWorkReport(idAt(path, 1)));
        if (path.equals("/piecework-wages") && isGet(method)) return ok(PRODUCTION_SERVICE.listWages());

        if (path.equals("/quality-inspections")) {
            if (isGet(method)) return ok(QUALITY_SERVICE.listInspections());
            if (isPost(method)) return created(QUALITY_SERVICE.createInspection(parse(body, MesQualityInspection.class)));
        }
        if (path.matches("/quality-inspections/\\d+") && isGet(method)) {
            return ok(QUALITY_SERVICE.getInspectionById(idAt(path, 1)).orElseThrow(() -> new NotFoundException("inspection not found")));
        }
        if (path.matches("/quality-inspections/\\d+/items") && isPost(method)) {
            long inspectionId = idAt(path, 1);
            MesQualityInspectionItem item = parse(body, MesQualityInspectionItem.class);
            item = new MesQualityInspectionItem(null, inspectionId, item.itemCode(), item.itemName(), item.standardValue(), item.actualValue(), item.itemResult(), item.remark());
            return created(QUALITY_SERVICE.addInspectionItem(item));
        }
        if (path.matches("/quality-inspections/\\d+/judge") && isPost(method)) {
            Map<String, Object> payload = JsonCodec.parseMap(body);
            String status = String.valueOf(payload.getOrDefault("status", "REWORK"));
            String result = String.valueOf(payload.getOrDefault("result", status));
            return ok(QUALITY_SERVICE.judgeInspection(idAt(path, 1), status, result));
        }
        if (path.equals("/quality-traces") && isGet(method)) return ok(QUALITY_SERVICE.listAllTraces());
        if (path.matches("/quality-traces/by-work-order/\\d+") && isGet(method)) return ok(QUALITY_SERVICE.listTracesByWorkOrder(idAt(path, 2)));
        if (path.matches("/quality-traces/by-inspection/\\d+") && isGet(method)) return ok(QUALITY_SERVICE.listTracesByInspection(idAt(path, 2)));

        if (path.equals("/rework-orders")) {
            if (isGet(method)) {
                Long inspectionId = longQuery(query, "inspectionId");
                return ok(inspectionId == null ? REWORK_SERVICE.listReworkOrders() : REWORK_SERVICE.listReworkOrdersByInspection(inspectionId));
            }
            if (isPost(method)) return created(REWORK_SERVICE.createReworkOrder(parse(body, MesReworkOrder.class)));
        }
        if (path.matches("/rework-orders/\\d+/dispatch") && isPost(method)) return ok(REWORK_SERVICE.updateStatus(idAt(path, 1), "DISPATCHED"));
        if (path.matches("/rework-orders/\\d+/finish") && isPost(method)) return ok(REWORK_SERVICE.updateStatus(idAt(path, 1), "FINISHED"));

        if (path.equals("/equipment")) {
            if (isGet(method)) return ok(EQUIPMENT_SERVICE.listEquipment());
            if (isPost(method)) return created(EQUIPMENT_SERVICE.createEquipment(parse(body, MesEquipment.class)));
        }
        if (path.matches("/equipment/by-line/\\d+") && isGet(method)) return ok(EQUIPMENT_SERVICE.listEquipmentByLine(idAt(path, 2)));
        if (path.matches("/equipment/\\d+/status") && isPost(method)) {
            Map<String, Object> payload = JsonCodec.parseMap(body);
            return ok(EQUIPMENT_SERVICE.updateEquipmentStatus(idAt(path, 1), String.valueOf(payload.get("status"))));
        }
        if (path.equals("/equipment-repair-reports")) {
            if (isGet(method)) return ok(EQUIPMENT_SERVICE.listRepairReports());
            if (isPost(method)) return created(EQUIPMENT_SERVICE.createRepairReport(parse(body, MesEquipmentRepairReport.class)));
        }
        if (path.matches("/equipment-repair-reports/\\d+/approve") && isPost(method)) return ok(EQUIPMENT_SERVICE.approveRepairReport(idAt(path, 1)));
        if (path.matches("/equipment-repair-reports/\\d+/to-maintenance-order") && isPost(method)) return created(EQUIPMENT_SERVICE.convertRepairReportToMaintenanceOrder(idAt(path, 1)));
        if (path.equals("/maintenance-orders") && isGet(method)) return ok(EQUIPMENT_SERVICE.listMaintenanceOrders());
        if (path.matches("/maintenance-orders/\\d+/assign") && isPost(method)) return ok(EQUIPMENT_SERVICE.updateMaintenanceOrderStatus(idAt(path, 1), "ASSIGNED"));
        if (path.matches("/maintenance-orders/\\d+/finish") && isPost(method)) return ok(EQUIPMENT_SERVICE.updateMaintenanceOrderStatus(idAt(path, 1), "FINISHED"));
        if (path.matches("/maintenance-orders/\\d+/accept") && isPost(method)) return ok(EQUIPMENT_SERVICE.updateMaintenanceOrderStatus(idAt(path, 1), "ACCEPTED"));
        if (path.equals("/maintenance-plans")) {
            if (isGet(method)) return ok(EQUIPMENT_SERVICE.listMaintenancePlans());
            if (isPost(method)) return created(EQUIPMENT_SERVICE.createMaintenancePlan(parse(body, MesMaintenancePlan.class)));
        }

        if (path.equals("/product-traces")) {
            if (isGet(method)) return ok(PRODUCT_TRACE_SERVICE.listProductTraces());
            if (isPost(method)) return created(PRODUCT_TRACE_SERVICE.createProductTrace(parse(body, MesProductTrace.class)));
        }
        if (path.matches("/product-traces/[^/]+") && isGet(method)) return ok(PRODUCT_TRACE_SERVICE.getProductTrace(partAt(path, 1)).orElseThrow(() -> new NotFoundException("product trace not found")));
        if (path.matches("/product-traces/work-orders/\\d+") && isGet(method)) return ok(PRODUCT_TRACE_SERVICE.listTracesByWorkOrder(idAt(path, 2)));
        if (path.equals("/dashboard/summary") && isGet(method)) return ok(DASHBOARD_SERVICE.listMetrics());
        if (path.equals("/dashboard/quality") && isGet(method)) return ok(DASHBOARD_SERVICE.listMetrics());
        if (path.equals("/dashboard/equipment") && isGet(method)) return ok(DASHBOARD_SERVICE.listMetrics());
        if (path.equals("/dashboard/production") && isGet(method)) return ok(DASHBOARD_SERVICE.listMetrics());
        if (path.equals("/dashboard/metrics")) {
            if (isGet(method)) return ok(DASHBOARD_SERVICE.listMetrics());
            if (isPost(method)) return created(DASHBOARD_SERVICE.createMetric(parse(body, com.example.messystem.dashboard.entity.MesDashboardMetric.class)));
        }
        if (path.equals("/management-feedback")) {
            if (isGet(method)) return ok(FEEDBACK_SERVICE.listFeedbackForWorkOrder(longQuery(query, "workOrderId") == null ? 1L : longQuery(query, "workOrderId")));
            if (isPost(method)) return created(FEEDBACK_SERVICE.createFeedback(parse(body, MesManagementFeedback.class)));
        }
        if (path.matches("/management-feedback/\\d+/close") && isPost(method)) return ok(FEEDBACK_SERVICE.closeFeedback(idAt(path, 1)));

        throw new NotFoundException("api not found: " + path);
    }

    private static boolean isGet(String method) {
        return "GET".equals(method);
    }

    private static boolean isPost(String method) {
        return "POST".equals(method);
    }

    private static String ok(Object data) {
        return JsonCodec.ok("ok", data);
    }

    private static String created(Object data) {
        return JsonCodec.ok("created", data);
    }

    private static <T> T parse(String body, Class<T> type) {
        return JsonCodec.parseObject(body, type);
    }

    private static Long longQuery(Map<String, String> query, String name) {
        String value = query.get(name);
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    private static long idAt(String path, int index) {
        return Long.parseLong(partAt(path, index));
    }

    private static String partAt(String path, int index) {
        String[] parts = path.substring(1).split("/");
        return parts[index];
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> result = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return result;
        }
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            result.put(decode(parts[0]), parts.length > 1 ? decode(parts[1]) : "");
        }
        return result;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        send(exchange, status, "application/json; charset=UTF-8", JsonCodec.utf8(json));
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html; charset=UTF-8";
        if (name.endsWith(".css")) return "text/css; charset=UTF-8";
        if (name.endsWith(".js")) return "application/javascript; charset=UTF-8";
        return "application/octet-stream";
    }
}
