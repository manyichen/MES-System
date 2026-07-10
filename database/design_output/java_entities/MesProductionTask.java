import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 生产任务表：存储PMC计划员排产形成的生产任务。 */
public class MesProductionTask {
    /** 任务主键 */
    private Long taskId;

    /** 任务编号 */
    private String taskNo;

    /** 来源订单 */
    private Long orderId;

    /** PMC计划员 */
    private Long plannerId;

    /** 计划数量 */
    private Integer planQty;

    /** 计划开始时间 */
    private LocalDateTime plannedStart Time;

    /** 计划完成时间 */
    private LocalDateTime plannedEnd Time;

    /** 目标产线 */
    private Long targetLine Id;

    /** 任务状态 */
    private String taskStatus;

    /** 齐套状态 */
    private String kittingStatus;

    /** 发布时间 */
    private LocalDateTime releaseTime;

    /** 闭环时间 */
    private LocalDateTime closeTime;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}
