package com.vio.aitukuviobe.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.interfaces.dto.user.UserQueryRequest;
import com.vio.aitukuviobe.interfaces.vo.LoginUserVO;
import com.vio.aitukuviobe.interfaces.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface UserDomainService {
    long userRegister(String userAccount, String userPassword, String checkPassword);
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);
    String getEncryptPassword(String userPassword);
    User getLoginUser(HttpServletRequest request);
    LoginUserVO getLoginUserVO(User user);
    UserVO getUserVO(User user);
    List<UserVO> getUserVOList(List<User> userList);
    boolean userLogout(HttpServletRequest request);
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
    boolean isAdmin(User user);
}
