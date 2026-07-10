package com.example.messystem.quality.service;

import com.example.messystem.quality.dao.ReworkOrderDao;
import com.example.messystem.quality.entity.MesReworkOrder;

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
}
