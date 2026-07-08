import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 返工单表：存储质量不合格后的返工任务和追溯信息。 */
public class MesReworkOrder {
    /** 返工单主键 */
    private Long reworkOrder Id;

    /** 返工单号 */
    private String reworkOrder No;

    /** 来源工单 */
    private Long sourceWork Order Id;

    /** 来源质检单 */
    private Long inspectionId;

    /** 返工原因 */
    private String reworkReason;

    /** 返工状态 */
    private String reworkStatus;

    /** 返工产线 */
    private Long assignedLine Id;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 关闭时间 */
    private LocalDateTime closedAt;

}
