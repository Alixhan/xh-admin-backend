package com.xh.system.service;

import com.xh.common.core.service.BaseServiceImpl;
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import com.xh.system.client.entity.SysDataEntity;
import com.xh.system.client.entity.SysDataPermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统数据权限service
 * sunxh 2023/8/19
 */
@Service
@Slf4j
public class SysDataPermissionService extends BaseServiceImpl {
    /**
     * 系统数据权限查询
     */
    @Transactional(readOnly = true)
    public List<SysDataEntity> queryEntity() {
        String sql = "select id, name from sys_data_entity";
        return baseJdbcDao.findList(SysDataEntity.class, sql);
    }

    /**
     * 系统数据权限查询
     */
    @Transactional(readOnly = true)
    public PageResult<SysDataPermission> query(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> param = pageQuery.getParam();
        String sql = "select id,name,expression,create_time,update_time,create_by,update_by from sys_data_permission where deleted = 0 ";
        if (CommonUtil.isNotEmpty(param.get("name"))) {
            sql += " and name like '%' ? '%'";
            pageQuery.addArg(param.get("name"));
        }
        pageQuery.setBaseSql(sql);
        return baseJdbcDao.query(SysDataPermission.class, pageQuery);
    }


    @Transactional
    public SysDataPermission save(SysDataPermission sysDataPermission) {
        if (sysDataPermission.getId() == null) {
            baseJdbcDao.insert(sysDataPermission);
        } else {
            baseJdbcDao.update(sysDataPermission);
        }
        return sysDataPermission;
    }

    /**
     * id获取数据权限详情
     */
    @Transactional(readOnly = true)
    public SysDataPermission getById(Serializable id) {
        return baseJdbcDao.findById(SysDataPermission.class, id);
    }

    /**
     * ids批量删除数据权限
     */
    @Transactional
    public void del(List<Serializable> ids) {
        log.info("批量删除数据权限--");
        String sql = "update sys_data_permission set deleted = 1 where id in (:ids)";
        Map<String, Object> paramMap = new HashMap<>() {{
            put("ids", ids);
        }};
        primaryNPJdbcTemplate.update(sql, paramMap);
    }
}
