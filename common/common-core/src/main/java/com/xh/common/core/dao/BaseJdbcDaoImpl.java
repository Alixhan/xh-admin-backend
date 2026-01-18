package com.xh.common.core.dao;

import com.xh.common.core.dao.sql.*;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BaseJdbcDaoImpl implements BaseJdbcDao {

    private final JdbcTemplate jdbcTemplate;

    public BaseJdbcDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public <K> K findById(Class<K> clazz, Serializable id) {
        SqlExecutor sqlExecutor = this.getSqlExecutor();
        return sqlExecutor.findById(jdbcTemplate, clazz, id);
    }

    @Override
    public <K> K findBySql(Class<K> clazz, String sql, Object... args) {
        SqlExecutor sqlExecutor = this.getSqlExecutor();
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
        SqlExecutor sqlExecutor = this.getSqlExecutor();
        sql = sqlExecutor.convertSql(sql);
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(clazz), args);
    }

    @Override
    public PageResult<Map> query(PageQuery<?> pageQuery) {
        return query(Map.class, pageQuery);
    }

    @Override
    public <K> PageResult<K> query(Class<K> clazz, PageQuery<?> pageQuery) {
        String sql = pageQuery.getSql();
        SqlExecutor sqlExecutor = this.getSqlExecutor();
        sql = sqlExecutor.convertSql(sql);
        PageResult<K> pageResult = new PageResult<>();
        if (pageQuery.getIsPage()) {
            String pageSql = sqlExecutor.getPageSql(sql, pageQuery.getCurrentPage(), pageQuery.getPageSize());
            String totalSql = "SELECT COUNT(1) FROM (%s) PAGE".formatted(sql);
            Integer total = jdbcTemplate.queryForObject(totalSql, Integer.class, pageQuery.getArgs().toArray());
            List<K> list = findList(clazz, pageSql, pageQuery.getArgs().toArray());
            pageResult.setList(list);
            pageResult.setTotal(total);
            pageResult.setIsPage(true);
            pageResult.setCurrentPage(pageQuery.getCurrentPage());
            pageResult.setPageSize(pageQuery.getPageSize());
        } else {
            List<K> list = findList(clazz, sql, pageQuery.getArgs().toArray());
            pageResult = new PageResult<>(list, list.size());
        }
        return pageResult;
    }

    @Override
    public <E> void insert(E[] entities) {
        SqlExecutor sqlExecutor = this.getSqlExecutor();
        for (E entity : entities) {
            this.autoSet(PersistenceType.INSERT, entity);
        }
        sqlExecutor.toInsert(jdbcTemplate, entities);
    }

    @Override
    public <E> void insert(E entity) {
        Object[] entities = {entity};
        this.insert(entities);
    }

    @Override
    public <E> int update(E entity) {
        return update(entity, null);
    }

    @Override
    public <E> int update(E entity, ColumnPicker columnPicker) {
        SqlExecutor sqlExecutor = this.getSqlExecutor();
        this.autoSet(PersistenceType.UPDATE, entity);
        return sqlExecutor.toUpdate(jdbcTemplate, entity, columnPicker);
    }

    @Override
    public <E> void deleteById(Class<E> clazz, Serializable id) {
        SqlExecutor sqlExecutor = this.getSqlExecutor();
        sqlExecutor.toDeleteById(jdbcTemplate, clazz, id);
    }

    /**
     * 获取数据库类型
     */
    private static final ConcurrentHashMap<JdbcTemplate, String> dbTypeMap = new ConcurrentHashMap<>();

    private String getDbType() {
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
    public SqlExecutor getSqlExecutor() {
        var dbType = this.getDbType();
        return switch (dbType) {
            case "MySQL" -> new MysqlExecutor();
            case "PostgreSQL" -> new PostgreSqlExecutor();
            default -> throw new RuntimeException("%s不支持".formatted(dbType));
        };
    }

    /**
     * 根据实体的 AutoSet注解自动注入值
     */
    private void autoSet(PersistenceType persistenceType, Object entity) {
        EntityStaff entityStaff = EntityStaff.init(entity.getClass());
        entityStaff.autoSet(persistenceType, entity);
    }
}
