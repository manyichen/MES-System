import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 设备表：存储生产设备基础信息和运行状态。 */
public class MesEquipment {
    /** 设备主键 */
    private Long equipmentId;

    /** 设备编码 */
    private String equipmentCode;

    /** 设备名称 */
    private String equipmentName;

    /** 设备类型 */
    private String equipmentType;

    /** 所属产线 */
    private Long lineId;

    /** 设备状态 */
    private String equipmentStatus;

    /** 最近维护时间 */
    private LocalDateTime lastMaintenance Time;

    /** 是否启用 */
    private Integer enabled;

}
