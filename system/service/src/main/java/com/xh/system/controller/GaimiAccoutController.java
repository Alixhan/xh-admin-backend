package com.xh.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import com.xh.common.core.web.RestResponse;
import com.xh.system.client.entity.GaimiAccount;
import com.xh.system.service.GaimiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "改密")
@RestController
@RequestMapping("/api/gaimi/account")
public class GaimiAccoutController {

    @Resource
    private GaimiService gaimiService;

    @Operation(description = "改密列表查询")
    @PostMapping("/query")
    @SaCheckPermission("gaimi:account")
    public RestResponse<PageResult<GaimiAccount>> query(@RequestBody PageQuery<Map<String, Object>> pageQuery) {
        PageResult<GaimiAccount> data = gaimiService.query(pageQuery);
        return RestResponse.success(data);
    }

    @SaCheckPermission(value = {"gaimi:account:add", "gaimi:account:edit"}, mode = SaMode.OR)
    @Operation(description = "切换改密字段值")
    @PostMapping("/switch_prop")
    public RestResponse<PageResult<GaimiAccount>> switchMenuProp(@RequestBody Map<String, Object> param) {
        gaimiService.switchProp(param);
        return RestResponse.success();
    }

    @SaCheckPermission(value = {"gaimi:account:add", "gaimi:account:edit"}, mode = SaMode.OR)
    @Operation(description = "改密保存")
    @PostMapping("/save")
    public RestResponse<GaimiAccount> save(@RequestBody GaimiAccount gaimiAccount) {
        return RestResponse.success(gaimiService.save(gaimiAccount));
    }

    @SaCheckPermission(value = {"gaimi:account:edit", "gaimi:account:detail"}, mode = SaMode.OR)
    @Operation(description = "获取改密详情")
    @GetMapping("/get/{id}")
    public RestResponse<GaimiAccount> getById(@PathVariable Serializable id) {
        return RestResponse.success(gaimiService.getById(id));
    }

    @SaCheckPermission("gaimi:account:del")
    @Operation(description = "改密批量删除")
    @DeleteMapping("/del")
    public RestResponse<?> del(@RequestParam List<Serializable> ids) {
        gaimiService.del(ids);
        return RestResponse.success();
    }

    @SaCheckPermission("gaimi:account:import")
    @Operation(description = "改密批量导入")
    @PostMapping("/imports")
    public RestResponse<ArrayList<Map<String, Object>>> imports(@RequestBody List<GaimiAccount> gaimiAccounts) {
        return RestResponse.success(gaimiService.imports(gaimiAccounts));
    }
}
