import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 齐套分析表：存储任务发布前的物料、产线、设备、工序齐套分析记录。 */
public class MesKittingAnalysis {
    /** 分析主键 */
    private Long analysisId;

    /** 分析编号 */
    private String analysisNo;

    /** 生产任务 */
    private Long taskId;

    /** 分析范围 */
    private String analysisScope;

    /** 分析结果 */
    private String resultStatus;

    /** 数据快照时间 */
    private LocalDateTime snapshotTime;

    /** 物料是否齐套 */
    private Integer materialOk;

    /** 产线是否可用 */
    private Integer lineOk;

    /** 设备是否可用 */
    private Integer equipmentOk;

    /** 工艺是否完整 */
    private Integer processOk;

    /** 分析人 */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
