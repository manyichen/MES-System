import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 仓库表：存储仓库基础信息。 */
public class MesWarehouse {
    /** 仓库主键 */
    private Long warehouseId;

    /** 仓库编码 */
    private String warehouseCode;

    /** 仓库名称 */
    private String warehouseName;

    /** 仓库类型 */
    private String warehouseType;

    /** 是否启用 */
    private Integer enabled;

}
