import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 生产产线表：存储生产线基础信息、产能和状态。 */
public class MesProductionLine {
    /** 产线主键 */
    private Long lineId;

    /** 产线编码 */
    private String lineCode;

    /** 产线名称 */
    private String lineName;

    /** 产线类型 */
    private String lineType;

    /** 日产能 */
    private Integer dailyCapacity;

    /** 产线状态 */
    private String lineStatus;

    /** 是否启用 */
    private Integer enabled;

}
