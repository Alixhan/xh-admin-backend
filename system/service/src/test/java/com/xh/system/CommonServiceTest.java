package com.xh.system;

import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSONObject;
import com.xh.common.core.service.CommonService;
import com.xh.system.service.SysLoginService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author sunxh
 * @since 2025/5/28
 */
@Slf4j
@SpringBootTest
public class CommonServiceTest {
    @Resource
    private CommonService commonService;
    @Resource
    private SysLoginService sysLoginService;

    @Test
    public void testGetPermissionSql() {
        SaTokenContextMockUtil.setMockContext(() -> {
            StpUtil.setTokenValueToStorage("test");
            // 先模拟登录
            JSONObject params = new JSONObject();
            params.put("username", "admin");
            params.put("password", "admin123");
            params.put("isDemo", "1");
            sysLoginService.login(params);

            String permissionSql = commonService.getPermissionSql("sys_log", "create_by", "sys_role_id", "sys_org_id");
            log.error("权限sql: {}", permissionSql);
        });
    }
}
