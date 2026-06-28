package com.vio.aitukuviobe.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vio.aitukuviobe.application.service.UserApplicationService;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.service.UserDomainService;
import com.vio.aitukuviobe.infrastructure.mapper.UserMapper;
import com.vio.aitukuviobe.interfaces.dto.user.UserQueryRequest;
import com.vio.aitukuviobe.interfaces.vo.LoginUserVO;
import com.vio.aitukuviobe.interfaces.vo.UserVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户应用服务实现
 */
@Service
public class UserApplicationServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserApplicationService {

    @Resource
    private UserDomainService userDomainService;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        return userDomainService.userRegister(userAccount, userPassword, checkPassword);
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        return userDomainService.userLogin(userAccount, userPassword, request);
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        return userDomainService.getEncryptPassword(userPassword);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        return userDomainService.getLoginUser(request);
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        return userDomainService.getLoginUserVO(user);
    }

    @Override
    public UserVO getUserVO(User user) {
        return userDomainService.getUserVO(user);
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        return userDomainService.getUserVOList(userList);
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        return userDomainService.userLogout(request);
    }

    @Override
    public boolean isAdmin(User user) {
        return userDomainService.isAdmin(user);
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        return userDomainService.getQueryWrapper(userQueryRequest);
    }
}
