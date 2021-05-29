package com.liujun.trade_ff.service;

import com.liujun.trade_ff.model.UserAccount;

/**
 * 用户相关 service
 * Created by WuShaotong on 2016/8/9.
 */
public interface UserService {



    /**
     * 根据手机号获取用户
     * @param mobile 手机号
     * @return 用户
     */
    UserAccount getUserByMobile(String mobile);
    /**
     * 根据邮箱获取用户
     * @param email 邮箱
     * @return 用户
     */
    UserAccount getUserByEmail(String email);

    /**
     * 校验密码
     * @param account 账号（账号,已验证的手机号,已验证的邮箱）
     * @param password 密码（密码原文）
     * @return 用户
     */
    UserAccount checkPassword(String account, String password);

    /**
     * 重置密码
     * @param userId 用户ID
     * @param password（新密码原文）
     * @return 是否成功
     */
    boolean resetPassword(int userId, String password);

    /**
     * 创建新账户
     * @param userAccount 用户账户
     * @return 账户信息
     */
    UserAccount createUserAccount(UserAccount userAccount);

    /**
     * 根据用户账号获取用户账户
     * @param userAccount 用户账号
     * @return 用户账户信息
     */
    UserAccount getUserByAccount(String userAccount);

}
