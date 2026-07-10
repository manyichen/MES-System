import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 欠料预警表：存储欠料及资源缺口预警处理记录。 */
public class MesShortageAlert {
    /** 预警主键 */
    private Long alertId;

    /** 预警编号 */
    private String alertNo;

    /** 生产任务 */
    private Long taskId;

    /** 齐套分析 */
    private Long analysisId;

    /** 预警类型 */
    private String alertType;

    /** 严重级别 */
    private String severity;

    /** 处理状态 */
    private String alertStatus;

    /** 接收角色 */
    private String receiverRole;

    /** 预警内容 */
    private String alertContent;

    /** 解决时间 */
    private LocalDateTime resolvedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
