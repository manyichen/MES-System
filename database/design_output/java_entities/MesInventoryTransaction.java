import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 库存流水表：记录物料入库、出库、冻结、释放、盘点调整等库存变动。 */
public class MesInventoryTransaction {
    /** 流水主键 */
    private Long transactionId;

    /** 流水编号 */
    private String transactionNo;

    /** 物料 */
    private Long materialId;

    /** 库存 */
    private Long inventoryId;

    /** 变动类型 */
    private String transactionType;

    /** 变动数量 */
    private BigDecimal qty;

    /** 来源单据类型 */
    private String sourceDoc Type;

    /** 来源单据ID */
    private Long sourceDoc Id;

    /** 操作人 */
    private Long operatorId;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
