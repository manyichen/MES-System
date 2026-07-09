package com.example.messystem.dashboard.service;

import com.example.messystem.dashboard.dao.ManagementFeedbackDao;
import com.example.messystem.dashboard.entity.MesManagementFeedback;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ManagementFeedbackService {

    private final ManagementFeedbackDao feedbackDao = new ManagementFeedbackDao();

    public long createFeedback(MesManagementFeedback feedback) throws SQLException {
        return feedbackDao.insert(feedback);
    }

    public Optional<MesManagementFeedback> getFeedback(long id) throws SQLException {
        return feedbackDao.findById(id);
    }

    public List<MesManagementFeedback> listFeedbackForWorkOrder(long workOrderId) throws SQLException {
        return feedbackDao.findByWorkOrderId(workOrderId);
    }

    public long createDefaultFeedback(long orderId, long taskId, long workOrderId, String type, String content) throws SQLException {
        MesManagementFeedback feedback = new MesManagementFeedback(
                null,
                "FB-" + System.currentTimeMillis(),
                orderId,
                taskId,
                workOrderId,
                type,
                content,
                "OPEN",
                LocalDateTime.now(),
                null
        );
        return feedbackDao.insert(feedback);
    }

    public boolean closeFeedback(long id) throws SQLException {
        return feedbackDao.close(id);
    }
}
