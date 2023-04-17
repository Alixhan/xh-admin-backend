package com.xh.system.controller;

import com.xh.common.core.web.RestResponse;
import com.xh.system.client.dto.SysLoginUserInfoDto;
import com.xh.system.client.service.SysUserLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "系统登录控制器")
@RestController
@RequestMapping("/api/system/login")
public class SysLoginController {

    @Resource
    private SysUserLoginService sysUserLoginService;

    @Operation(description = "web管理系统登录")
    @PostMapping("/login")
    public RestResponse<SysLoginUserInfoDto> login(@RequestBody Map<String, Object> params) {
        return sysUserLoginService.login(params);
    }

    @Operation(description = "web管理系统注销")
    @PostMapping("/logout")
    public RestResponse<?> login() {
        return sysUserLoginService.logout();
    }
}