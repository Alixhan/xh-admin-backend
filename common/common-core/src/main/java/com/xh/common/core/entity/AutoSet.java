package com.xh.common.core.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于注释实体类字段
 * 实现持久化时自动注入字段值的逻辑
 * sunxh 2024/5/7
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoSet {

    /**
     * 自动注入值执行方法
     */
    AutoSetFun[] value();
}
