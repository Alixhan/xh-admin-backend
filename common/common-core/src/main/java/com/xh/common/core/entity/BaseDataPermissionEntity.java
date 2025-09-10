package com.xh.common.core.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 数据权限基础实体
 * 会自动写入当前机构ID和当前角色ID
 * sunxh 2024/8/24
 */
@Getter
@Setter
public class BaseDataPermissionEntity<I extends Serializable> extends BaseEntity<I> implements Serializable {

    @Schema(title = "机构ID")
    @AutoSet(AutoSetFun.CURRENT_ORG)
    protected Integer sysOrgId;

    @Schema(title = "角色ID")
    @AutoSet(AutoSetFun.CURRENT_ROLE)
    protected Integer sysRoleId;
}
