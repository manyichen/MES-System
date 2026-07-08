import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 生产工单表：存储由生产任务拆分生成的车间执行工单。 */
public class MesWorkOrder {
    /** 工单主键 */
    private Long workOrder Id;

    /** 工单编号 */
    private String workOrder No;

    /** 生产任务 */
    private Long taskId;

    /** 产线 */
    private Long lineId;

    /** 工序 */
    private Long processId;

    /** 计划数量 */
    private Integer plannedQty;

    /** 实际数量 */
    private Integer actualQty;

    /** 优先级 */
    private Integer priorityLevel;

    /** 工单状态 */
    private String workOrder Status;

    /** 派发时间 */
    private LocalDateTime dispatchTime;

    /** 接收时间 */
    private LocalDateTime receiveTime;

    /** 完成时间 */
    private LocalDateTime completedTime;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}
