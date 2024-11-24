package com.xh.common.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据权限基础DTO
 * @author sunxh 2024/11/16
 */
@Data
public class DataPermissionEntityDTO<I> extends BaseDTO<I> {

    @Schema(title = "机构ID")
    protected Integer sysOrgId;

    @Schema(title = "角色ID")
    protected Integer sysRoleId;
}
