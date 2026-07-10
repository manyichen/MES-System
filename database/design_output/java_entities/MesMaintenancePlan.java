import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 设备维护计划表：存储设备周期维护计划。 */
public class MesMaintenancePlan {
    /** 维护计划主键 */
    private Long maintenancePlan Id;

    /** 设备 */
    private Long equipmentId;

    /** 维护周期 */
    private String planCycle;

    /** 下次计划时间 */
    private LocalDateTime nextPlan Time;

    /** 计划状态 */
    private String planStatus;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
