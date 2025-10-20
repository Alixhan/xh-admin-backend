package com.xh.common.core.dao.sql;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * mysql orm
 *
 * @author sunxh
 * @since 2024/12/6
 */
@Slf4j
public class MysqlExecutor implements SqlExecutor {

    @Override
    public <E> void toInsert(JdbcTemplate jdbcTemplate, E[] entitys) {
        EntityStaff entityStaff = EntityStaff.init(entitys[0].getClass());
        String columnStr = entityStaff.getColumns().stream()
                .map(EntityStaff.EntityColumnStaff::getColumnName)
                .collect(Collectors.joining("`,`", "`", "`"));
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
        String sql = "INSERT INTO `%s` (%s) VALUES (%s)".formatted(entityStaff.getTableName(), columnStr, valueStr);

        BeanPropertySqlParameterSource[] args = Arrays.stream(entitys).map(BeanPropertySqlParameterSource::new)
                .toArray(BeanPropertySqlParameterSource[]::new);

        GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
        new NamedParameterJdbcTemplate(jdbcTemplate).batchUpdate(sql, args, generatedKeyHolder);

        List<Map<String, Object>> keyList = generatedKeyHolder.getKeyList();
        for (int i = 0; i < keyList.size(); i++) {
            for (EntityStaff.EntityColumnStaff idColumn : entityStaff.getIdColumns()) {
                Map<String, Object> keyMap = keyList.get(i);
                var val = (BigInteger) keyMap.get("GENERATED_KEY");
                if (val == null) val = (BigInteger) keyMap.get(idColumn.getColumnName());
                if (val != null) {
                    try {
                        idColumn.setFieldValue(entitys[i], val.intValue());
                    } catch (Exception e) {
                        log.error("设置主键错误", e);
                    }
                }
            }
        }
    }

    @Override
    public <E> void toUpdate(JdbcTemplate jdbcTemplate, E entity) {
        EntityStaff entityStaff = EntityStaff.init(entity.getClass());
        String columnStr = entityStaff.getColumns().stream()
                .map(i -> "`%s`=:%s".formatted(i.getColumnName(), i.getFieldName()))
                .collect(Collectors.joining(","));
        String idWhereStr = entityStaff.getIdColumns().stream()
                .map(i -> "`%s`=:%s".formatted(i.getColumnName(), i.getFieldName()))
                .collect(Collectors.joining(","));
        var sql = "UPDATE `%s` SET %s WHERE %s".formatted(entityStaff.getTableName(), columnStr, idWhereStr);

        BeanPropertySqlParameterSource arg = new BeanPropertySqlParameterSource(entity);

        new NamedParameterJdbcTemplate(jdbcTemplate).update(sql, arg);
    }


    @Override
    public <E> E findById(JdbcTemplate jdbcTemplate, Class<E> clazz, Object id) {
        EntityStaff entityStaff = EntityStaff.init(clazz);
        if (entityStaff.getIdColumns().isEmpty()) {
            throw new PersistenceException("实体没有主键");
        }
        if (entityStaff.getIdColumns().size() > 1) {
            throw new PersistenceException("无法查询联合主键的数据");
        }
        String columnStr = entityStaff.getColumns().stream()
                .map(i -> "`%s` as `%s`".formatted(i.getColumnName(), i.getFieldName()))
                .collect(Collectors.joining(","));
        assert entityStaff.getIdColumns().peek() != null;
        String idWhere = "`" + entityStaff.getIdColumns().peek().getColumnName() + "` = ?";
        var sql = "select %s FROM `%s` WHERE %s".formatted(columnStr, entityStaff.getTableName(), idWhere);
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(clazz), id);
    }
}
