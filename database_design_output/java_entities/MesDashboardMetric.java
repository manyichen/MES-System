import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 综合生产看板指标表：存储产量、订单、质量、设备等看板聚合指标。 */
public class MesDashboardMetric {
    /** 指标主键 */
    private Long metricId;

    /** 指标日期 */
    private LocalDate metricDate;

    /** 指标类型 */
    private String metricType;

    /** 指标名称 */
    private String metricName;

    /** 指标值 */
    private BigDecimal metricValue;

    /** 单位 */
    private String metricUnit;

    /** 维度键 */
    private String dimensionKey;

    /** 生成时间 */
    private LocalDateTime generatedAt;

}
