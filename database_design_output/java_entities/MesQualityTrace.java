import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 质量追溯表：存储订单、任务、工单、质检、返工之间的追溯关系。 */
public class MesQualityTrace {
    /** 追溯主键 */
    private Long traceId;

    /** 追溯编号 */
    private String traceNo;

    /** 订单 */
    private Long orderId;

    /** 任务 */
    private Long taskId;

    /** 工单 */
    private Long workOrder Id;

    /** 批次号 */
    private String batchNo;

    /** 质检单 */
    private Long inspectionId;

    /** 返工单 */
    private Long reworkOrder Id;

    /** 追溯状态 */
    private String traceStatus;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
