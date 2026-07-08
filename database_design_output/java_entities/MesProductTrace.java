import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 产品追溯表：存储订单、物料、工序、质量的产品追溯主链路。 */
public class MesProductTrace {
    /** 产品追溯主键 */
    private Long productTrace Id;

    /** 追溯码 */
    private String traceCode;

    /** 订单 */
    private Long orderId;

    /** 任务 */
    private Long taskId;

    /** 工单 */
    private Long workOrder Id;

    /** 产品 */
    private Long productId;

    /** 生产批次 */
    private String batchNo;

    /** 追溯状态 */
    private String traceStatus;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
