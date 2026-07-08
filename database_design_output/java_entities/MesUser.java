import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 用户表：存储系统用户基础信息。 */
public class MesUser {
    /** 用户主键 */
    private Long userId;

    /** 登录名 */
    private String username;

    /** 姓名 */
    private String realName;

    /** 角色编码 */
    private String roleCode;

    /** 部门 */
    private String department;

    /** 电话 */
    private String phone;

    /** 是否启用 */
    private Integer enabled;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
