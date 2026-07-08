import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 齐套缺口明细表：存储齐套分析发现的物料、产线、设备、工序缺口。 */
public class MesKittingShortageItem {
    /** 缺口明细主键 */
    private Long shortageItem Id;

    /** 齐套分析 */
    private Long analysisId;

    /** 生产任务 */
    private Long taskId;

    /** 缺口类型 MATERIAL/LINE/EQUIPMENT/PROCESS */
    private String shortageType;

    /** 资源主键 */
    private Long resourceId;

    /** 资源编码 */
    private String resourceCode;

    /** 资源名称 */
    private String resourceName;

    /** 需求数量 */
    private BigDecimal requiredQty;

    /** 可用数量 */
    private BigDecimal availableQty;

    /** 缺口数量 */
    private BigDecimal shortageQty;

    /** 影响说明 */
    private String impactDesc;

    /** 处理建议 */
    private String suggestion;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
