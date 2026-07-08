import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 质量检验项目表：存储抽检项目、标准值、实测值和项目判定。 */
public class MesQualityInspectionItem {
    /** 检验项目主键 */
    private Long inspectionItem Id;

    /** 抽检单 */
    private Long inspectionId;

    /** 项目编码 */
    private String itemCode;

    /** 项目名称 */
    private String itemName;

    /** 标准值 */
    private String standardValue;

    /** 实测值 */
    private String actualValue;

    /** 项目判定 */
    private String itemResult;

    /** 备注 */
    private String remark;

}
