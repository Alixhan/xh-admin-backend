package com.xh.common.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 存放系统登录的用户信息DTO
 *
 * @author sunxh 2023/3/1
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SysUserDTO extends BaseDTO<Integer> {

    @Schema(title = "用户代码")
    private String code;
    @Schema(title = "用户名")
    private String name;
    @Schema(title = "头像")
    private String avatar;
    @Schema(title = "手机号码")
    private String telephone;
    @Schema(title = "是否启用")
    private Boolean enabled;
}
