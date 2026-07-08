import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 库存表：存储物料库存数量、批次、库位和质量状态。 */
public class MesInventory {
    /** 库存主键 */
    private Long inventoryId;

    /** 物料 */
    private Long materialId;

    /** 仓库 */
    private Long warehouseId;

    /** 库位 */
    private Long locationId;

    /** 批次号 */
    private String batchNo;

    /** 可用数量 */
    private BigDecimal availableQty;

    /** 占用数量 */
    private BigDecimal reservedQty;

    /** 冻结数量 */
    private BigDecimal frozenQty;

    /** 质量状态 */
    private String qualityStatus;

    /** 最近核对时间 */
    private LocalDateTime lastCheck Time;

}
