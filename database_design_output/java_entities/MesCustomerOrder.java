import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 客户订单表：存储轮胎客户订单主数据，作为生产任务来源。 */
public class MesCustomerOrder {
    /** 订单主键 */
    private Long orderId;

    /** 订单编号 */
    private String orderNo;

    /** 客户名称 */
    private String customerName;

    /** 产品主键 */
    private Long productId;

    /** 产品编码快照 */
    private String productCode;

    /** 轮胎型号/规格 */
    private String productModel;

    /** 订单数量 */
    private Integer orderQty;

    /** 计量单位 */
    private String unit;

    /** 交付日期 */
    private LocalDate deliveryDate;

    /** 优先级，1最高 */
    private Integer priorityLevel;

    /** 订单状态 */
    private String orderStatus;

    /** 来源系统 */
    private String sourceSystem;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}
