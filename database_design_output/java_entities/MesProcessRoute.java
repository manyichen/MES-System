import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 工艺路线表：存储轮胎产品生产工序和工艺顺序。 */
public class MesProcessRoute {
    /** 工序主键 */
    private Long processId;

    /** 产品 */
    private Long productId;

    /** 工序编码 */
    private String processCode;

    /** 工序名称 */
    private String processName;

    /** 工序顺序 */
    private Integer processSeq;

    /** 标准工时 */
    private BigDecimal standardHours;

    /** 所需设备类型 */
    private String requiredEquipment Type;

    /** 是否启用 */
    private Integer enabled;

}
