package com.xh.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import com.xh.common.core.web.RestResponse;
import com.xh.system.client.dto.SysRolePermissionDTO;
import com.xh.system.client.entity.SysMenu;
import com.xh.system.client.entity.SysRole;
import com.xh.system.client.entity.SysRoleDataPermission;
import com.xh.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Tag(name = "系统角色")
@RestController
@RequestMapping("/api/system/role")
@SaCheckPermission("system:role")
public class SysRoleController {

    @Resource
    private SysRoleService sysRoleService;

    @Operation(description = "角色列表查询")
    @PostMapping("/query")
    public RestResponse<PageResult<SysRole>> query(@RequestBody PageQuery<Map<String, Object>> pageQuery) {
        PageResult<SysRole> data = sysRoleService.query(pageQuery);
        return RestResponse.success(data);
    }

    @SaCheckPermission(value = {"system:role:add", "system:role:edit"}, mode = SaMode.OR)
    @Operation(description = "角色保存")
    @PostMapping("/save")
    public RestResponse<SysRole> save(@RequestBody SysRole sysRole) {
        return RestResponse.success(sysRoleService.save(sysRole));
    }

    @SaCheckPermission(value = {"system:role:edit", "system:role:detail"}, mode = SaMode.OR)
    @Operation(description = "获取角色详情")
    @GetMapping("/get/{id}")
    public RestResponse<SysRole> getById(@PathVariable Serializable id) {
        return RestResponse.success(sysRoleService.getById(id));
    }

    @SaCheckPermission("system:role:del")
    @Operation(description = "角色批量删除")
    @DeleteMapping("/del")
    public RestResponse<?> del(@RequestParam List<Serializable> ids) {
        sysRoleService.del(ids);
        return RestResponse.success();
    }

    @Operation(description = "查询角色可配置的所有菜单权限")
    @GetMapping("/queryRoleMenu")
    public RestResponse<List<SysMenu>> queryRoleMenu(@RequestParam Map<String, Object> param) {
        List<SysMenu> roleMenus = sysRoleService.queryRoleMenu(param);
        return RestResponse.success(roleMenus);
    }

    @Operation(description = "查询角色的数据权限")
    @GetMapping("/queryRoleDataPermission")
    public RestResponse<List<SysRoleDataPermission>> queryRoleDataPermission(@RequestParam Map<String, Object> param) {
        List<SysRoleDataPermission> roleDataPermission = sysRoleService.queryRoleDataPermission(param);
        return RestResponse.success(roleDataPermission);
    }

    @SaCheckPermission("system:role:dataPermission")
    @Operation(description = "保存角色数据权限")
    @PostMapping("/saveRoleDataPermission")
    public RestResponse<?> saveRoleDataPermission(@RequestBody SysRolePermissionDTO sysRolePermissionDTO) {
        sysRoleService.saveRoleDataPermission(sysRolePermissionDTO);
        return RestResponse.success();
    }
}
