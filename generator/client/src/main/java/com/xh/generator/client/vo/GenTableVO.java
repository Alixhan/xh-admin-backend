package com.xh.generator.client.vo;

import com.xh.generator.client.dto.GenTableColumnDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 代码生成 VO
 * sunxh 2024/11/17
 */
@Data
@Schema(title = "代码生成表")
public class GenTableVO {

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

    @Schema(title = "生成日期")
    private String date;

    @Schema(title = "controller包名")
    private String controllerPackage;

    @Schema(title = "实体包名")
    private String entityPackage;

    @Schema(title = "实体字段名称")
    private String entityVarName;

    @Schema(title = "实体继承类")
    private String entityExtendClass;

    @Schema(title = "service包名")
    private String servicePackage;

    @Schema(title = "service类名")
    private String serviceName;

    @Schema(title = "service变量")
    private String serviceVarName;

    @Schema(title = "权限前缀")
    private String permissionPrefix;

    @Schema(title = "dto包名")
    private String dtoPackage;

    @Schema(title = "dto类名")
    private String dtoName;

    @Schema(title = "dto属性名")
    private String dtoVarName;

    @Schema(title = "dto继承类")
    private String dtoExtendClass;

    @Schema(title = "查询方法名")
    private String queryFun;

    @Schema(title = "保存方法名")
    private String saveFun;

    @Schema(title = "详情方法名")
    private String getFun;

    @Schema(title = "删除方法名")
    private String delFun;

    @Schema(title = "导入方法名")
    private String importFun;

    @Schema(title = "api路径")
    private String apiPath;

    @Schema(title = "列表页全路径")
    private String indexViewPath;

    @Schema(title = "列表排序")
    private String orderBy;

    @Schema(title = "主键字段")
    private String idProp;

    @Schema(title = "主键get方法")
    private String primaryKeyGet;

    @Schema(title = "controller映射路径")
    private String mappingPath;

    @Schema(title = "是否有导入")
    private Boolean hasImport;

    @Schema(title = "是否有@Id注解")
    private Boolean hasId;

    @Schema(title = "是否有LocalDate")
    private Boolean hasLocalDate;

    @Schema(title = "是否有LocalDateTime")
    private Boolean hasLocalDateTime;

    @Schema(title = "是否有BigDecimal")
    private Boolean hasBigDecimal;

    @Schema(title = "是否包含数据字典")
    private Set<String> hasDict = new HashSet<>();

    @Schema(title = "前端项目路径")
    private String frontendPath;

    @Schema(title = "后端项目路径")
    private String backendPath;

    @Schema(title = "是否创建系统菜单")
    private Boolean isCreateMenu;

    @Schema(title = "是否启用数据权限")
    private Boolean isDataPermission;

    @Schema(title = "建表语句")
    private String createTableSql;

    @Schema(title = "菜单id")
    private Integer menuId;

    @Schema(title = "列")
    private List<GenTableColumnDTO> columns;

    @Schema(title = "当前登录用户ID")
    private Integer userId;
}
