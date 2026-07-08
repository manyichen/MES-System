import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 领料任务明细表：存储领料任务所需物料明细。 */
public class MesMaterialRequisitionItem {
    /** 明细主键 */
    private Long requisitionItem Id;

    /** 领料任务 */
    private Long requisitionId;

    /** 物料 */
    private Long materialId;

    /** 需求数量 */
    private BigDecimal requiredQty;

    /** 已发数量 */
    private BigDecimal issuedQty;

    /** 单位 */
    private String unit;

    /** 指定批次 */
    private String batchNo;

    /** 明细状态 */
    private String itemStatus;

}
