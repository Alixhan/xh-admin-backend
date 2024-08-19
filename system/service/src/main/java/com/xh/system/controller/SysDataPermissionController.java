package com.xh.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import com.xh.common.core.web.RestResponse;
import com.xh.system.client.entity.SysDataEntity;
import com.xh.system.client.entity.SysDataPermission;
import com.xh.system.service.SysDataPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Tag(name = "系统数据权限")
@RestController
@RequestMapping("/api/system/dataPermission")
@SaCheckPermission("system:dataPermission")
public class SysDataPermissionController {

    @Resource
    private SysDataPermissionService sysDataPermissionService;

    @Operation(description = "数据实体列表查询")
    @PostMapping("/entity/query")
    public RestResponse<List<SysDataEntity>> queryEntity() {
        List<SysDataEntity> data = sysDataPermissionService.queryEntity();
        return RestResponse.success(data);
    }

    @Operation(description = "数据权限列表查询")
    @PostMapping("/query")
    public RestResponse<PageResult<SysDataPermission>> query(@RequestBody PageQuery<Map<String, Object>> pageQuery) {
        PageResult<SysDataPermission> data = sysDataPermissionService.query(pageQuery);
        return RestResponse.success(data);
    }

    @SaCheckPermission(value = {"system:dataPermission:add", "system:dataPermission:edit"}, mode = SaMode.OR)
    @Operation(description = "数据权限保存")
    @PostMapping("/save")
    public RestResponse<SysDataPermission> save(@RequestBody SysDataPermission sysDataPermission) {
        return RestResponse.success(sysDataPermissionService.save(sysDataPermission));
    }

    @SaCheckPermission(value = {"system:dataPermission:edit", "system:dataPermission:detail"}, mode = SaMode.OR)
    @Operation(description = "获取数据权限详情")
    @GetMapping("/get/{id}")
    public RestResponse<SysDataPermission> getById(@PathVariable Serializable id) {
        return RestResponse.success(sysDataPermissionService.getById(id));
    }

    @SaCheckPermission("system:dataPermission:del")
    @Operation(description = "数据权限批量删除")
    @DeleteMapping("/del")
    public RestResponse<?> del(@RequestParam List<Serializable> ids) {
        sysDataPermissionService.del(ids);
        return RestResponse.success();
    }
}
