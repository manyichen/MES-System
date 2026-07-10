package com.example.messystem.dashboard.service;

import com.example.messystem.dashboard.dao.DashboardMetricDao;
import com.example.messystem.dashboard.entity.MesDashboardMetric;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class DashboardService {

    private final DashboardMetricDao metricDao = new DashboardMetricDao();

    public long createMetric(MesDashboardMetric metric) throws SQLException {
        return metricDao.insert(metric);
    }

    public Optional<MesDashboardMetric> getMetric(long metricId) throws SQLException {
        return metricDao.findById(metricId);
    }

    public List<MesDashboardMetric> listMetrics() throws SQLException {
        return metricDao.findAll();
    }

    public long createDefaultMetric(String metricKey, String metricName, String metricValue, String metricType) throws SQLException {
        MesDashboardMetric metric = new MesDashboardMetric(
                null,
                metricKey,
                metricName,
                metricValue,
                metricType,
                LocalDateTime.now()
        );
        return metricDao.insert(metric);
    }
}
