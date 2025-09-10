package com.xh.common.core.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.xh.common.core.Constant;
import com.xh.common.core.dao.sql.EntityStaff;
import com.xh.common.core.dto.DataPermissionsCheckDTO;
import com.xh.common.core.dto.OnlineUserDTO;
import com.xh.common.core.dto.RolePermissionsDTO;
import com.xh.common.core.dto.SysMenuDTO;
import com.xh.common.core.entity.SysLog;
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.utils.LoginUtil;
import com.xh.common.core.web.MyException;
import com.xh.common.core.web.NoDataPermissionException;
import com.xh.common.core.web.RestResponse;
import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公用service
 * sunxh 2024/8/20
 */
@Service
@Slf4j
public class CommonService extends BaseServiceImpl {

    /**
     * 获取角色拥有的权限集合
     */
    public List<SysMenuDTO> getRolePermissions(Integer roleId, Boolean refresh) {
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();

        if (!refresh) {
            RolePermissionsDTO rolePermissions = (RolePermissionsDTO) operations.get(Constant.ROLE_PERMISSIONS_PREFIX + roleId);
            if (rolePermissions != null) {
                return rolePermissions.getPermissions();
            }
        }

        String sql2 = """
                    select
                         a.sys_role_id role_id,b.*
                    from sys_role_menu a
                    left join sys_menu b on a.sys_menu_id = b.id
                    where
                        a.deleted is false
                        and b.deleted is false
                        and b.enabled is true
                        and a.sys_role_id = ?
                    order by b.`order` asc
                """;
        //查询角色拥有的所有菜单权限
        List<SysMenuDTO> menus = baseJdbcDao.findList(SysMenuDTO.class, sql2, roleId);

        RolePermissionsDTO rolePermissions = new RolePermissionsDTO();
        rolePermissions.setPermissions(menus);
        rolePermissions.setRoleId(roleId);
        rolePermissions.setCreateTime(LocalDateTime.now());
        operations.set(Constant.ROLE_PERMISSIONS_PREFIX + roleId, rolePermissions);

        return menus;
    }

    /**
     * 保存日志
     */
    @Transactional
    public void saveSysLog(SysLog sysLog) {
        sysLog.setToken(StpUtil.getTokenValue());
        sysLog.setEndTime(LocalDateTime.now());
        sysLog.setTime(ChronoUnit.MILLIS.between(sysLog.getStartTime(), sysLog.getEndTime()));
        try {
            if (StpUtil.isLogin()) {
                OnlineUserDTO onlineUserDTO = LoginUtil.getOnlineUserInfo();
                sysLog.setLocale(onlineUserDTO.getLocale());
                sysLog.setLocaleLabel(onlineUserDTO.getLocaleLabel());
                sysLog.setSysOrgId(onlineUserDTO.getOrgId());
                sysLog.setSysRoleId(onlineUserDTO.getRoleId());
                sysLog.setOrgName(onlineUserDTO.getOrgName());
                sysLog.setRoleName(onlineUserDTO.getRoleName());
                BeanUtils.copyProperties(onlineUserDTO, sysLog);
            }

            String responseBody = sysLog.getResponseBody();
            if (CommonUtil.isNotEmpty(responseBody)) {
                RestResponse<?> restResponse = JSON.parseObject(responseBody, RestResponse.class);
                sysLog.setStatus(restResponse.getStatus());
            }
        } catch (Exception e) {
            log.error("存储日志异常", e);
        }
        baseJdbcDao.insert(sysLog);
    }

