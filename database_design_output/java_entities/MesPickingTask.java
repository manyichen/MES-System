import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 备货拣货任务表：存储仓库根据领料任务生成的备货与拣货任务。 */
public class MesPickingTask {
    /** 拣货任务主键 */
    private Long pickingTask Id;

    /** 拣货任务号 */
    private String pickingTask No;

    /** 领料任务 */
    private Long requisitionId;

    /** 仓库 */
    private Long warehouseId;

    /** 任务状态 */
    private String taskStatus;

    /** 拣货人 */
    private Long assignedTo;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

}
