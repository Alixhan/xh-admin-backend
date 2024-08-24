package com.xh.common.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 在线用户DTO
 * sunxh 2023/8/19
 */

@Schema(title = "在线用户DTO")
@Data
public class OnlineUserDTO implements Serializable {
    @Schema(title = "token")
    private String token;
    @Schema(title = "登录用户ID")
    private Integer userId;
    @Schema(title = "登录账户")
    private String userCode;
    @Schema(title = "登录账户名称")
    private String userName;
    @Schema(title = "登录时间")
    private LocalDateTime loginTime;
    @Schema(title = "ip地址")
    private String loginIp;
    @Schema(title = "登录地点")
    private String loginAddress;
    @Schema(title = "登录浏览器")
    private String loginBrowser;
    @Schema(title = "浏览器版本")
    private String browserVersion;
    @Schema(title = "操作系统")
    private String loginOs;
    @Schema(title = "是否手机端")
    private Boolean isMobile;
    @Schema(title = "当前语言")
    private String locale;
    @Schema(title = "当前语言名称")
    private String localeLabel;
    @Schema(title = "当前机构ID")
    private Integer orgId;
    @Schema(title = "当前角色ID")
    private Integer roleId;
    @Schema(title = "当前机构")
    private String orgName;
    @Schema(title = "当前使用角色名称")
    private String roleName;
}
