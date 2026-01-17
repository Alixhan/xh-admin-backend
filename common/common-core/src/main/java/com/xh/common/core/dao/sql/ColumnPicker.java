package com.xh.common.core.dao.sql;

import java.util.List;

/**
 * 列选取器，用于从实体类中选取部分列出来
 * 主要用于update时仅更新部分列
 *
 * @author sunxh 2026/1/17
 */
@FunctionalInterface
public interface ColumnPicker {
    /**
     * 选取部分列出来
     *
     * @param columns 原实体列
     * @return 选择后的列
     */
    List<EntityStaff.EntityColumnStaff> exec(List<EntityStaff.EntityColumnStaff> columns);
}
