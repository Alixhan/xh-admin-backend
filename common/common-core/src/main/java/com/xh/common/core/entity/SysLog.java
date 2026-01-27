package com.xh.common.core.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;


@Schema(title = "系统日志")
@Table
@Data
@EqualsAndHashCode(callSuper = true)
public class SysLog extends BaseEntity<Integer> {
    @Schema(title = "token")
    private String token;

    @Schema(title = "请求方法")
    private String method;

    @Schema(title = "请求路径")
    private String url;

    @Schema(title = "请求内容类型")
    private String contentType;

    @Schema(title = "请求参数")
    private String requestParameter;

    @Schema(title = "请求体")
    private String requestBody;

    @Schema(title = "响应体")
    private String responseBody;

    @Schema(title = "ip地址")
    private String ip;

    @Schema(title = "ip属地")
    private String ipAddress;

    @Schema(title = "模块")
    private String tag;

    @Schema(title = "操作")
    private String operation;

    @Schema(title = "开始时间")
    private LocalDateTime startTime;

    @Schema(title = "结束时间")
    private LocalDateTime endTime;

    @Schema(title = "耗时ms")
    private Long time;

    @Schema(title = "响应状态")
    private String status;

    @Schema(title = "异常堆栈信息")
    private String stackTrace;

    @Schema(title = "使用语言")
    private String locale;

    @Schema(title = "使用语言名称")
    private String localeLabel;

    @Schema(title = "使用机构ID")
    private Integer sysOrgId;

    @Schema(title = "使用机构名称")
    private String orgName;

    @Schema(title = "使用角色ID")
    private Integer sysRoleId;

    @Schema(title = "使用角色名称")
    private String roleName;

    @Schema(title = "登录设备")
    private String deviceType;

    @Transient
    @Schema(title = "用户名称")
    private String name;
}
