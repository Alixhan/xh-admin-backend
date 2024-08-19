package com.xh.system.client.entity;

import com.xh.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Table
@Data
@Schema(title = "数据字典明细")
@EqualsAndHashCode(callSuper = true)
public class SysDictDetail extends BaseEntity<Integer> {
    @Schema(title = "字典类型ID")
    private Integer sysDictTypeId;

    @Schema(title = "上级id")
    private Integer parentId;

    @Schema(title = "字典值key")
    private String value;

    @Schema(title = "字典名称")
    private String label;

    @Schema(title = "排序号")
    private Integer order;

    @Schema(title = "是否启用")
    private Boolean enabled;

    @Schema(title = "数据字典类型ID")
    @Transient
    private String dictTypeId;

    @Schema(title = "数据自定类型名称")
    @Transient
    private String dictTypeName;
}
