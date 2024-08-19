package com.xh.system.client.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


@Schema(title = "系统数据实体")
@Table
@Data
public class SysDataEntity {

    @Schema(title = "主键ID")
    @Id
    private String id;

    @Schema(title = "数据实体名称")
    private String name;
}
