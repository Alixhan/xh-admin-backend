package com.xh.system.service;

import com.xh.common.core.entity.SysLog;
import com.xh.common.core.service.BaseServiceImpl;
import com.xh.common.core.service.CommonService;
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import com.xh.system.client.entity.SysUser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SysLogService extends BaseServiceImpl {

    @Resource
    private CommonService commonService;

    /**
     * 日志查询
     */
    @Transactional(readOnly = true)
    public PageResult<SysLog> query(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> param = pageQuery.getParam();
        String sql = """
                 select a.id, a.token,a.method,a.url,a.ip,a.ip_address,a.tag,a.operation,a.start_time,a.end_time,a.time,a.status,
                 a.locale, a.locale_label, a.sys_org_id, a.org_name, a.sys_role_id, a.role_name, a.create_by, a.create_time, b.name
                 from sys_log a left join sys_user b on b.id = a.create_by where a.deleted is false
                """;
        if (CommonUtil.isNotEmpty(param.get("token"))) {
            sql += " and a.token = ? ";
            pageQuery.addArg(param.get("token"));
        }
        if (CommonUtil.isNotEmpty(param.get("method"))) {
            sql += " and a.method like '%' ? '%'";
            pageQuery.addArg(param.get("method"));
        }
        if (CommonUtil.isNotEmpty(param.get("url"))) {
            sql += " and a.url like '%' ? '%'";
            pageQuery.addArg(param.get("url"));
        }
        if (CommonUtil.isNotEmpty(param.get("ip"))) {
            sql += " and a.ip like '%' ? '%'";
            pageQuery.addArg(param.get("ip"));
        }
        if (CommonUtil.isNotEmpty(param.get("ipAddress"))) {
            sql += " and a.ip_address like '%' ? '%'";
            pageQuery.addArg(param.get("ipAddress"));
        }
        if (CommonUtil.isNotEmpty(param.get("tag"))) {
            sql += " and a.tag like '%' ? '%'";
            pageQuery.addArg(param.get("tag"));
        }
        if (CommonUtil.isNotEmpty(param.get("operation"))) {
            sql += " and a.operation like '%' ? '%'";
            pageQuery.addArg(param.get("operation"));
        }
        if (CommonUtil.isNotEmpty(param.get("status"))) {
            sql += " and a.status = ?";
            pageQuery.addArg(param.get("status"));
        }
        if (CommonUtil.isNotEmpty(param.get("name"))) {
            sql += " and b.name like '%' ? '%'";
            pageQuery.addArg(param.get("name"));
        }

        // 数据权限
        String permissionSql = commonService.getPermissionSql("sys_log", "a.create_by", "a.sys_role_id", "a.sys_org_id");
        if (CommonUtil.isNotEmpty(permissionSql)) {
            sql += " and %s".formatted(permissionSql);
        }

        sql += " order by a.id desc";
        pageQuery.setBaseSql(sql);
        return baseJdbcDao.query(SysLog.class, pageQuery);
    }

    /**
     * id获取日志详情
     */
    @Transactional(readOnly = true)
    public SysLog getById(Serializable id) {
        // 校验数据权限是否满足
        commonService.checkDataPermissionByIds(
                "sys_log", "create_by", "sys_role_id", "sys_org_id",
                SysLog.class, id
        );
        SysLog sysLog = baseJdbcDao.findById(SysLog.class, id);
        if (sysLog.getCreateBy() != null) {
            SysUser sysUser = baseJdbcDao.findById(SysUser.class, sysLog.getCreateBy());
            sysLog.setName(sysUser.getName());
        }
        return sysLog;
    }

    /**
     * ids批量删除日志
     */
    @Transactional
    public void del(List<Integer> ids) {
        log.info("批量删除日志--");
        // 校验数据权限是否满足
        commonService.checkDataPermissionByIds(
                "sys_log", "create_by", "sys_role_id", "sys_org_id",
                SysLog.class, ids.toArray()
        );
        String sql = "update sys_log set deleted = 1 where id in (:ids)";
        Map<String, Object> paramMap = new HashMap<>() {{
            put("ids", ids);
        }};
        primaryNPJdbcTemplate.update(sql, paramMap);
    }
}
