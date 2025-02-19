package com.xh.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.xh.common.core.dto.OnlineUserDTO;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import com.xh.common.core.web.RestResponse;
import com.xh.system.client.dto.ImageCaptchaDTO;
import com.xh.system.client.dto.SysUserJobDTO;
import com.xh.system.client.entity.SysUser;
import com.xh.system.client.entity.SysUserGroup;
import com.xh.system.client.entity.SysUserJob;
import com.xh.system.client.vo.LoginUserInfoVO;
import com.xh.system.service.SysLoginService;
import com.xh.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/system/user")
public class SysUserController {

    @Resource
    private SysLoginService sysLoginService;
    @Resource
    private SysUserService sysUserService;

    @SaIgnore
    @Operation(description = "获取图形验证码")
    @GetMapping("/captcha")
    public RestResponse<ImageCaptchaDTO> getImageCaptcha(String captchaKey) {
        return RestResponse.success(sysLoginService.getImageCaptcha(captchaKey));
    }

    @SaIgnore
    @Operation(description = "登录")
    @PostMapping("/login")
    public RestResponse<LoginUserInfoVO> login(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        return RestResponse.success(sysLoginService.login(request, params));
    }

    @Operation(description = "角色切换")
    @PostMapping("/switchUserRole")
    public RestResponse<LoginUserInfoVO> switchUserRole(@RequestBody Map<String, Object> params) {
        return RestResponse.success(sysLoginService.switchUserRole(params));
    }

    @Operation(description = "切换语言")
    @PostMapping("/switchLocale")
    public RestResponse<?> switchLocale(@RequestBody Map<String, Object> params) {
        sysLoginService.switchLocale(params);
        return RestResponse.success();
    }

    @Operation(description = "注销")
    @PostMapping("/logout")
    public RestResponse<?> login() {
        return sysLoginService.logout();
    }

    @Operation(description = "保存个人中心信息")
    @PostMapping("/personalCenterSave")
    public RestResponse<SysUser> personalCenterSave(@RequestBody SysUser sysUser) {
        return RestResponse.success(sysUserService.personalCenterSave(sysUser));
    }

    @Operation(description = "获取个人中心详情")
    @GetMapping("/getPersonalCenterDetail")
    public RestResponse<SysUser> getPersonalCenterDetail() {
        return RestResponse.success(sysUserService.getById((Serializable) StpUtil.getLoginId()));
    }

    @Operation(description = "用户列表查询")
    @PostMapping("/query")
    @SaCheckPermission("system:user")
    public RestResponse<PageResult<SysUser>> query(@RequestBody PageQuery<Map<String, Object>> pageQuery) {
        PageResult<SysUser> data = sysUserService.query(pageQuery);
        return RestResponse.success(data);
    }

    @SaCheckPermission(value = {"system:user:add", "system:user:edit"}, mode = SaMode.OR)
    @Operation(description = "切换用户字段值")
    @PostMapping("/switch_prop")
    public RestResponse<PageResult<SysUser>> switchMenuProp(@RequestBody Map<String, Object> param) {
        sysUserService.switchMenuProp(param);
        return RestResponse.success();
    }

    @SaCheckPermission(value = {"system:user:add", "system:user:edit"}, mode = SaMode.OR)
    @Operation(description = "用户保存")
    @PostMapping("/save")
    public RestResponse<SysUser> save(@RequestBody SysUser sysUser) {
        return RestResponse.success(sysUserService.save(sysUser));
    }

    @SaCheckPermission(value = {"system:user:edit", "system:user:detail"}, mode = SaMode.OR)
    @Operation(description = "获取用户详情")
    @GetMapping("/get/{id}")
    public RestResponse<SysUser> getById(@PathVariable Integer id) {
        return RestResponse.success(sysUserService.getById(id));
    }

    @SaCheckPermission("system:user:del")
    @Operation(description = "用户批量删除")
    @DeleteMapping("/del")
    public RestResponse<?> del(@RequestParam List<Integer> ids) {
        sysUserService.del(ids);
        return RestResponse.success();
    }

