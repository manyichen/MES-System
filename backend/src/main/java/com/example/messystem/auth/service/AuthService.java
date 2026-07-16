package com.example.messystem.auth.service;

import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.auth.LoginRequest;
import com.example.messystem.auth.LoginSession;
import com.example.messystem.auth.dao.AuthDao;
import com.example.messystem.common.BadRequestException;
import java.util.Set;

/** 认证业务服务，是过滤器和控制器访问认证流程的唯一入口。 */
public class AuthService {
    private static final Set<String> RETIRED_ACCOUNTS = Set.of("mes_sysmaint", "mes_viewer");
    private final AuthDao dao = new AuthDao();

    /** 校验登录参数后执行原子登录事务。 */
    public LoginSession login(LoginRequest request, String loginIp, String userAgent) {
        if (request == null || request.username == null || request.username.isBlank()
                || request.password == null || request.password.isBlank()) {
            throw new BadRequestException("账号和密码不能为空");
        }
        if (RETIRED_ACCOUNTS.contains(request.username.trim())) {
            throw new BadRequestException("账号或密码错误");
        }
        return dao.login(request, loginIp, userAgent);
    }

    /** 将 Bearer 令牌解析为有效用户、角色、权限和数据范围。 */
    public AuthenticatedUser authenticate(String token) {
        return token == null || token.isBlank() ? null : dao.authenticate(token);
    }

    /** 撤销当前令牌并记录退出登录审计事件。 */
    public void logout(String token, AuthenticatedUser currentUser) {
        dao.logout(token, currentUser);
    }
}
