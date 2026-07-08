import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 设备故障报修表：存储设备故障上报和报修单信息。 */
public class MesEquipmentRepairReport {
    /** 报修主键 */
    private Long repairReport Id;

    /** 报修单号 */
    private String repairReport No;

    /** 设备 */
    private Long equipmentId;

    /** 关联工单 */
    private Long workOrder Id;

    /** 故障等级 */
    private String faultLevel;

    /** 故障描述 */
    private String faultDesc;

    /** 报修人 */
    private Long reporterId;

    /** 报修时间 */
    private LocalDateTime reportTime;

    /** 报修状态 */
    private String repairStatus;

}
