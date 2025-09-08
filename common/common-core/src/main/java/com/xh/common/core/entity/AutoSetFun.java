package com.xh.common.core.entity;

import cn.dev33.satoken.stp.StpUtil;
import com.xh.common.core.dao.PersistenceType;
import com.xh.common.core.dto.OnlineUserDTO;
import com.xh.common.core.utils.LoginUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 执行持久化操作时，自动注入字段值
 * sunxh 2024/5/6
 */
@Slf4j
public enum AutoSetFun {
    /**
     * 新增时并且当字段为null时自动添时间为当前时间
     * 仅LocalDate或LocalDateTime类型生效
     */
    INSERT_NOW((persistenceType, field, object) -> {
        if (persistenceType == PersistenceType.INSERT) {
            try {
                field.setAccessible(true);
                if (field.get(object) != null) return;
                Class<?> type = field.getType();
                if (type == LocalDate.class) {
                    field.set(object, LocalDate.now());
                }
                if (type == LocalDateTime.class) {
                    field.set(object, LocalDateTime.now());
                }
            } catch (IllegalAccessException e) {
                log.error("自动注入失败", e);
            }
        }
    }),

    /**
     * 修改时自动添时间为当前时间
     * 仅LocalDate或LocalDateTime类型生效
     */
    UPDATE_NOW((persistenceType, field, object) -> {
        if (persistenceType == PersistenceType.UPDATE) {
            try {
                field.setAccessible(true);
                Class<?> type = field.getType();
                if (type == LocalDate.class) {
                    field.set(object, LocalDate.now());
                }
                if (type == LocalDateTime.class) {
                    field.set(object, LocalDateTime.now());
                }
            } catch (IllegalAccessException e) {
                log.error("自动注入失败", e);
            }
        }
    }),
    /**
     * 新增时并且字段值为null时，自动添加当前登录人员ID
     * 仅Integer类型有效
     */
    INSERT_BY((persistenceType, field, object) -> {
        if (persistenceType == PersistenceType.INSERT) {
            try {
                field.setAccessible(true);
                Class<?> type = field.getType();
                if (field.get(object) == null && type == Integer.class) {
                    Object id = StpUtil.getLoginIdDefaultNull();
                    if (id != null) {
                        field.set(object, Integer.parseInt(id.toString()));
                    }
                }
            } catch (IllegalAccessException e) {
                log.error("自动注入失败", e);
            }
        }
    }),
    /**
     * 修改时，自动更新为当前登录人ID
     * 仅LocalDate或LocalDateTime类型生效
     */
    UPDATE_BY((persistenceType, field, object) -> {
        if (persistenceType == PersistenceType.UPDATE) {
            try {
                field.setAccessible(true);
                Class<?> type = field.getType();
                if (type == Integer.class) {
                    Object id = StpUtil.getLoginIdDefaultNull();
                    if (id != null) {
                        field.set(object, Integer.parseInt(id.toString()));
                    }
                }
            } catch (IllegalAccessException e) {
                log.error("自动注入失败", e);
            }
        }
    }),
    /**
     * 实体保存时，如果字段为null，设置字段值为当前使用岗位机构id
     */
    CURRENT_ORG((persistenceType, field, object) -> {
        try {
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (field.get(object) == null && type == Integer.class) {
                OnlineUserDTO onlineUserInfo = LoginUtil.getOnlineUserInfo();
                if (onlineUserInfo != null) {
                    field.set(object, onlineUserInfo.getOrgId());
                }
            }
        } catch (IllegalAccessException e) {
            log.error("自动注入失败", e);
        }
    }),
    /**
     * 实体保存时，如果字段为null，设置字段值为当前使用岗位角色id
     */
    CURRENT_ROLE((persistenceType, field, object) -> {
        try {
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (field.get(object) == null && type == Integer.class) {
                OnlineUserDTO onlineUserInfo = LoginUtil.getOnlineUserInfo();
                if (onlineUserInfo != null) {
                    field.set(object, onlineUserInfo.getRoleId());
                }
            }
        } catch (IllegalAccessException e) {
            log.error("自动注入失败", e);
        }
    }),
    /**
     * 当字段值为null，默认值为false
     */
    DEFAULT_FALSE((persistenceType, field, object) -> {
        try {
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (field.get(object) == null && type == Boolean.class) {
                field.set(object, false);
            }
        } catch (IllegalAccessException e) {
            log.error("自动注入失败", e);
        }
    });

    public final AutoSetFunction<PersistenceType, Field, Object> fun;

    AutoSetFun(AutoSetFunction<PersistenceType, Field, Object> fun) {
        this.fun = fun;
    }

    @FunctionalInterface
    public interface AutoSetFunction<T1, T2, T3> {
        /**
         * @param persistenceType 操作类型
         * @param field           当前field
         * @param object          实体对象
         */
        void exec(T1 persistenceType, T2 field, T3 object);
    }
}
