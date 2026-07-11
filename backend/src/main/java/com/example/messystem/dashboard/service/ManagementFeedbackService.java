package com.example.messystem.dashboard.service;

import com.example.messystem.dashboard.dao.ManagementFeedbackDao;
import com.example.messystem.dashboard.entity.MesManagementFeedback;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ManagementFeedbackService {

    private final ManagementFeedbackDao feedbackDao = new ManagementFeedbackDao();

    public long createFeedback(MesManagementFeedback feedback, long createdBy) throws SQLException {
        return feedbackDao.insert(feedback, createdBy);
    }

    public Optional<MesManagementFeedback> getFeedback(long id) throws SQLException {
        return feedbackDao.findById(id);
    }

    public List<MesManagementFeedback> listFeedbackForWorkOrder(long workOrderId) throws SQLException {
        return feedbackDao.findByWorkOrderId(workOrderId);
    }

    public List<MesManagementFeedback> listOwnFeedbackForWorkOrder(long workOrderId, long userId) throws SQLException {
        return feedbackDao.findByWorkOrderIdAndCreator(workOrderId, userId);
    }

    public Optional<MesManagementFeedback> getOwnFeedback(long id, long userId) throws SQLException {
        return feedbackDao.findByIdAndCreator(id, userId);
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
        return feedbackDao.insert(feedback, 1L);
    }

    public boolean closeFeedback(long id) throws SQLException {
        return feedbackDao.close(id);
    }
}
