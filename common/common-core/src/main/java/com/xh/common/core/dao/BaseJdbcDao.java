package com.xh.common.core.dao;

import com.xh.common.core.dao.sql.ColumnPicker;
import com.xh.common.core.dao.sql.SqlExecutor;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface BaseJdbcDao {
    /**
     * 通过ID查询实体
     *
     * @param clazz 实体类 Class 类型
     * @param id    主键值
     * @return 返回实体对象
     */
    <K> K findById(Class<K> clazz, Serializable id);

    /**
     * 通过语句查询，返回第一行记录
     *
     * @param clazz 实体类Class类型
     * @param sql   查询语句
     * @param args  占位符变量
     * @return 第一行匹配的数据
     */
    <K> K findBySql(Class<K> clazz, String sql, Object... args);

    /**
     * 获取列表数据
     *
     * @param clazz 接受对象类型
     * @param sql   查询语句
     * @param args  占位符变量
     * @return 返回匹配的记录
     */
    <K> List<K> findList(Class<K> clazz, String sql, Object... args);

    /**
     * 分页查询，Map类型
     *
     * @param pageQuery 分页查询配置
     * @return Map 类型的分页数据
     */
    PageResult<Map> query(PageQuery<?> pageQuery);

    /**
     * 分页查询，对象类型
     *
     * @param clazz     接受对象类型
     * @param pageQuery 分页查询配置
     * @return 对象类型的分页数据
     */
    <K> PageResult<K> query(Class<K> clazz, PageQuery<?> pageQuery);

    /**
     * 配量插入实体数据
     *
     * @param entities 实体数据数组
     */
    <E> void insert(E[] entities);

    /**
     * 插入单条实体数据
     *
     * @param entity 实体数据
     */
    <E> void insert(E entity);

    /**
     * 更新单条实体数据
     *
     * @param entity 实体数据
     */
    <E> int update(E entity);

    /**
     * 更新单条实体数据，可选取列更新
     *
     * @param entity 实体数据
     * @param columnPicker 列选取器
     */
    <E> int update(E entity, ColumnPicker columnPicker);

    /**
     * 通过主键删除数据（物理删除）
     *
     * @param clazz 实体类Class类型
     * @param id    主键值
     */
    <E> void deleteById(Class<E> clazz, Serializable id);

    /**
     * 获取默认数据源 SqlExecutor
     */
    SqlExecutor getSqlExecutor();
}
