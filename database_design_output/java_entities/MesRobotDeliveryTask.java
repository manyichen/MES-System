import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 机器人配送任务表：存储物料装载、配送和交接任务。 */
public class MesRobotDeliveryTask {
    /** 配送任务主键 */
    private Long deliveryTask Id;

    /** 配送任务号 */
    private String deliveryTask No;

    /** 拣货任务 */
    private Long pickingTask Id;

    /** 机器人 */
    private Long robotId;

    /** 起点库位 */
    private Long fromLocation Id;

    /** 目标产线 */
    private Long toLine Id;

    /** 配送状态 */
    private String deliveryStatus;

    /** 装载时间 */
    private LocalDateTime loadTime;

    /** 交接时间 */
    private LocalDateTime handoverTime;

}
