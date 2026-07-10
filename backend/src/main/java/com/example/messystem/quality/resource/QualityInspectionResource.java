package com.example.messystem.quality.resource;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.quality.entity.MesQualityInspection;
import com.example.messystem.quality.entity.MesQualityInspectionItem;
import com.example.messystem.quality.service.QualityInspectionService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;

@Path("/quality-inspections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QualityInspectionResource {

    private final QualityInspectionService service = new QualityInspectionService();

    @GET
    public ApiResponse<List<MesQualityInspection>> list() {
        try {
            return ApiResponse.ok(service.listInspections());
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/{id}")
    public ApiResponse<MesQualityInspection> get(@PathParam("id") long id) {
        try {
            return service.getInspectionById(id)
                    .map(ApiResponse::ok)
                    .orElseGet(() -> ApiResponse.fail("Inspection not found"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    public ApiResponse<Long> create(MesQualityInspection inspection) {
        try {
            long id = service.createInspection(inspection);
            return ApiResponse.ok(id);
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/items")
    public ApiResponse<Long> addItem(@PathParam("id") long id, MesQualityInspectionItem item) {
        try {
            if (item == null) {
                throw new BadRequestException("Item body is required");
            }
            long itemId = service.addInspectionItem(new MesQualityInspectionItem(
                    null,
                    id,
                    item.itemCode(),
                    item.itemName(),
                    item.standardValue(),
                    item.actualValue(),
                    item.itemResult(),
                    item.remark()
            ));
            return ApiResponse.ok(itemId);
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/judge")
    public ApiResponse<Boolean> judge(@PathParam("id") long id, QualityJudgement judgement) {
        try {
            if (judgement == null || judgement.status() == null || judgement.result() == null) {
                throw new BadRequestException("Judgement status and result are required");
            }
            return ApiResponse.ok(service.judgeInspection(id, judgement.status(), judgement.result()));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    public record QualityJudgement(String status, String result) {
    }
}
