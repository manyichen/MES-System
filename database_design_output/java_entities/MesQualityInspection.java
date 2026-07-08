import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 质量抽检单表：存储抽检工单、检验对象和检验结果。 */
public class MesQualityInspection {
    /** 抽检单主键 */
    private Long inspectionId;

    /** 抽检单号 */
    private String inspectionNo;

    /** 工单 */
    private Long workOrder Id;

    /** 抽检数量 */
    private Integer sampleQty;

    /** 抽检状态 */
    private String inspectionStatus;

    /** 质检员 */
    private Long inspectorId;

    /** 检验时间 */
    private LocalDateTime inspectionTime;

    /** 判定结果 */
    private String judgementResult;

}
