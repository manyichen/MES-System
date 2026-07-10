import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 计件工资表：存储报工对应计件工资核算结果。 */
public class MesPieceworkWage {
    /** 计件工资主键 */
    private Long wageId;

    /** 报工 */
    private Long reportId;

    /** 操作工 */
    private Long operatorId;

    /** 单件费率 */
    private BigDecimal pieceRate;

    /** 合格数量 */
    private Integer qualifiedQty;

    /** 工资金额 */
    private BigDecimal wageAmount;

    /** 结算状态 */
    private String settlementStatus;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
