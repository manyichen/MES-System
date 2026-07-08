import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 数据同步日志表：记录ERP/WMS/设备等外部系统同步结果。 */
public class MesSyncLog {
    /** 同步日志主键 */
    private Long syncLog Id;

    /** 来源系统 */
    private String sourceSystem;

    /** 同步对象 */
    private String syncObject;

    /** 业务键 */
    private String businessKey;

    /** 同步状态 */
    private String syncStatus;

    /** 错误信息 */
    private String errorMessage;

    /** 同步时间 */
    private LocalDateTime syncTime;

}
