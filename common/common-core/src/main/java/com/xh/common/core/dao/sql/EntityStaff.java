package com.xh.common.core.dao.sql;

import com.xh.common.core.entity.AutoSetFun;
import com.xh.common.core.dao.PersistenceType;
import com.xh.common.core.entity.AutoSet;
import com.xh.common.core.utils.CommonUtil;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 实体的映射详情
 * sunxh 2024/12/4
 */
@Setter
@Getter
public class EntityStaff {
    public static final ConcurrentMap<Class<?>, EntityStaff> classMap = new ConcurrentHashMap<>();

    /**
     * 实体Class
     */
    private Class<?> clazz;

    /**
     * 实体对应表名
     */
    private String tableName;

    /**
     * 主键列
     */
    private Queue<EntityColumnStaff> idColumns = new LinkedList<>();

    /**
     * 所有列
     */
    private Queue<EntityColumnStaff> columns = new LinkedList<>();

    /**
     * 初始化EntityStaff
     */
    public static EntityStaff init(Class<?> clazz) {
        var staff = classMap.get(clazz);
        if (staff == null) {
            staff = new EntityStaff();
            staff.clazz = clazz;
            Table table = clazz.getAnnotation(Table.class);
            if (table == null) {
                throw new PersistenceException("%s 不是一个实体".formatted(clazz.getSimpleName()));
            }

            staff.tableName = table.name();
            if (CommonUtil.isEmpty(staff.tableName)) {
                staff.tableName = CommonUtil.toLowerUnderscore(clazz.getSimpleName());
            }
            Collection<Field> fields = CommonUtil.getAllFields(clazz);
            for (Field field : fields) {
                String fieldName = field.getName();
                Transient ignoredField = field.getAnnotation(Transient.class);
                if (ignoredField == null) {
                    Column column = field.getAnnotation(Column.class);
                    String columnName = null;
                    if (column != null) columnName = column.name();
                    if (CommonUtil.isEmpty(columnName)) columnName = CommonUtil.toLowerUnderscore(fieldName);

                    EntityColumnStaff entityColumnStaff = new EntityColumnStaff();
                    entityColumnStaff.setColumnName(columnName);
                    entityColumnStaff.setFieldName(fieldName);
                    entityColumnStaff.setAutoSet(field.getAnnotation(AutoSet.class));
                    staff.columns.add(entityColumnStaff);

                    if (field.getAnnotation(Id.class) != null) {
                        entityColumnStaff.setIsId(true);
                        entityColumnStaff.setGeneratedValue(field.getAnnotation(GeneratedValue.class));
                        staff.idColumns.add(entityColumnStaff);
                    } else {
                        entityColumnStaff.setIsId(false);
                    }
                    field.setAccessible(true);
                    entityColumnStaff.setField(field);
                }
            }

            classMap.put(clazz, staff);
        }

        return staff;
    }

    private EntityStaff() {
    }

    /**
     * 根据实体的 AutoSet注解自动注入值
     */
    public void autoSet(PersistenceType persistenceType, Object entity) {
        if (this.clazz != entity.getClass()) {
            throw new ClassCastException("%s与%s类型不匹配".formatted(clazz.getSimpleName(), entity.getClass().getSimpleName()));
        }
        this.columns.forEach(column -> {
            // 自动注入值
            AutoSet autoSet = column.autoSet;
            if (autoSet != null) {
                for (AutoSetFun autoSetFun : autoSet.value()) {
                    autoSetFun.fun.exec(persistenceType, column.field, entity);
                }
            }
        });
    }

    /**
     * 实体映射列
     */
    @Data
    public static class EntityColumnStaff {
        /**
         * 属性名
         */
        private Field field;

        /**
         * 属性名
         */
        private String fieldName;

        /**
         * 字段名
         */
        private String columnName;

        /**
         * 是否主键
         */
        private Boolean isId;

        /**
         * 主键生成类型注解
         */
        private GeneratedValue generatedValue;

        /**
         * 自动注入注解
         */
        private AutoSet autoSet;

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
}
