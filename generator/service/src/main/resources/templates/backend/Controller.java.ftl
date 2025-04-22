package ${controllerPackage};

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import com.xh.common.core.web.RestResponse;
import ${entityPackage}.${entityName};
import ${dtoPackage}.${dtoName};
import ${servicePackage}.${serviceName};
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ${name} Controller
 *
 * @author ${author}
 * @since ${date}
 */
@Tag(name = "${name}")
@RestController
@RequestMapping("${mappingPath}")
public class ${entityName}Controller {

    @Resource
    private ${serviceName} ${serviceVarName};

    @SaCheckPermission("${permissionPrefix}")
    @Operation(description = "${name}查询")
    @PostMapping("/query")
    public RestResponse<PageResult<${entityName}>> query(@RequestBody PageQuery<Map<String, Object>> pageQuery) {
        PageResult<${entityName}> data = ${serviceVarName}.query(pageQuery);
        return RestResponse.success(data);
    }

    @SaCheckPermission("${permissionPrefix}:add")
    @Operation(description = "${name}新增")
    @PostMapping("/insert")
    public RestResponse<${entityName}> insert(@RequestBody ${dtoName} ${dtoVarName}) {
        return RestResponse.success(${serviceVarName}.insert(${dtoVarName}));
    }

    @SaCheckPermission("${permissionPrefix}:edit")
    @Operation(description = "${name}修改")
    @PutMapping("/update")
    public RestResponse<${entityName}> update(@RequestBody ${dtoName} ${dtoVarName}) {
        return RestResponse.success(${serviceVarName}.update(${dtoVarName}));
    }

    @SaCheckPermission(value = {"${permissionPrefix}:edit", "${permissionPrefix}:detail"}, mode = SaMode.OR)
    @Operation(description = "${name}详情")
    @GetMapping("/get/{id}")
    public RestResponse<${entityName}> getById(@PathVariable ${idJavaType} id) {
        return RestResponse.success(${serviceVarName}.getById(id));
    }

    @SaCheckPermission("${permissionPrefix}:del")
    @Operation(description = "${name}删除")
    @DeleteMapping("/del")
    public RestResponse<?> del(@RequestParam List<${idJavaType}> ids) {
        ${serviceVarName}.del(ids);
        return RestResponse.success();
    }
<#if hasImport>

    @SaCheckPermission("${permissionPrefix}:import")
    @Operation(description = "${name}导入")
    @PostMapping("/imports")
    public RestResponse<?> imports(@RequestBody List<${dtoName}> data) {
        ${serviceVarName}.imports(data);
        return RestResponse.success();
    }
</#if>
}
