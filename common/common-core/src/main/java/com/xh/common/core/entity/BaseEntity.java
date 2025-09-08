package com.xh.common.core.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class BaseEntity<I extends Serializable> implements Serializable {

    @Schema(title = "主键ID")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected I id;

    @Schema(title = "创建时间")
    @AutoSet(AutoSetFun.INSERT_NOW)
    private LocalDateTime createTime;

    @Schema(title = "修改时间")
    @AutoSet(AutoSetFun.UPDATE_NOW)
    private LocalDateTime updateTime;

    @Schema(title = "创建人")
    @AutoSet(AutoSetFun.INSERT_BY)
    private Integer createBy;

    @Schema(title = "修改人")
    @AutoSet(AutoSetFun.UPDATE_BY)
    private Integer updateBy;

    @Schema(title = "是否已删除")
    @AutoSet(AutoSetFun.DEFAULT_FALSE)
    private Boolean deleted;
}
