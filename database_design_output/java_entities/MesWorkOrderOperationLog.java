import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 工单调度操作日志表：记录工单下发、暂停、优先级调整、撤销等调度操作。 */
public class MesWorkOrderOperationLog {
    /** 日志主键 */
    private Long operationLog Id;

    /** 工单 */
    private Long workOrder Id;

    /** 操作类型 */
    private String operationType;

    /** 操作前状态 */
    private String beforeStatus;

    /** 操作后状态 */
    private String afterStatus;

    /** 操作人 */
    private Long operatorId;

    /** 操作原因 */
    private String operationReason;

    /** 操作时间 */
    private LocalDateTime operationTime;

    /** 是否可撤销 */
    private Integer undoable;

}
