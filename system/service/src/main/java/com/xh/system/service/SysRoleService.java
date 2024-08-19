package com.xh.system.service;

import com.xh.common.core.service.BaseServiceImpl;
import com.xh.common.core.service.CommonService;
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import com.xh.system.client.dto.SysRolePermissionDTO;
import com.xh.system.client.entity.SysMenu;
import com.xh.system.client.entity.SysRole;
import com.xh.system.client.entity.SysRoleDataPermission;
import com.xh.system.client.entity.SysRoleMenu;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 系统角色service
 * sunxh 2023/6/3
 */
@Service
@Slf4j
public class SysRoleService extends BaseServiceImpl {

    @Resource
    private CommonService commonService;

    /**
     * 系统角色查询
     */
    @Transactional(readOnly = true)
    public PageResult<SysRole> query(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> param = pageQuery.getParam();
        String sql = "select * from sys_role where deleted = 0 ";
        if (CommonUtil.isNotEmpty(param.get("name"))) {
            sql += " and name like '%' ? '%'";
            pageQuery.addArg(param.get("name"));
        }
        if (CommonUtil.isNotEmpty(param.get("enabled"))) {
            sql += " and enabled = ?";
            pageQuery.addArg(param.get("enabled"));
        }
        pageQuery.setBaseSql(sql);
        return baseJdbcDao.query(SysRole.class, pageQuery);
    }


    @Transactional
    public SysRole save(SysRole sysRole) {
        if (sysRole.getId() == null) {
            baseJdbcDao.insert(sysRole);
        } else {
            //删除之前的权限
            String sql = "delete from sys_role_menu where sys_role_id = ?";
            primaryJdbcTemplate.update(sql, sysRole.getId());
            baseJdbcDao.update(sysRole);
        }
        List<SysRoleMenu> roleMenus = sysRole.getRoleMenus();
        for (SysRoleMenu roleMenu : roleMenus) {
            roleMenu.setSysRoleId(sysRole.getId());
            baseJdbcDao.insert(roleMenu);
        }
        //需要删除子级角色多出的权限（mysql8.0递归查询并删除）
        String sql2 = """
                WITH recursive tb as (
                	SELECT * from sys_role where parent_id = ?
                	UNION ALL
                	SELECT b.* from tb inner join sys_role b on b.parent_id = tb.id
                )
                DELETE FROM sys_role_menu
                WHERE sys_role_id IN ( SELECT id FROM tb )
                    AND sys_menu_id NOT IN (
                     select * from (select sys_menu_id from sys_role_menu where sys_role_id = ?) temp
                    )
                """;
        primaryJdbcTemplate.update(sql2, sysRole.getId(), sysRole.getId());

        //刷新角色权限
        commonService.getRolePermissions(sysRole.getId(), true);
        return sysRole;
    }

    /**
     * id获取角色详情
     */
    @Transactional(readOnly = true)
    public SysRole getById(Serializable id) {
        SysRole role = baseJdbcDao.findById(SysRole.class, id);
        if (role.getParentId() != null) {
            SysRole parentRole = baseJdbcDao.findById(SysRole.class, role.getParentId());
            role.setParentName(parentRole.getName());
        }
        String sql = "select * from sys_role_menu where sys_role_id = ? ";
        List<SysRoleMenu> roleMenus = baseJdbcDao.findList(SysRoleMenu.class, sql, id);
        role.setRoleMenus(roleMenus);
        return role;
    }

    /**
     * ids批量删除角色
     */
    @Transactional
    public void del(List<Serializable> ids) {
        log.info("批量删除角色--");
        String sql = "update sys_role set deleted = 1 where id in (:ids)";
        Map<String, Object> paramMap = new HashMap<>() {{
            put("ids", ids);
        }};
        primaryNPJdbcTemplate.update(sql, paramMap);
    }

    /**
     * 查询角色可配置的所有菜单权限
     */
    @Transactional(readOnly = true)
    public List<SysMenu> queryRoleMenu(Map<String, Object> param) {
        log.info("查询角色可配置的所有菜单权限---");
        if (param == null) param = new HashMap<>();
        List<Object> args = new LinkedList<>();
        String sql = "select * from sys_menu where deleted = 0 ";
        //如果角色隶属于某个角色，那此角色只能维护隶属角色所拥有的角色权限
        if (CommonUtil.isNotEmpty(param.get("parentId"))) {
            sql += " and id in ( select sys_menu_id from sys_role_menu where sys_role_id = ?) ";
            args.add(param.get("parentId"));
        }
        sql += " order by `order` asc";
        return baseJdbcDao.findList(SysMenu.class, sql, args.toArray());
    }

    /**
     * 查询角色的数据权限
     */
    public List<SysRoleDataPermission> queryRoleDataPermission(Map<String, Object> param) {
        String sql = "select * from sys_role_data_permission where deleted = 0 and sys_role_id = ? ";
        return baseJdbcDao.findList(SysRoleDataPermission.class, sql, param.get("sysRoleId"));
    }

    /**
     * 保存角色数据权限
     */
    @Transactional
    public void saveRoleDataPermission(SysRolePermissionDTO sysRolePermissionDTO) {
        String sql = "delete from sys_role_data_permission where sys_role_id = ? ";
        primaryJdbcTemplate.update(sql, sysRolePermissionDTO.getSysRoleId());
        List<SysRoleDataPermission> permissions = sysRolePermissionDTO.getPermissions();
        for (SysRoleDataPermission dataPermission : permissions) {
            dataPermission.setSysRoleId(sysRolePermissionDTO.getSysRoleId());
            baseJdbcDao.insert(dataPermission);
        }
    }
}
