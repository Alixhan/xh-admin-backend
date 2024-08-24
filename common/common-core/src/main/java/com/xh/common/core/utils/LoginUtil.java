package com.xh.common.core.utils;


import cn.dev33.satoken.stp.StpUtil;
import com.xh.common.core.dto.OnlineUserDTO;
import com.xh.common.core.dto.SysLoginUserInfoDTO;
import com.xh.common.core.dto.WxUserInfoDTO;

/**
 * 获取当前登录用户
 */
public class LoginUtil {
    public final static String SYS_USER_KEY = "sysUserInfo";
    public final static String WX_USER_KEY = "wxUserInfo";

    /**
     * 获取当前系统登录信息
     */
    public static SysLoginUserInfoDTO getSysUserInfo() {
        return StpUtil.getSession().getModel(SYS_USER_KEY, SysLoginUserInfoDTO.class);
    }

    /**
     * 获取当前登录用户详情
     */
    public static OnlineUserDTO getOnlineUserInfo() {
        return StpUtil.getTokenSession().getModel(SYS_USER_KEY, OnlineUserDTO.class);
    }

    /**
     * 获取当前微信登录用户信息
     */
    public static WxUserInfoDTO getWxUserInfo() {
        return StpUtil.getTokenSession().getModel(WX_USER_KEY, WxUserInfoDTO.class);
    }
}
