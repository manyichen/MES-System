import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 管理决策反馈表：存储异常处理、计划调整和管理反馈记录。 */
public class MesManagementFeedback {
    /** 反馈主键 */
    private Long feedbackId;

    /** 反馈编号 */
    private String feedbackNo;

    /** 反馈类型 */
    private String feedbackType;

    /** 关联单据类型 */
    private String relatedDoc Type;

    /** 关联单据ID */
    private Long relatedDoc Id;

    /** 反馈内容 */
    private String feedbackContent;

    /** 决策动作 */
    private String decisionAction;

    /** 处理状态 */
    private String feedbackStatus;

    /** 创建人 */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
