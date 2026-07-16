package com.example.messystem.quality.service;

import com.example.messystem.quality.dao.ReworkOrderDao;
import com.example.messystem.quality.entity.MesReworkOrder;
import com.example.messystem.common.BadRequestException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ReworkOrderService {

    private final ReworkOrderDao dao = new ReworkOrderDao();

    public long createReworkOrder(MesReworkOrder order) throws SQLException {
        return dao.insert(order);
    }

    public Optional<MesReworkOrder> getReworkOrder(long id) throws SQLException {
        return dao.findById(id);
    }

    public List<MesReworkOrder> listReworkOrders() throws SQLException {
        return dao.findAll();
    }

    public List<MesReworkOrder> listReworkOrdersByInspection(long inspectionId) throws SQLException {
        return dao.findByInspectionId(inspectionId);
    }

    public boolean updateStatus(long id, String status) throws SQLException {
        return dao.updateStatus(id, status);
    }

    public boolean dispatch(long id) throws SQLException {
        if (!dao.updateStatus(id, "DISPATCHED", "PLANNED")) {
            throw new BadRequestException("只有已由 PMC 排产的返工单才能派发");
        }
        return true;
    }

    public boolean finish(long id) throws SQLException {
        if (!dao.updateStatus(id, "FINISHED", "DISPATCHED")) {
            throw new BadRequestException("只有已派发的返工单才能完成");
        }
        return true;
    }
}
