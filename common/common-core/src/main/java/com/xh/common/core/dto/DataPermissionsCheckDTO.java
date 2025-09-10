package com.xh.common.core.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据权限验证结果DTO
 */
@Data
public class DataPermissionsCheckDTO implements Serializable {
    Boolean result;
    Serializable id;
}
