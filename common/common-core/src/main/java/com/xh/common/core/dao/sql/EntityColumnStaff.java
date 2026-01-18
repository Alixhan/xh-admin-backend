package com.xh.common.core.dao.sql;

import com.xh.common.core.entity.AutoSet;
import com.xh.common.core.utils.CommonUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.lang.reflect.Field;

/**
 * 实体映射列
 */
@Data
public class EntityColumnStaff {
    /**
     * Field
     */
    final private Field field;

    /**
     * 实体属性名
     */
    final private String fieldName;

    /**
     * 表字段名
     */
    final private String columnName;

    /**
     * 标题名称
     */
    final private String title;

    /**
     * 是否主键
     */
    final private Boolean isId;

    /**
     * 主键生成类型注解
     */
    final private GeneratedValue generatedValue;

    /**
     * 自动注入注解
     */
    final private AutoSet autoSet;

    public EntityColumnStaff(Field field) {
        this.field = field;
        String fieldName = field.getName();
        Column column = field.getAnnotation(Column.class);
        String columnName = null;
        if (column != null) columnName = column.name();
        if (CommonUtil.isEmpty(columnName)) columnName = CommonUtil.toLowerUnderscore(fieldName);

        this.columnName = columnName;
        this.fieldName = fieldName;
        this.autoSet = field.getAnnotation(AutoSet.class);

        Schema schema = field.getAnnotation(Schema.class);
        if (schema != null) {
            this.title = schema.title();
        } else {
            this.title = null;
        }

        if (field.getAnnotation(Id.class) != null) {
            this.isId = true;
            this.generatedValue = field.getAnnotation(GeneratedValue.class);
        } else {
            this.isId = false;
            this.generatedValue = null;
        }
        field.setAccessible(true);
    }

    public static EntityColumnStaff create(Class<?> clazz, String fieldName) {
        Field f = CommonUtil.getField(clazz, fieldName);
        if (f == null) throw new RuntimeException(fieldName + "在" + clazz.getName() + "不存在");
        return new EntityColumnStaff(f);
    }

    /**
     * 设置实体类相应字段值
     */
    public void setFieldValue(Object entity, Object fieldValue) {
        try {
            this.field.set(entity, fieldValue);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取实体类相应字段值
     */
    public Object getFieldValue(Object entity) {
        try {
            return this.field.get(entity);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}