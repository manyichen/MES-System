import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 维修工单表：存储设备维修派发、处理和结果反馈。 */
public class MesMaintenanceOrder {
    /** 维修工单主键 */
    private Long maintenanceOrder Id;

    /** 维修工单号 */
    private String maintenanceOrder No;

    /** 报修单 */
    private Long repairReport Id;

    /** 设备 */
    private Long equipmentId;

    /** 维修人 */
    private Long maintainerId;

    /** 维修状态 */
    private String maintenanceStatus;

    /** 派发时间 */
    private LocalDateTime dispatchTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 维修结果 */
    private String resultDesc;

}
