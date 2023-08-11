package com.xh.system.service;

import com.xh.common.core.dto.SysLoginUserInfoDto;
import com.xh.common.core.dto.SysMenuDTO;
import com.xh.common.core.dto.SysUserDTO;
import com.xh.common.core.service.BaseServiceImpl;
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.utils.WebLogs;
import com.xh.common.core.web.*;
import com.xh.system.client.dto.SysUserJobDTO;
import com.xh.system.client.entity.SysUser;
import com.xh.system.client.entity.SysUserGroup;
import com.xh.system.client.entity.SysUserGroupMember;
import com.xh.system.client.entity.SysUserJob;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SysUserService extends BaseServiceImpl {


    @Value("${sys.auth.authTokenRedisPrefix}")
    private String authTokenRedisPrefix;

    /**
     * 管理端用户登录
     */
    public RestResponse<SysLoginUserInfoDto> login(Map<String, Object> params) {
        String authToken = SysContextHolder.getAuthToken();
        SysLoginUserInfoDto loginUserInfo = null;
        //查看token是否已登录
        if (CommonUtil.isNotEmpty(authToken)) {
            loginUserInfo = getLoginInfoByAuthToken(authToken);
        }
        String username = CommonUtil.getString(params.get("username"));
        String password = CommonUtil.getString(params.get("password"));
        //尝试登录
        if (loginUserInfo == null && CommonUtil.isNotEmpty(username) && CommonUtil.isNotEmpty(password)) {
            String sql = "select * from sys_user where code = ? and enabled = 1";
            SysUser sysUser = baseJdbcDao.findBySql(SysUser.class, sql, username);
            if (sysUser == null) throw new MyException("账号不存在");
            if (!password.equals(sysUser.getPassword())) throw new MyException("密码错误！");

            //生成token，(用户id:uuid)
            authToken = sysUser.getId() + ":" + "sdr28skdf8rsdr287fsdkf0eqd87eisd";

            //查询权限菜单
            String menuSql = "select * from sys_menu where enabled = 1 order by `order` asc";
            List<SysMenuDTO> menus = baseJdbcDao.findList(SysMenuDTO.class, menuSql);

            SysUserDTO sysUserDto = new SysUserDTO();
            BeanUtils.copyProperties(sysUser, sysUserDto);

            loginUserInfo = new SysLoginUserInfoDto();
            loginUserInfo.setUser(sysUserDto);
            loginUserInfo.setMenus(menus);
            loginUserInfo.setToken(authToken);
        }
        //登录成功，将登录的信息缓存
        if (loginUserInfo != null) setLoginInfo(authToken, loginUserInfo);
        return RestResponse.success(loginUserInfo);
    }

    /**
     * 管理端用户注销
     */
    public RestResponse<?> logout() {
        String authToken = SysContextHolder.getAuthToken();
        deleteLoginInfoByAuthToken(authToken);
        return RestResponse.success();
    }

    public void setLoginInfo(String authToken, SysLoginUserInfoDto loginUserInfoDto) {
        redisTemplate.opsForValue().set(authTokenRedisPrefix + authToken, loginUserInfoDto, 60, TimeUnit.MINUTES);
    }

    public SysLoginUserInfoDto getLoginInfoByAuthToken(String authToken) {
        return (SysLoginUserInfoDto) redisTemplate.opsForValue().get(authTokenRedisPrefix + authToken);
    }

    public void deleteLoginInfoByAuthToken(String authToken) {
        redisTemplate.delete(authTokenRedisPrefix + authToken);
    }

    /**
     * 系统用户查询
     */
    @Transactional(readOnly = true)
    public PageResult<SysUser> query(PageQuery<Map<String, Object>> pageQuery) {
        WebLogs.info("用户列表查询---");
        Map<String, Object> param = pageQuery.getParam();
        String sql = "select * from sys_user where deleted = 0 ";
        if (CommonUtil.isNotEmpty(param.get("code"))) {
            sql += " and code like '%' ? '%'";
            pageQuery.addArg(param.get("code"));
        }
        if (CommonUtil.isNotEmpty(param.get("name"))) {
            sql += " and name like '%' ? '%'";
            pageQuery.addArg(param.get("name"));
        }
        if (CommonUtil.isNotEmpty(param.get("enabled"))) {
            sql += " and enabled = ?";
            pageQuery.addArg(param.get("enabled"));
        }
        pageQuery.setBaseSql(sql);
        return baseJdbcDao.query(SysUser.class, pageQuery);
    }

    /**
     * 切换用户字段值
     */
    @Transactional
    public void switchMenuProp(Map<String, Object> param) {
        WebLogs.info("切换用户字段值---", param);
        Object id = param.get("id");
        Object prop = param.get("prop");
        Object value = param.get("value");
        SysUser menu = baseJdbcDao.findById(SysUser.class, (Serializable) id);
        if ("enabled".equals(prop)) menu.setEnabled((Boolean) value);
        else throw new MyException("参数异常，检查后重试！");
        baseJdbcDao.update(menu);
    }

    /**
     * 用户保存
     */
    @Transactional
    public SysUser save(SysUser sysUser) {
        WebLogs.getLogger().info("用户保存---");
        String sql = "select count(1) from sys_user where deleted = 0 and code = ? ";
        if (sysUser.getId() != null) sql += " and id <> " + sysUser.getId();
        Integer count = primaryJdbcTemplate.queryForObject(sql, Integer.class, sysUser.getCode());
        if (count > 0) throw new MyException("用户账号%s已存在！".formatted(sysUser.getCode()));
        if (sysUser.getId() == null) baseJdbcDao.insert(sysUser);
        else baseJdbcDao.update(sysUser);
        return sysUser;
    }

    /**
     * id获取用户详情
     */
    @Transactional(readOnly = true)
    public SysUser getById(Serializable id) {
        return baseJdbcDao.findById(SysUser.class, id);
    }

    /**
     * ids批量删除用户
     */
    @Transactional
    public void del(String ids) {
        String sql = "select * from sys_user where id in (%s)".formatted(ids);
        List<SysUser> list = baseJdbcDao.findList(SysUser.class, sql);
        for (SysUser sysUser : list) {
            sysUser.setDeleted(true);//已删除
            baseJdbcDao.update(sysUser);
        }
    }

    /**
     * 系统用户组查询
     */
    @Transactional(readOnly = true)
    public PageResult<SysUserGroup> queryUserGroupList(PageQuery<Map<String, Object>> pageQuery) {
        WebLogs.info("用户组列表查询---");
        Map<String, Object> param = pageQuery.getParam();
        String sql = "select * from sys_user_group where deleted = 0 ";
        if (CommonUtil.isNotEmpty(param.get("code"))) {
            sql += " and code like '%' ? '%'";
            pageQuery.addArg(param.get("code"));
        }
        if (CommonUtil.isNotEmpty(param.get("name"))) {
            sql += " and name like '%' ? '%'";
            pageQuery.addArg(param.get("name"));
        }
        pageQuery.setBaseSql(sql);
        return baseJdbcDao.query(SysUserGroup.class, pageQuery);
    }


    /**
     * 用户组保存
     */
    @Transactional
    public SysUserGroup saveUserGroup(SysUserGroup sysUserGroup) {
        WebLogs.getLogger().info("用户组保存---");

        String sql = "select count(1) from sys_user_group where deleted = 0 and name = ? ";
        if (sysUserGroup.getId() != null) sql += " and id <> " + sysUserGroup.getId();
        Integer count = primaryJdbcTemplate.queryForObject(sql, Integer.class, sysUserGroup.getName());
        if (count > 0) throw new MyException("用户组%s已存在，不能新增重复的用户组！".formatted(sysUserGroup.getName()));

        if (sysUserGroup.getId() == null) baseJdbcDao.insert(sysUserGroup);
        else {
            baseJdbcDao.update(sysUserGroup);

            //修改保存话，先删除原本的用户组成员重新新增
            String sql1 = "delete from sys_user_group_member where sys_user_group_id = ? ";
            primaryJdbcTemplate.update(sql1, sysUserGroup.getId());
        }

        //保存用户组岗位
        saveUserJobs(new SysUserJobDTO() {{
            setType(2);
            setUserId(sysUserGroup.getId());
            setJobData(sysUserGroup.getJobData());
        }});

        //保存用户组成员
        List<SysUserGroupMember> memberData = sysUserGroup.getMemberData();
        for (SysUserGroupMember member : memberData) {
            member.setSysUserGroupId(sysUserGroup.getId());
            baseJdbcDao.insert(member);
        }
        return sysUserGroup;
    }

    /**
     * id获取用户组详情
     */
    @Transactional(readOnly = true)
    public SysUserGroup getUserGroupById(Serializable id) {
        SysUserGroup userGroup = baseJdbcDao.findById(SysUserGroup.class, id);
        //查询用户组成员
        String sql = """
            select 
                a.*, b.code user_code, b.name user_name 
            from sys_user_group_member a
            left join sys_user b on b.id = a.sys_user_id
            where a.sys_user_group_id = ?
        """;
        List<SysUserGroupMember> memberData = baseJdbcDao.findList(SysUserGroupMember.class, sql, id);
        userGroup.setMemberData(memberData);
        return userGroup;
    }

    /**
     * ids批量删除用户组
     */
    @Transactional
    public void delUserGroup(String ids) {
        String sql = "select * from sys_user_group where id in (%s)".formatted(ids);
        List<SysUserGroup> list = baseJdbcDao.findList(SysUserGroup.class, sql);
        for (SysUserGroup sysUserGroup : list) {
            sysUserGroup.setDeleted(true);//已删除
            baseJdbcDao.update(sysUserGroup);
        }
    }

    /**
     * 获取用户或者用户组的岗位信息
     */
    @Transactional(readOnly = true)
    public List<SysUserJob> getUserJobs(Map<String, Object> param) {
        List<Object> args = new ArrayList<>();
        args.add(param.get("userId"));
        String sql = """
                    select
                        a.*,
                        b.name orgName,
                        c.name roleName
                    from sys_user_job a
                    left join sys_org b on b.id = a.sys_org_id
                    left join sys_role c on c.id = a.sys_role_id
                    where a.user_id = ?
                """;
        Object type = param.get("type");
        if (CommonUtil.isNotEmpty(type)) {
            sql += " and a.type = ? ";
            args.add(type);
        }
        return baseJdbcDao.findList(SysUserJob.class, sql, args.toArray());
    }

    /**
     * 用户或用户组岗位保存
     */
    @Transactional
    public void saveUserJobs(SysUserJobDTO sysUserJobDTO) {
        String sql1 = "delete from sys_user_job where user_id = ? and type = ?";
        primaryJdbcTemplate.update(sql1, sysUserJobDTO.getUserId(), sysUserJobDTO.getType());
        List<SysUserJob> sysUserJobs = sysUserJobDTO.getJobData();
        for (SysUserJob sysUserJob : sysUserJobs) {
            sysUserJob.setType(sysUserJobDTO.getType());
            sysUserJob.setUserId(sysUserJobDTO.getUserId());
            baseJdbcDao.insert(sysUserJob);
        }
    }

    /**
     * 获取用户所在的所有用户组信息
     */
    @Transactional(readOnly = true)
    public List<SysUserGroup> getUserGroups(Serializable userId) {
        String sql = """
                    select
                        b.*
                    from sys_user_group_member a
                    left join sys_user_group b on b.id = a.sys_user_group_id
                    where a.sys_user_id = ?
                """;
        return baseJdbcDao.findList(SysUserGroup.class, sql, userId);
    }
}
