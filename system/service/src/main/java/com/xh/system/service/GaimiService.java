package com.xh.system.service;

import com.xh.common.core.dto.SysLoginUserInfoDTO;
import com.xh.common.core.dto.SysUserDTO;
import com.xh.common.core.service.BaseServiceImpl;
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.utils.LoginUtil;
import com.xh.common.core.web.MyException;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import com.xh.system.client.entity.GaimiAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GaimiService extends BaseServiceImpl {

    /**
     * 改密账号查询
     */
    @Transactional(readOnly = true)
    public PageResult<GaimiAccount> query(PageQuery<Map<String, Object>> pageQuery) {
        SysLoginUserInfoDTO sysUserInfo = LoginUtil.getSysUserInfo();
        Map<String, Object> param = pageQuery.getParam();
        String sql = "select * from gaimi_account where deleted = 0 and create_by = ? ";
        pageQuery.addArg(sysUserInfo.getUser().getId());
        if (CommonUtil.isNotEmpty(param.get("username"))) {
            sql += " and username like '%' ? '%'";
            pageQuery.addArg(param.get("username"));
        }
        if (CommonUtil.isNotEmpty(param.get("password"))) {
            sql += " and password like '%' ? '%'";
            pageQuery.addArg(param.get("password"));
        }
        if (CommonUtil.isNotEmpty(param.get("remark"))) {
            sql += " and remark like '%' ? '%'";
            pageQuery.addArg(param.get("remark"));
        }
        pageQuery.setBaseSql(sql);
        return baseJdbcDao.query(GaimiAccount.class, pageQuery);
    }

    /**
     * 切换改密账号字段值
     */
    @Transactional
    public void switchProp(Map<String, Object> param) {
        SysUserDTO user = LoginUtil.getSysUserInfo().getUser();
        Object id = param.get("id");
        Object username = param.get("username");
        Object prop = param.get("prop");
        Object value = param.get("value");
        GaimiAccount gaimiAccount = null;
        if (CommonUtil.isNotEmpty(id)) {
            gaimiAccount = baseJdbcDao.findById(GaimiAccount.class, (Serializable) id);
        } else if (CommonUtil.isNotEmpty(username)) {
            gaimiAccount = baseJdbcDao.findBySql(GaimiAccount.class,
                    "select * from gaimi_account where create_by = ? and username = ? and deleted = 0", user.getId(), username);
        }
        if (gaimiAccount != null) {
            if ("password".equals(prop)) gaimiAccount.setPassword((String) value);
            else throw new MyException("参数异常，检查后重试！");
            baseJdbcDao.update(gaimiAccount);
        } else {
            throw new MyException("改密失败，账号不存在");
        }

    }

    /**
     * 改密账号保存
     */
    @Transactional
    public GaimiAccount save(GaimiAccount gaimiAccoun) {
        SysLoginUserInfoDTO sysUserInfo = LoginUtil.getSysUserInfo();

        String sql = "select count(1) from gaimi_account where deleted = 0 and create_by = ? and username = ? ";
        if (gaimiAccoun.getId() != null) sql += " and id <> " + gaimiAccoun.getId();
        Integer count = primaryJdbcTemplate.queryForObject(sql, Integer.class, sysUserInfo.getUser().getId(), gaimiAccoun.getUsername());
        assert count != null;
        if (count > 0) throw new MyException("账号%s已存在！".formatted(gaimiAccoun.getUsername()));

        if (gaimiAccoun.getId() == null) {
            baseJdbcDao.insert(gaimiAccoun);
        } else {
            baseJdbcDao.update(gaimiAccoun);
        }
        return gaimiAccoun;
    }

    /**
     * id获取改密账号详情
     */
    @Transactional(readOnly = true)
    public GaimiAccount getById(Serializable id) {
        return baseJdbcDao.findById(GaimiAccount.class, id);
    }

    /**
     * username获取改密账号详情
     */
    @Transactional(readOnly = true)
    public GaimiAccount getByUsername(String username) {
        String sql = "select * from gaimi_account where username = ? and deleted = 0";
        return baseJdbcDao.findBySql(GaimiAccount.class, sql,username);
    }

    /**
     * ids批量删除改密账号
     */
    @Transactional
    public void del(List<Serializable> ids) {
        log.info("批量删除改密账号--");
        String sql = "update gaimi_account set deleted = 1 where id in (:ids)";
        Map<String, Object> paramMap = new HashMap<>() {{
            put("ids", ids);
        }};
        primaryNPJdbcTemplate.update(sql, paramMap);
    }

    /**
     * 改密账号批量导入
     */
    @Transactional
    public ArrayList<Map<String, Object>> imports(List<GaimiAccount> sysUsers) {
        ArrayList<Map<String, Object>> res = new ArrayList<>();
        for (int i = 0; i < sysUsers.size(); i++) {
            try {
                GaimiAccount gaimiAccoun = sysUsers.get(i);
//                gaimiAccoun.setEnabled(true);
                save(gaimiAccoun);
            } catch (MyException e) {
                Map<String, Object> resMap = new HashMap<>();
                resMap.put("num", i + 1);
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
}
