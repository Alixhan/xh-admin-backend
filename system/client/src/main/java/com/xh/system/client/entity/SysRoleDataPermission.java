package com.xh.system.client.entity;

import com.xh.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Schema(title = "系统角色数据权限")
@Table
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleDataPermission extends BaseEntity<Integer> {

    @Schema(title = "角色ID")
    private Integer sysRoleId;

    @Schema(title = "数据实体ID")
    private String sysDataEntityId;

    @Schema(title = "数据权限ID")
    private Integer sysDataPermissionId;
}