    /**
     * 获取数据权限条件sql
     *
     * @param sysDataEntityId 数据实体ID
     * @param userColumn      用户id字段名
     * @param roleColumn      角色id字段名
     * @param orgColumn       机构id字段名
     */
    public String getPermissionSql(String sysDataEntityId, String userColumn, String roleColumn, String orgColumn) {
        OnlineUserDTO onlineUserDTO = LoginUtil.getOnlineUserInfo();
        if (onlineUserDTO != null) {
            var sql = """
                    SELECT min( b.expression ) as expression
                    FROM sys_role_data_permission a
                    LEFT JOIN sys_data_permission b ON a.sys_data_permission_id = b.id
                    WHERE a.sys_role_id = ? and a.sys_data_entity_id = ?
                    """;
            String expression = primaryJdbcTemplate.queryForObject(sql, String.class, onlineUserDTO.getRoleId(), sysDataEntityId);
            if (CommonUtil.isNotEmpty(expression)) {
                String regex = "(\\^)?(\\$[A-Z]+)(\\(([^)]+)\\))?";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(expression);
                StringBuilder permissionSql = new StringBuilder();
                while (matcher.find()) {
                    // negative
                    boolean negative = CommonUtil.isNotEmpty(matcher.group(1));
                    // columnType
                    String columnType = matcher.group(2);
                    // 指定的id (带括号)
                    String idStr = matcher.group(3);
                    // 指定的id
                    String ids = matcher.group(4);
                    if (ids != null && ids.split(",").length > 1) {
                        ids = idStr;
                    }
                    var condition = "";
                    if ((columnType.equals("$BR") || columnType.equals("$ZDYH")) && userColumn == null) {
                        condition = "true";
                    }
                    if ((columnType.equals("$BJG") || columnType.equals("$BJGX") || columnType.equals("$ZDJG")) && orgColumn == null) {
                        condition = "true";
                    }
                    if ((columnType.equals("$DQJS") || columnType.equals("$DQJSX") || columnType.equals("$ZDJS")) && roleColumn == null) {
                        condition = "true";
                    }
                    if (!condition.equals("true")) {
                        condition = switch (columnType) {
                            //  本人
                            case "$BR" ->
                                    "%s %s %s".formatted(userColumn, negative ? "<>" : "=", onlineUserDTO.getUserId());
                            //  本机构
                            case "$BJG" ->
                                    "%s %s %s".formatted(orgColumn, negative ? "<>" : "=", onlineUserDTO.getOrgId());
                            //  本机构下属机构
                            case "$BJGX" -> """
                                    %s %s (
                                        WITH recursive tb as (
                                            SELECT id from sys_org where deleted is false and parent_id = %s
                                            UNION
                                            SELECT b.id from tb inner join sys_org b on b.parent_id = tb.id
                                        )
                                        select id from tb
                                    )
                                    """.formatted(orgColumn, negative ? "not in" : "in", onlineUserDTO.getOrgId());
                            //  当前角色
                            case "$DQJS" ->
                                    "%s %s %s".formatted(roleColumn, negative ? "<>" : "=", onlineUserDTO.getRoleId());
                            //  当前角色下属角色
                            case "$DQJSX" -> """
                                    %s %s (
                                        WITH recursive tb as (
                                            SELECT id from sys_role where deleted is false and parent_id = %s
                                            UNION
                                            SELECT b.id from tb inner join sys_role b on b.parent_id = tb.id
                                        )
                                        select id from tb
                                    )
                                    """.formatted(roleColumn, negative ? "not in" : "in", onlineUserDTO.getRoleId());
                            // 指定机构
                            case "$ZDJG" -> {
                                var place = negative ? "<>" : "=";
                                if (Objects.equals(ids, idStr)) {
                                    place = negative ? "not in" : "in";
                                }
                                yield "%s %s %s".formatted(orgColumn, place, ids);
                            }
                            //  指定角色
                            case "$ZDJS" -> {
                                var place = negative ? "<>" : "=";
                                if (Objects.equals(ids, idStr)) {
                                    place = negative ? "not in" : "in";
                                }
                                yield "%s %s %s".formatted(roleColumn, place, ids);
                            }
                            //  指定用户
                            case "$ZDYH" -> {
                                var place = negative ? "<>" : "=";
                                if (Objects.equals(ids, idStr)) {
                                    place = negative ? "not in" : "in";
                                }
                                yield "%s %s %s".formatted(userColumn, place, ids);
                            }
                            default -> "true";
                        };
                    }
                    matcher.appendReplacement(permissionSql, condition);
                }
                matcher.appendTail(permissionSql);
                return "(%s)".formatted(permissionSql);
            }
            // 权限表达式为空则说明是所有权限
            return "";
        }
        //如果角色未设置数据权限，抛出业务异常提醒维护角色数据权限
        throw new MyException("当前角色未设置数据权限，无法访问数据，请联系管理员处理！");
    }


    /**
     * 验证那些直接使用ID直接访问的数据是否在数据权限范围内
     * 如无数据权限则会抛出异常
     *
     * @param sysDataEntityId 数据实体ID
     * @param userColumn      用户id字段名
     * @param roleColumn      角色id字段名
     * @param orgColumn       机构id字段名
     * @param clazz           实体类型
     * @param ids             主键
     * @throws NoDataPermissionException 无数据权限异常
     */
    public <K> void checkDataPermissionByIds(
            String sysDataEntityId,
            String userColumn,
            String roleColumn,
            String orgColumn,
            Class<K> clazz,
            Object... ids
    ) throws NoDataPermissionException {
        EntityStaff entityStaff = EntityStaff.init(clazz);
        if (entityStaff.getIdColumns().isEmpty()) {
            throw new PersistenceException("实体没有主键");
        }
        assert entityStaff.getIdColumns().peek() != null;
        String idColumn = "`" + entityStaff.getIdColumns().peek().getColumnName() + "`";

        String permissionSql = getPermissionSql(entityStaff.getTableName(), userColumn, roleColumn, orgColumn);
        if (CommonUtil.isEmpty(permissionSql)) return; //无权限控制直接返回

        var sql = "select %s as `result`, %s as `id` FROM `%s` WHERE %s in (:ids)".formatted(
                permissionSql,
                idColumn,
                entityStaff.getTableName(),
                idColumn
        );
        //转化一下sql
        sql = baseJdbcDao.getSqlExecutor().convertSql(sql);

        Map<String, Object> paramMap = new HashMap<>() {{
            put("ids", List.of(ids));
        }};
        var res = primaryNPJdbcTemplate.query(sql, paramMap, new BeanPropertyRowMapper<>(DataPermissionsCheckDTO.class));
        for (DataPermissionsCheckDTO item : res) {
            if (item.getResult() != Boolean.TRUE) {
                throw new NoDataPermissionException(sysDataEntityId, sql, JSON.toJSONString(paramMap));
            }
        }
    }
}
