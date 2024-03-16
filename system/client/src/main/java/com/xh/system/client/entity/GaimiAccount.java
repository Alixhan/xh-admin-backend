package com.xh.system.client.entity;

import com.xh.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Schema(title = "改密")
@Table
@Data
@EqualsAndHashCode(callSuper = true)
public class GaimiAccount extends BaseEntity<Integer> {

    @Schema(title = "用户名")
    private String username;
    @Schema(title = "密码")
    private String password;
    @Schema(title = "备注")
    private String remark;
}
