package com.liujun.trade_ff.service.impl;

import com.liujun.trade_ff.common.exception.BizException;
import com.liujun.trade_ff.dao.UserAccountMapper;
import com.liujun.trade_ff.model.UserAccount;
import com.liujun.trade_ff.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户相关 service
 * Created by WuShaotong on 2016/8/9.
 */
@Service
@Transactional(readOnly=true)
public class UserServiceImpl implements UserService {
    private static Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    UserAccountMapper userAccountDao;


    @Override
    public UserAccount getUserByMobile(String mobile) {
        UserAccount params = new UserAccount();
        params.setMobile(mobile);
        List<UserAccount> userList = null;
        try {
            userList = userAccountDao.selectByConditions(params);
        }catch (Exception e){
            log.error("getUserByMobile, 异常：", e);
        }
        if(null != userList && userList.size() > 0){
            return userList.get(0);
        }
        return null;
    }


    @Override
    public UserAccount getUserByEmail(String email) {
        UserAccount params = new UserAccount();
        params.setEmail(email);
        List<UserAccount> userList = null;
        try {
            userList = userAccountDao.selectByConditions(params);
        }catch (Exception e){
            log.error("getUserByEmail, 异常：", e);
        }
        if(null != userList && userList.size() > 0){
            return userList.get(0);
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor=Exception.class, readOnly=false)
    public UserAccount checkPassword(String account, String password) {
        UserAccount userAccount = null;

            userAccount = userAccountDao.selectByAccountAndPassword(account, password);

        return userAccount;
    }


    @Override
    @Transactional(rollbackFor=Exception.class, readOnly=false)
    public boolean resetPassword(int userId, String password) {
        throw new BizException("功能没有实现");

    }


    @Override
    @Transactional(rollbackFor=Exception.class, readOnly=false)
    public UserAccount createUserAccount(UserAccount userAccount) {
        throw new BizException("功能还没实现");
    }

    @Override
    public UserAccount getUserByAccount(String userAccount) {
        UserAccount params = new UserAccount();
        params.setUserAccount(userAccount);
        List<UserAccount> userList = null;
        try {
            userList = userAccountDao.selectByConditions(params);
        }catch (Exception e){
            log.error("getUserByAccount, 异常：", e);
        }
        if(null != userList && userList.size() > 0){
            return userList.get(0);
        }
        return null;
    }
}
