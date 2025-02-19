package com.xh.system.service;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.xh.common.core.dto.SysLoginUserInfoDTO;
import com.xh.common.core.dto.SysUserDTO;
import com.xh.common.core.service.BaseServiceImpl;
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.utils.LoginUtil;
import com.xh.common.core.web.MyException;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import com.xh.system.client.dto.SysUserJobDTO;
import com.xh.system.client.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.io.Serializable;
import java.util.*;

@Service
@Slf4j
public class SysUserService extends BaseServiceImpl {

    /**
     * 系统用户查询
     */
    @Transactional(readOnly = true)
    public PageResult<SysUser> query(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> param = pageQuery.getParam();
        String sql = "select * from sys_user where deleted is false ";
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
        Object id = param.get("id");
        Object prop = param.get("prop");
        Object value = param.get("value");
        SysUser menu = baseJdbcDao.findById(SysUser.class, (Serializable) id);
        if ("enabled".equals(prop)) menu.setEnabled((Boolean) value);
        else throw new MyException("参数异常，检查后重试！");
        baseJdbcDao.update(menu);
    }

    /**
     * 个人中心用户保存
     */
    @Transactional
    public SysUser personalCenterSave(SysUser sysUser) {
        if (!Objects.equals(CommonUtil.getString(sysUser.getId()), StpUtil.getLoginId())) {
            throw new MyException("非法请求！");
        }
        SysUser sysUser1 = baseJdbcDao.findById(SysUser.class, sysUser.getId());
        sysUser1.setName(sysUser.getName());
        sysUser1.setPassword(sysUser.getPassword());
        sysUser1.setAvatar(sysUser.getAvatar());
        sysUser1.setTelephone(sysUser.getTelephone());
        save(sysUser1);

        //刷新一下
        SaSession session = StpUtil.getSession();
        SysLoginUserInfoDTO userInfoDTO = session.getModel(LoginUtil.SYS_USER_KEY, SysLoginUserInfoDTO.class);
        SysUserDTO sysUserDTO = new SysUserDTO();
        BeanUtils.copyProperties(sysUser1, sysUserDTO);
        userInfoDTO.setUser(sysUserDTO);
        session.set(LoginUtil.SYS_USER_KEY, userInfoDTO);
        return sysUser1;
    }

    /**
     * 用户保存
     */
    @Transactional
    public SysUser save(SysUser sysUser) {
        String sql = "select count(1) from sys_user where deleted is false and code = ? ";
        if (sysUser.getId() != null) sql += " and id <> " + sysUser.getId();
        Integer count = primaryJdbcTemplate.queryForObject(sql, Integer.class, sysUser.getCode());
        assert count != null;
        if (count > 0) throw new MyException("用户账号%s已存在！".formatted(sysUser.getCode()));
        if (CommonUtil.isNotEmpty(sysUser.getPassword())) {
            // 密码加密
            String pwHash = BCrypt.hashpw(sysUser.getPassword(), BCrypt.gensalt());
            sysUser.setPassword(pwHash);
        }
        if (sysUser.getId() == null) {
            sysUser.setStatus(1);
            baseJdbcDao.insert(sysUser);
        } else {
            SysUser oldUser = baseJdbcDao.findById(SysUser.class, sysUser.getId());
            if (CommonUtil.isNotEmpty(sysUser.getPassword())) {
                if (oldUser.getPassword().equals(sysUser.getPassword()))
                    throw new MyException("新密码不能与旧密码相同！");
                //记录密码修改日志
                SysUserPasswordLog passwordLog = new SysUserPasswordLog();
                passwordLog.setSysUserId(sysUser.getId());
                passwordLog.setOldPassword(oldUser.getPassword());
                passwordLog.setOldPassword(sysUser.getPassword());
                baseJdbcDao.insert(passwordLog);
            } else {
                sysUser.setPassword(oldUser.getPassword());
            }
            baseJdbcDao.update(sysUser);
        }
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
    public void del(List<Integer> ids) {
        log.info("批量删除用户--");
        String sql = "update sys_user set deleted = 1 where id in (:ids)";
        Map<String, Object> paramMap = new HashMap<>() {{
            put("ids", ids);
        }};
        primaryNPJdbcTemplate.update(sql, paramMap);
    }

    /**
     * 用户批量导入
     */
    @Transactional
    public ArrayList<Map<String, Object>> imports(List<SysUser> sysUsers) {
        ArrayList<Map<String, Object>> res = new ArrayList<>();
        for (int i = 0; i < sysUsers.size(); i++) {
            try {
                SysUser sysUser = sysUsers.get(i);
                sysUser.setEnabled(true);
                save(sysUser);
            } catch (MyException e) {
                Map<String, Object> resMap = new HashMap<>();
                resMap.put("rowIndex", i);
                resMap.put("error", e.getMessage());
                res.add(resMap);
            }
        }
        //有错误信息直接手动回滚整个事务
        if (!res.isEmpty()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return res;
        }
        return null;
    }

    /**
     * 密码重置
     */
    @Transactional
    public void resetPassword(SysUser sysUser) {
        SysUser user = baseJdbcDao.findById(SysUser.class, sysUser.getId());
        // 密码加密
        String pwHash = BCrypt.hashpw(sysUser.getPassword(), BCrypt.gensalt());
        user.setPassword(pwHash);
        baseJdbcDao.update(user);
    }

    /**
     * 系统用户组查询
     */
    @Transactional(readOnly = true)
    public PageResult<SysUserGroup> queryUserGroupList(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> param = pageQuery.getParam();
        String sql = "select * from sys_user_group a where a.deleted is false ";
        if (CommonUtil.isNotEmpty(param.get("code"))) {
            sql += " and a.code like '%' ? '%'";
            pageQuery.addArg(param.get("code"));
        }
        if (CommonUtil.isNotEmpty(param.get("name"))) {
            sql += " and a.name like '%' ? '%'";
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
        String sql = "select count(1) from sys_user_group where deleted is false and name = ? ";
        if (sysUserGroup.getId() != null) sql += " and id <> " + sysUserGroup.getId();
        Integer count = primaryJdbcTemplate.queryForObject(sql, Integer.class, sysUserGroup.getName());
        assert count != null;
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
    public void delUserGroup(List<Integer> ids) {
        log.info("ids批量删除用户组--");
        String sql = "update sys_user_group set deleted = 1 where id in (:ids)";
        Map<String, Object> paramMap = new HashMap<>() {{
            put("ids", ids);
        }};
        primaryNPJdbcTemplate.update(sql, paramMap);
    }

    /**
     * 获取用户或者用户组的岗位信息
     */
    @Transactional(readOnly = true)
    public List<SysUserJob> getUserJobs(SysUserJob param) {
        List<Object> args = new ArrayList<>();
        args.add(param.getUserId());
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
        Object type = param.getType();
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
