package com.xh.common.core.dao.sql;

import com.xh.common.core.dao.PersistenceType;
import com.xh.common.core.entity.AutoSet;
import com.xh.common.core.entity.AutoSetFun;
import com.xh.common.core.utils.CommonUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 实体的映射详情
 * sunxh 2024/12/4
 */
@Getter
public class EntityStaff<E> {
    private static final ConcurrentMap<Class<?>, EntityStaff<?>> classMap = new ConcurrentHashMap<>();

    private static <T> void register(Class<T> clazz, EntityStaff<T> staff) {
        classMap.put(clazz, staff);
    }

    @SuppressWarnings("unchecked")
    private static <T> EntityStaff<T> getStaff(Class<T> clazz) {
        return (EntityStaff<T>) classMap.get(clazz);
    }

    /**
     * 实体Class
     */
    private final Class<E> clazz;

    /**
     * 实体对应表名
     */
    private final String tableName;

    /**
     * 主键列
     */
    private final Deque<EntityColumnStaff<E>> idColumns = new LinkedList<>();

    /**
     * 所有列
     */
    private final Deque<EntityColumnStaff<E>> columns = new LinkedList<>();

    /**
     * 初始化EntityStaff
     */
    public static <E> EntityStaff<E> init(Class<E> clazz) {
        var staff = EntityStaff.getStaff(clazz);
        if (staff == null) {
            Table table = clazz.getAnnotation(Table.class);
            if (table == null) {
                throw new PersistenceException("%s 不是一个实体".formatted(clazz.getSimpleName()));
            }
            var tableName = table.name();
            if (CommonUtil.isEmpty(tableName)) tableName = CommonUtil.toLowerUnderscore(clazz.getSimpleName());
            staff = new EntityStaff<>(clazz, tableName);
            staff.columns.addAll(EntityStaff.getColumns(clazz));
            staff.columns.stream().filter(EntityColumnStaff::getIsId).forEach(staff.idColumns::add);
            EntityStaff.register(clazz, staff);
        }
        return staff;
    }

    /**
     * 获取实体类所有列
     */
    public static <E> Deque<EntityColumnStaff<E>> getColumns(Class<E> clazz) {
        Deque<EntityColumnStaff<E>> columns = new LinkedList<>();
        Collection<Field> fields = CommonUtil.getAllFields(clazz);
        for (Field field : fields) {
            Transient ignoredField = field.getAnnotation(Transient.class);
            if (ignoredField == null) columns.add(new EntityColumnStaff<>(field));
        }
        return columns;
    }

    private EntityStaff(Class<E> clazz, String tableName) {
        this.clazz = clazz;
        this.tableName = tableName;
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
    public static class EntityColumnStaff<E> {
        /**
         * Field
         */
        private Field field;

        /**
         * 实体属性名
         */
        private String fieldName;

        /**
         * 表字段名
         */
        private String columnName;

        /**
         * 标题名称
         */
        private String title;

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

        private EntityColumnStaff(Field field) {
            this.field = field;
            String fieldName = field.getName();
            Column column = field.getAnnotation(Column.class);
            String columnName = null;
            if (column != null) columnName = column.name();
            if (CommonUtil.isEmpty(columnName)) columnName = CommonUtil.toLowerUnderscore(fieldName);

            this.setColumnName(columnName);
            this.setFieldName(fieldName);
            this.setAutoSet(field.getAnnotation(AutoSet.class));

            Schema schema = field.getAnnotation(Schema.class);
            if (schema != null) {
                this.setTitle(schema.title());
            }

            if (field.getAnnotation(Id.class) != null) {
                this.setIsId(true);
                this.setGeneratedValue(field.getAnnotation(GeneratedValue.class));
            } else {
                this.setIsId(false);
            }
            field.setAccessible(true);
            this.setField(field);
        }

        public static <E> EntityColumnStaff<E> create(Class<E> clazz, String fieldName) {
            Field f = CommonUtil.getField(clazz, fieldName);
            if (f == null) throw new RuntimeException(fieldName + "在" + clazz.getName() + "不存在");
            return new EntityColumnStaff<>(f);
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
}
