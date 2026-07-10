import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 领料任务表：存储生产工单对应的物料申请任务。 */
public class MesMaterialRequisition {
    /** 领料任务主键 */
    private Long requisitionId;

    /** 领料单号 */
    private String requisitionNo;

    /** 生产工单 */
    private Long workOrder Id;

    /** 申请人 */
    private Long requestedBy;

    /** 领料状态 */
    private String requestStatus;

    /** 申请时间 */
    private LocalDateTime requestTime;

    /** 审批人 */
    private Long approvedBy;

    /** 审批时间 */
    private LocalDateTime approvedTime;

    /** 备注 */
    private String remark;

}
