import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 物料主数据表：存储轮胎生产所需原材料、半成品、辅料等物料主数据。 */
public class MesMaterial {
    /** 物料主键 */
    private Long materialId;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 物料类型 */
    private String materialType;

    /** 规格 */
    private String specification;

    /** 单位 */
    private String unit;

    /** 保质期天数 */
    private Integer shelfLife Days;

    /** 是否启用 */
    private Integer enabled;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
