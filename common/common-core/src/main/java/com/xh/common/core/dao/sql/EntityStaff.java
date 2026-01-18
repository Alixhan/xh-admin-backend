package com.xh.common.core.dao.sql;

import com.xh.common.core.dao.PersistenceType;
import com.xh.common.core.entity.AutoSet;
import com.xh.common.core.entity.AutoSetFun;
import com.xh.common.core.utils.CommonUtil;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
public class EntityStaff {
    private static final ConcurrentMap<Class<?>, EntityStaff> classMap = new ConcurrentHashMap<>();

    private static void register(Class<?> clazz, EntityStaff staff) {
        classMap.put(clazz, staff);
    }

    private static EntityStaff getStaff(Class<?> clazz) {
        return classMap.get(clazz);
    }

    /**
     * 实体Class
     */
    private final Class<?> clazz;

    /**
     * 实体对应表名
     */
    private final String tableName;

    /**
     * 主键列
     */
    private final Deque<EntityColumnStaff> idColumns = new LinkedList<>();

    /**
     * 所有列
     */
    private final Deque<EntityColumnStaff> columns = new LinkedList<>();

    /**
     * 初始化EntityStaff
     */
    public static EntityStaff init(Class<?> clazz) {
        var staff = EntityStaff.getStaff(clazz);
        if (staff == null) {
            Table table = clazz.getAnnotation(Table.class);
            if (table == null) {
                throw new PersistenceException("%s 不是一个实体".formatted(clazz.getSimpleName()));
            }
            var tableName = table.name();
            if (CommonUtil.isEmpty(tableName)) tableName = CommonUtil.toLowerUnderscore(clazz.getSimpleName());
            staff = new EntityStaff(clazz, tableName);
            staff.columns.addAll(EntityStaff.getColumns(clazz));
            staff.columns.stream().filter(EntityColumnStaff::getIsId).forEach(staff.idColumns::add);
            EntityStaff.register(clazz, staff);
        }
        return staff;
    }

    /**
     * 获取实体类所有列
     */
    public static Deque<EntityColumnStaff> getColumns(Class<?> clazz) {
        Deque<EntityColumnStaff> columns = new LinkedList<>();
        Collection<Field> fields = CommonUtil.getAllFields(clazz);
        for (Field field : fields) {
            Transient ignoredField = field.getAnnotation(Transient.class);
            if (ignoredField == null) columns.add(new EntityColumnStaff(field));
        }
        return columns;
    }

    private EntityStaff(Class<?> clazz, String tableName) {
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
            AutoSet autoSet = column.getAutoSet();
            if (autoSet != null) {
                for (AutoSetFun autoSetFun : autoSet.value()) {
                    autoSetFun.fun.exec(persistenceType, column.getField(), entity);
                }
            }
        });
    }
}
