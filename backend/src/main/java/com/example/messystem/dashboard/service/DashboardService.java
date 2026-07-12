package com.example.messystem.dashboard.service;

import com.example.messystem.common.Db;
import com.example.messystem.dashboard.dao.DashboardMetricDao;
import com.example.messystem.dashboard.entity.MesDashboardMetric;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public List<MesDashboardMetric> aggregateSummary() throws SQLException {
        List<MesDashboardMetric> metrics = new ArrayList<>();
        add(metrics, "orders", "客户订单", count("select count(*) from mes_customer_order"), "SUMMARY");
        add(metrics, "work_orders", "生产工单", count("select count(*) from mes_work_order"), "SUMMARY");
        add(metrics, "reports", "生产报工", count("select count(*) from mes_work_report"), "SUMMARY");
        add(metrics, "inspections", "质检单", count("select count(*) from mes_quality_inspection"), "SUMMARY");
        add(metrics, "fault_equipment", "故障设备", count("select count(*) from mes_equipment where equipment_status = 'FAULT'"), "SUMMARY");
        return metrics;
    }

    public List<MesDashboardMetric> aggregateQuality() throws SQLException {
        List<MesDashboardMetric> metrics = new ArrayList<>();
        add(metrics, "quality_total", "质检总数", count("select count(*) from mes_quality_inspection"), "QUALITY");
        add(metrics, "quality_pass", "合格质检", count("select count(*) from mes_quality_inspection where judgement_result = 'PASS'"), "QUALITY");
        add(metrics, "quality_fail", "不合格质检", count("select count(*) from mes_quality_inspection where judgement_result = 'FAIL'"), "QUALITY");
        add(metrics, "quality_rework", "返工质检", count("select count(*) from mes_quality_inspection where judgement_result = 'REWORK'"), "QUALITY");
        add(metrics, "rework_open", "未关闭返工", count("select count(*) from mes_rework_order where rework_status <> 'FINISHED'"), "QUALITY");
        return metrics;
    }

    public List<MesDashboardMetric> aggregateEquipment() throws SQLException {
        List<MesDashboardMetric> metrics = new ArrayList<>();
        add(metrics, "equipment_total", "设备总数", count("select count(*) from mes_equipment where enabled = 1"), "EQUIPMENT");
        add(metrics, "equipment_running", "运行设备", count("select count(*) from mes_equipment where equipment_status = 'RUNNING'"), "EQUIPMENT");
        add(metrics, "equipment_fault", "故障设备", count("select count(*) from mes_equipment where equipment_status = 'FAULT'"), "EQUIPMENT");
        add(metrics, "repair_pending", "待审核报修", count("select count(*) from mes_equipment_repair_report where repair_status = 'REPORTED'"), "EQUIPMENT");
        add(metrics, "maintenance_pending", "待处理维修", count("select count(*) from mes_maintenance_order where maintenance_status in ('CREATED','ASSIGNED','IN_PROGRESS')"), "EQUIPMENT");
        return metrics;
    }

    public List<MesDashboardMetric> aggregateProduction() throws SQLException {
        List<MesDashboardMetric> metrics = new ArrayList<>();
        add(metrics, "work_order_created", "待派发工单", count("select count(*) from mes_work_order where work_order_status = 'CREATED'"), "PRODUCTION");
        add(metrics, "work_order_running", "执行中工单", count("select count(*) from mes_work_order where work_order_status in ('DISPATCHED','RECEIVED','IN_PROGRESS')"), "PRODUCTION");
        add(metrics, "work_order_completed", "已完成工单", count("select count(*) from mes_work_order where work_order_status = 'COMPLETED'"), "PRODUCTION");
        add(metrics, "report_qty", "累计报工数", count("select coalesce(sum(report_qty),0) from mes_work_report"), "PRODUCTION");
        add(metrics, "defect_qty", "累计不合格数", count("select coalesce(sum(defect_qty),0) from mes_work_report"), "PRODUCTION");
        return metrics;
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

    private void add(List<MesDashboardMetric> metrics, String key, String name, long value, String type) {
        metrics.add(new MesDashboardMetric(null, key, name, String.valueOf(value), type, LocalDateTime.now()));
    }

    private long count(String sql) throws SQLException {
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}
