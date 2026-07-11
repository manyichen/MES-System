package com.example.messystem.security;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.ApiResponse;
import com.example.messystem.security.DataScopeService.ScopeSnapshot;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Provider
@Priority(Priorities.AUTHORIZATION + 10)
public class DataScopeFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final String SCOPE_PROPERTY = DataScopeFilter.class.getName() + ".scope";
    private final DataScopeService service = new DataScopeService();

    @Override
    public void filter(ContainerRequestContext request) {
        AuthenticatedUser user = AuthFilter.currentUser(request);
        if (user == null) return;
        ScopeSnapshot scope = service.snapshot(user);
        request.setProperty(SCOPE_PROPERTY, scope);
        if (!scope.restricted()) return;
        try {
            enforcePath(scope, normalize(request.getUriInfo().getPath()));
        } catch (RuntimeException ex) {
            request.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiResponse.fail(ex.getMessage())).build());
        }
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        ScopeSnapshot scope = (ScopeSnapshot) request.getProperty(SCOPE_PROPERTY);
        if (scope == null || !scope.restricted() || response.getStatus() >= 400) return;
        Object entity = response.getEntity();
        if (!(entity instanceof ApiResponse api) || api.data == null) return;
        if (api.data instanceof List<?> list) {
            List<Object> filtered = new ArrayList<>();
            for (Object item : list) if (scope.canView(item)) filtered.add(item);
            api.data = filtered;
        } else if (!scope.canView(api.data)) {
            response.setStatusInfo(Response.Status.FORBIDDEN);
            response.setEntity(ApiResponse.fail("数据不在当前用户的授权范围内"));
        }
    }

    private static void enforcePath(ScopeSnapshot scope, String path) {
        Long id;
        if ((id = match(path, "^work-orders/(\\d+)(?:/.*)?$")) != null) scope.requireWorkOrder(id);
        else if ((id = match(path, "^work-reports/by-work-order/(\\d+)$")) != null) scope.requireWorkOrder(id);
        else if ((id = match(path, "^work-reports/(\\d+)(?:/.*)?$")) != null) scope.requireReport(id);
        else if ((id = match(path, "^quality-inspections/(\\d+)(?:/.*)?$")) != null) scope.requireInspection(id);
        else if ((id = match(path, "^rework-orders/(\\d+)(?:/.*)?$")) != null) scope.requireRework(id);
        else if ((id = match(path, "^equipment/by-line/(\\d+)$")) != null) scope.requireLine(id);
        else if ((id = match(path, "^equipment/(\\d+)(?:/.*)?$")) != null) scope.requireEquipment(id);
        else if ((id = match(path, "^equipment-repair-reports/(\\d+)(?:/.*)?$")) != null) scope.requireRepair(id);
        else if ((id = match(path, "^maintenance-orders/(\\d+)(?:/.*)?$")) != null) scope.requireMaintenance(id);
        else if ((id = match(path, "^product-traces/work-orders/(\\d+)$")) != null) scope.requireWorkOrder(id);
        else if ((id = match(path, "^warehouses/locations/(\\d+)(?:/.*)?$")) != null) scope.requireWarehouseEntity("location", id);
        else if ((id = match(path, "^warehouses/(\\d+)(?:/.*)?$")) != null) scope.requireWarehouseEntity("warehouse", id);
        else if ((id = match(path, "^warehouse-locations/(\\d+)(?:/.*)?$")) != null) scope.requireWarehouseEntity("location", id);
        else if ((id = match(path, "^inventory/transactions/(\\d+)$")) != null) scope.requireWarehouseEntity("transaction", id);
        else if ((id = match(path, "^inventory/(\\d+)(?:/.*)?$")) != null) scope.requireWarehouseEntity("inventory", id);
        else if ((id = match(path, "^requisitions/(\\d+)(?:/.*)?$")) != null) scope.requireWarehouseEntity("requisition", id);
        else if ((id = match(path, "^picking-tasks/(\\d+)(?:/.*)?$")) != null) scope.requireWarehouseEntity("picking", id);
        else if ((id = match(path, "^robot-delivery-tasks/(\\d+)(?:/.*)?$")) != null) scope.requireWarehouseEntity("delivery", id);
        else if ((id = match(path, "^robots/(\\d+)(?:/.*)?$")) != null) scope.requireWarehouseEntity("robot", id);
    }

    private static Long match(String value, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(value);
        return matcher.matches() ? Long.valueOf(matcher.group(1)) : null;
    }

    private static String normalize(String path) {
        return path == null ? "" : path.replaceFirst("^/+", "").replaceFirst("/+$", "");
    }
}
