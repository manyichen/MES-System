import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 生产报工表：存储操作工工单报工、产量和工时数据。 */
public class MesWorkReport {
    /** 报工主键 */
    private Long reportId;

    /** 报工编号 */
    private String reportNo;

    /** 工单 */
    private Long workOrder Id;

    /** 操作工 */
    private Long operatorId;

    /** 报工数量 */
    private Integer reportQty;

    /** 合格数量 */
    private Integer qualifiedQty;

    /** 不合格数量 */
    private Integer defectQty;

    /** 工时 */
    private BigDecimal workHours;

    /** 报工时间 */
    private LocalDateTime reportTime;

    /** 报工状态 */
    private String reportStatus;

}
