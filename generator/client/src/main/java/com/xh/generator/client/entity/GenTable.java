package com.xh.generator.client.entity;

import com.xh.common.core.entity.BaseEntity;
import com.xh.generator.client.dto.GenTableColumnDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


/**
 * 代码生成表
 * sunxh 2024/11/17
 */
@Table
@Data
@Schema(title = "代码生成表")
@EqualsAndHashCode(callSuper = true)
public class GenTable extends BaseEntity<Integer> {

    @Schema(title = "设计方式")
    private String designType;

    @Schema(title = "表名")
    private String tableName;

    @Schema(title = "表注释")
    private String tableComment;

    @Schema(title = "实体类名")
    private String entityName;

    @Schema(title = "功能名")
    private String name;

    @Schema(title = "所属服务")
    private String service;

    @Schema(title = "模块名")
    private String module;

    @Schema(title = "作者")
    private String author;

    @Schema(title = "继承类")
    private String extend;

    @Schema(title = "列json串")
    private String columnsJson;

    @Schema(title = "前端项目生成路径")
    private String frontendPath;

    @Schema(title = "后端项目生成路径")
    private String backendPath;

    @Schema(title = "是否创建系统菜单")
    private Boolean isCreateMenu;

    @Schema(title = "是否启用数据权限")
    private Boolean isDataPermission;

    @Schema(title = "列")
    @Transient
    private List<GenTableColumnDTO> columns;
}
