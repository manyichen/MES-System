import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 运输机器人表：存储AGV/运输机器人基础信息。 */
public class MesRobot {
    /** 机器人主键 */
    private Long robotId;

    /** 机器人编码 */
    private String robotCode;

    /** 机器人名称 */
    private String robotName;

    /** 机器人状态 */
    private String robotStatus;

    /** 电量百分比 */
    private BigDecimal batteryLevel;

    /** 当前位置 */
    private String currentLocation;

    /** 是否启用 */
    private Integer enabled;

}
