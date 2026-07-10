import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 产品主数据表：存储轮胎产品型号、规格和基础信息。 */
public class MesProduct {
    /** 产品主键 */
    private Long productId;

    /** 产品编码 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 产品型号 */
    private String productModel;

    /** 规格 */
    private String specification;

    /** 是否启用 */
    private Integer enabled;

}
