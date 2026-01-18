package com.xh.common.core.dao.sql;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.PersistenceException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 利用jdbcTempLate 进行ORM映射
 *
 * @author sunxh
 * @since 2024/12/3
 */
public interface SqlExecutor {
    /**
     * 执行insert语句
     */
    default <E> void toInsert(JdbcTemplate jdbcTemplate, E[] entity) {
        EntityStaff entityStaff = EntityStaff.init(entity[0].getClass());
        String columnStr = entityStaff.getColumns().stream()
                .map(EntityColumnStaff::getColumnName)
                .collect(Collectors.joining(","));
        String valueStr = entityStaff.getColumns().stream()
                .map(i -> {
                    GeneratedValue generatedValue = i.getGeneratedValue();
                    // 如果是序列类型，则此主键由序列生成
                    if (generatedValue != null && generatedValue.strategy() == GenerationType.SEQUENCE) {
                        return generatedValue.generator();
                    }
                    return ":" + i.getFieldName();
                })
                .collect(Collectors.joining(","));
        var sql = "INSERT INTO %s (%s) VALUES (%s)".formatted(entityStaff.getTableName(), columnStr, valueStr);

        BeanPropertySqlParameterSource[] args = Arrays.stream(entity).map(BeanPropertySqlParameterSource::new)
                .toArray(BeanPropertySqlParameterSource[]::new);
        GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();

        new NamedParameterJdbcTemplate(jdbcTemplate).batchUpdate(sql, args, generatedKeyHolder);

        List<Map<String, Object>> keyList = generatedKeyHolder.getKeyList();
        for (int i = 0; i < keyList.size(); i++) {
            for (EntityColumnStaff idColumn : entityStaff.getIdColumns()) {
                var val = keyList.get(i).get(idColumn.getColumnName());
                idColumn.setFieldValue(entity[i], val);
            }
        }
    }

    /**
     * 执行update语句
     */
    default <E> int toUpdate(JdbcTemplate jdbcTemplate, E entity, ColumnPicker columnPicker) {
        EntityStaff entityStaff = EntityStaff.init(entity.getClass());
        var columns = entityStaff.getColumns().stream().toList();
        if (columnPicker != null) columns = columnPicker.exec(columns);
        if (columns.isEmpty()) throw new RuntimeException("更新列为空");
        String columnStr = columns.stream()
                .map(i -> "%s = :%s".formatted(i.getColumnName(), i.getFieldName()))
                .collect(Collectors.joining(","));
        String idWhereStr = entityStaff.getIdColumns().stream()
                .map(i -> "%s = :%s".formatted(i.getColumnName(), i.getFieldName()))
                .collect(Collectors.joining(","));
        var sql = "UPDATE %s SET %s WHERE %s".formatted(entityStaff.getTableName(), columnStr, idWhereStr);

        BeanPropertySqlParameterSource arg = new BeanPropertySqlParameterSource(entity);

        return new NamedParameterJdbcTemplate(jdbcTemplate).update(sql, arg);
    }

    /**
     * 执行delete语句
     */
    default <E> void toDeleteById(JdbcTemplate jdbcTemplate, Class<E> clazz, Object id) {
        EntityStaff entityStaff = EntityStaff.init(clazz);
        assert entityStaff.getIdColumns().peek() != null;
        String idWhereStr = entityStaff.getIdColumns().peek().getColumnName() + " = ?";
        var sql = "DELETE FROM %s WHERE %s".formatted(entityStaff.getTableName(), idWhereStr);
        jdbcTemplate.update(sql, id);
    }

    /**
     * 执行findById语句
     */
    default <E> E findById(JdbcTemplate jdbcTemplate, Class<E> clazz, Object id) {
        EntityStaff entityStaff = EntityStaff.init(clazz);
        if (entityStaff.getIdColumns().isEmpty()) {
            throw new PersistenceException("实体没有主键");
        }
        if (entityStaff.getIdColumns().size() > 1) {
            throw new PersistenceException("无法查询联合主键的数据");
        }
        String columnStr = entityStaff.getColumns().stream()
                .map(i -> "%s as %s".formatted(i.getColumnName(), i.getFieldName()))
                .collect(Collectors.joining(","));
        assert entityStaff.getIdColumns().peek() != null;
        String idWhere = entityStaff.getIdColumns().peek().getColumnName() + " = ?";
        var sql = "select %s FROM %s WHERE %s".formatted(columnStr, entityStaff.getTableName(), idWhere);
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(clazz), id);
    }

    /**
     * 获取分页语句
     */
    default String getPageSql(String sql, int currentPage, int pageSize) {
        return "SELECT * FROM (%s) PAGE LIMIT %s,%s".formatted(sql, (currentPage - 1) * pageSize, pageSize);
    }

    /**
     * 转换一下sql包裹符
     */
    default String convertSql(String sql) {
        return sql;
    }
}
