package com.xh.common.core.dao;

import com.xh.common.core.dao.sql.EntityStaff;
import com.xh.common.core.dao.sql.MysqlExecutor;
import com.xh.common.core.dao.sql.PostgreSqlExecutor;
import com.xh.common.core.dao.sql.SqlExecutor;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository(value = "baseJdbcDao")
public class BaseJdbcDaoImpl implements BaseJdbcDao {

    @Resource
    protected JdbcTemplate primaryJdbcTemplate;

    @Override
    public <K> K findById(Class<K> clazz, Serializable id) {
        return this.findById(clazz, primaryJdbcTemplate, id);
    }

    @Override
    public <K> K findById(Class<K> clazz, JdbcTemplate jdbcTemplate, Serializable id) {
        SqlExecutor sqlExecutor = this.getSqlExecutor(jdbcTemplate);
        return sqlExecutor.findById(jdbcTemplate, clazz, id);
    }

    @Override
    public <K> K findBySql(Class<K> clazz, String sql, Object... args) {
        return findBySql(clazz, sql, primaryJdbcTemplate, args);
    }

    @Override
    public <K> K findBySql(Class<K> clazz, String sql, JdbcTemplate jdbcTemplate, Object... args) {
        SqlExecutor sqlExecutor = this.getSqlExecutor(jdbcTemplate);
        sql = sqlExecutor.convertSql(sql);
        K obj = null;
        List<K> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(clazz), args);
        if (!list.isEmpty()) {
            obj = list.getFirst();
        }
        return obj;
    }

    @Override
    public <K> List<K> findList(Class<K> clazz, String sql, Object... args) {
        return findList(clazz, sql, primaryJdbcTemplate, args);
    }

    @Override
    public <K> List<K> findList(Class<K> clazz, String sql, JdbcTemplate jdbcTemplate, Object... args) {
        SqlExecutor sqlExecutor = this.getSqlExecutor(jdbcTemplate);
        sql = sqlExecutor.convertSql(sql);
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(clazz), args);
    }

    @Override
    public PageResult<Map> query(PageQuery<?> pageQuery) {
        return query(Map.class, pageQuery);
    }

    @Override
    public <K> PageResult<K> query(Class<K> clazz, PageQuery<?> pageQuery) {
        return query(clazz, pageQuery, primaryJdbcTemplate);
    }

    @Override
    public <K> PageResult<K> query(Class<K> clazz, PageQuery<?> pageQuery, JdbcTemplate jdbcTemplate) {
        String sql = pageQuery.getSql();
        SqlExecutor sqlExecutor = this.getSqlExecutor(jdbcTemplate);
        sql = sqlExecutor.convertSql(sql);
        PageResult<K> pageResult = new PageResult<>();
        if (pageQuery.getIsPage()) {
            String pageSql = sqlExecutor.getPageSql(sql, pageQuery.getCurrentPage(), pageQuery.getPageSize());
            String totalSql = "SELECT COUNT(1) FROM (%s) PAGE".formatted(sql);
            Integer total = jdbcTemplate.queryForObject(totalSql, Integer.class, pageQuery.getArgs().toArray());
            List<K> list = findList(clazz, pageSql, jdbcTemplate, pageQuery.getArgs().toArray());
            pageResult.setList(list);
            pageResult.setTotal(total);
            pageResult.setIsPage(true);
            pageResult.setCurrentPage(pageQuery.getCurrentPage());
            pageResult.setPageSize(pageQuery.getPageSize());
        } else {
            List<K> list = findList(clazz, sql, jdbcTemplate, pageQuery.getArgs().toArray());
            pageResult = new PageResult<>(list, list.size());
        }
        return pageResult;
    }

    @Override
    public <E> void insert(E[] entities) {
        this.insert(primaryJdbcTemplate, entities);
    }

    @Override
    public <E> void insert(JdbcTemplate jdbcTemplate, E[] entities) {
        SqlExecutor sqlExecutor = this.getSqlExecutor(jdbcTemplate);
        for (E entity : entities) {
            this.autoSet(PersistenceType.INSERT, entity);
        }
        sqlExecutor.toInsert(jdbcTemplate, entities);
    }

    @Override
    public <E> void insert(E entity) {
        this.insert(primaryJdbcTemplate, entity);
    }

    @Override
    public <E> void insert(JdbcTemplate jdbcTemplate, E entity) {
        Object[] entities = {entity};
        this.insert(jdbcTemplate, entities);
    }

    @Override
    public <E> void update(E entity) {
        update(primaryJdbcTemplate, entity);
    }

    @Override
    public <E> void update(JdbcTemplate jdbcTemplate, E entity) {
        SqlExecutor sqlExecutor = this.getSqlExecutor(jdbcTemplate);
        this.autoSet(PersistenceType.UPDATE, entity);
        sqlExecutor.toUpdate(jdbcTemplate, entity);
    }

    @Override
    public <E> void deleteById(Class<E> clazz, Serializable id) {
        this.deleteById(clazz, primaryJdbcTemplate, id);
    }

    @Override
    public <E> void deleteById(Class<E> clazz, JdbcTemplate jdbcTemplate, Serializable id) {
        SqlExecutor sqlExecutor = this.getSqlExecutor(jdbcTemplate);
        sqlExecutor.toDeleteById(jdbcTemplate, clazz, id);
    }

    /**
     * 获取数据库类型
     */
    private static final ConcurrentHashMap<JdbcTemplate, String> dbTypeMap = new ConcurrentHashMap<>();

    protected String getDbType(JdbcTemplate jdbcTemplate) {
        String dbType = dbTypeMap.get(jdbcTemplate);
        if (dbType == null) {
            try (Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
                dbType = connection.getMetaData().getDatabaseProductName();
                dbTypeMap.put(jdbcTemplate, dbType);
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
        return dbType;
    }

    @Override
    public SqlExecutor getSqlExecutor(JdbcTemplate jdbcTemplate) {
        var dbType = this.getDbType(jdbcTemplate);
        return switch (dbType) {
            case "MySQL" -> new MysqlExecutor();
            case "PostgreSQL" -> new PostgreSqlExecutor();
            default -> throw new RuntimeException("%s不支持".formatted(dbType));
        };
    }

    @Override
    public SqlExecutor getSqlExecutor() {
        return this.getSqlExecutor(primaryJdbcTemplate);
    }

    /**
     * 根据实体的 AutoSet注解自动注入值
     */
    private void autoSet(PersistenceType persistenceType, Object entity) {
        EntityStaff entityStaff = EntityStaff.init(entity.getClass());
        entityStaff.autoSet(persistenceType, entity);
    }
}
