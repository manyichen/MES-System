import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 库位表：存储仓库库位信息。 */
public class MesWarehouseLocation {
    /** 库位主键 */
    private Long locationId;

    /** 仓库 */
    private Long warehouseId;

    /** 库位编码 */
    private String locationCode;

    /** 库位名称 */
    private String locationName;

    /** 是否启用 */
    private Integer enabled;

}