    @SaCheckPermission("system:user:import")
    @Operation(description = "用户批量导入")
    @PostMapping("/imports")
    public RestResponse<ArrayList<Map<String, Object>>> imports(@RequestBody List<SysUser> sysUsers) {
        return RestResponse.success(sysUserService.imports(sysUsers));
    }

    @SaCheckPermission("system:user:resetPassword")
    @Operation(description = "用户账号密码重置")
    @PostMapping("/resetPassword")
    public RestResponse<ArrayList<Map<String, Object>>> resetPassword(@RequestBody SysUser sysUsers) {
        sysUserService.resetPassword(sysUsers);
        return RestResponse.success();
    }

    @SaCheckPermission("system:userGroup")
    @Operation(description = "系统用户组查询")
    @PostMapping("/queryUserGroupList")
    public RestResponse<PageResult<SysUserGroup>> queryUserGroupList(@RequestBody PageQuery<Map<String, Object>> pageQuery) {
        PageResult<SysUserGroup> data = sysUserService.queryUserGroupList(pageQuery);
        return RestResponse.success(data);
    }

    @SaCheckPermission(value = {"system:userGroup:add", "system:userGroup:edit"}, mode = SaMode.OR)
    @Operation(description = "用户组保存")
    @PostMapping("/saveUserGroup")
    public RestResponse<SysUserGroup> saveUserGroup(@RequestBody SysUserGroup sysUserGroup) {
        return RestResponse.success(sysUserService.saveUserGroup(sysUserGroup));
    }

    @SaCheckPermission(value = {"system:userGroup:edit", "system:userGroup:detail"}, mode = SaMode.OR)
    @Operation(description = "id获取用户组详情")
    @GetMapping("/getUserGroup/{id}")
    public RestResponse<SysUserGroup> getUserGroupById(@PathVariable Integer id) {
        return RestResponse.success(sysUserService.getUserGroupById(id));
    }

    @SaCheckPermission("system:userGroup:del")
    @Operation(description = "ids批量删除用户组")
    @DeleteMapping("/delUserGroup")
    public RestResponse<?> delUserGroup(@RequestParam List<Integer> ids) {
        sysUserService.delUserGroup(ids);
        return RestResponse.success();
    }

    @SaCheckPermission(value = {"system:user", "system:userGroup"}, mode = SaMode.OR)
    @Operation(description = "获取用户或者用户组的岗位信息")
    @GetMapping("/getUserJobs")
    public RestResponse<List<SysUserJob>> getUserJobs(SysUserJob param) {
        return RestResponse.success(sysUserService.getUserJobs(param));
    }

    @SaCheckPermission(value = {"system:user", "system:userGroup"}, mode = SaMode.OR)
    @Operation(description = "用户岗位保存")
    @PostMapping("/saveUserJobs")
    public RestResponse<?> saveUserJobs(@RequestBody SysUserJobDTO sysUserJobDTO) {
        sysUserService.saveUserJobs(sysUserJobDTO);
        return RestResponse.success();
    }

    @SaCheckPermission(value = {"system:user:detail"}, mode = SaMode.OR)
    @Operation(description = "id获取用户所在的所有用户组信息")
    @GetMapping("/getUserGroups/{id}")
    public RestResponse<List<SysUserGroup>> getUserGroups(@PathVariable Integer id) {
        return RestResponse.success(sysUserService.getUserGroups(id));
    }

    @SaCheckPermission("monitor:online")
    @Operation(description = "在线用户查询")
    @PostMapping("/queryOnlineUser")
    public RestResponse<PageResult<OnlineUserDTO>> queryOnlineUser(@RequestBody PageQuery<Map<String, Object>> pageQuery) {
        PageResult<OnlineUserDTO> data = sysLoginService.queryOnlineUser(pageQuery);
        return RestResponse.success(data);
    }

    @SaCheckPermission("monitor:online")
    @Operation(description = "踢用户下线")
    @PostMapping("/kickOut")
    public RestResponse<?> kickOut(@RequestBody String token) {
        sysLoginService.kickOut(token);
        return RestResponse.success();
    }
}
