import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 产品BOM表：存储产品用料参数。 */
public class MesProductBom {
    /** BOM主键 */
    private Long bomId;

    /** 产品 */
    private Long productId;

    /** 物料 */
    private Long materialId;

    /** 单位用量 */
    private BigDecimal usageQty;

    /** 单位 */
    private String unit;

    /** 使用工序 */
    private Long processId;

    /** 是否启用 */
    private Integer enabled;

}
