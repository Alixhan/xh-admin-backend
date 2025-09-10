package com.xh.generator.client.dto;

import com.xh.common.core.dto.BaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 代码生成列明细
 * sunxh 2024/11/17
 */
@Table
@Data
@Schema(title = "代码生成字段明细")
@EqualsAndHashCode(callSuper = true)
public class GenTableColumnDTO extends BaseDTO<Integer> {

    @Schema(title = "表单类型")
    private String formType;

    @Schema(title = "数据字典类型")
    private Integer dictType;

    @Schema(title = "属性名")
    private String prop;

    @Schema(title = "名称")
    private String label;

    @Schema(title = "验证规则")
    private List<String> rules;

    @Schema(title = "虚拟列")
    private Boolean isVirtual;

    @Schema(title = "主键")
    private Boolean primaryKey;

    @Schema(title = "主键类型")
    private String primaryKeyType;

    @Schema(title = "字段名")
    private String columnName;

    @Schema(title = "字段类型")
    private String colType;

    @Schema(title = "长度")
    private Integer columnSize;

    @Schema(title = "小数点")
    private Integer decimalDigits;

    @Schema(title = "注释")
    private String remarks;

    @Schema(title = "简单查询")
    private Boolean isQuery;

    @Schema(title = "表格列表")
    private Boolean isTable;

    @Schema(title = "表单维护")
    private Boolean isForm;

    @Schema(title = "导入")
    private Boolean isImport;

    @Schema(title = "导出")
    private Boolean isExport;

    @Schema(title = "是否继承而来的列")
    private Boolean isExtend;

    @Schema(title = "Java类型")
    private String javaType;

    @Schema(title = "简易查询sql")
    private String querySql;

    @Schema(title = "列表列")
    private String tableColStr;

    @Schema(title = "简单查询列")
    private String queryColStr;

    @Schema(title = "表单列")
    private String formColStr;

    @Schema(title = "导入列")
    private String importColStr;

    @Schema(title = "sql列")
    private String sqlColStr;
}
