package com.xh.common.core.web;

import lombok.Getter;
import lombok.Setter;

/**
 * 无数据权限异常，当访问越权数据时抛出
 *
 * @author sxh
 * @since 2025-09-10
 */
@Getter
@Setter
public class NoDataPermissionException extends RuntimeException {
    //数据实体名称
    String dataEntity;

    //验证sql
    String verificationSql;

    //sql参数
    Object sqlParam;

    public NoDataPermissionException(String dataEntity, String verificationSql, Object sqlParam) {
        super("数据权限验证失败，数据实体：%s \n验证sql: %s\nsql参数：%s".formatted(dataEntity, verificationSql, sqlParam));
        this.dataEntity = dataEntity;
        this.verificationSql = verificationSql;
    }
}
