package com.vio.aitukuviobe.application.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.interfaces.dto.user.UserQueryRequest;
import com.vio.aitukuviobe.interfaces.vo.LoginUserVO;
import com.vio.aitukuviobe.interfaces.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户应用服务接口
 */
public interface UserApplicationService extends IService<User> {

    long userRegister(String userAccount, String userPassword, String checkPassword);

    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    String getEncryptPassword(String userPassword);

    User getLoginUser(HttpServletRequest request);

    LoginUserVO getLoginUserVO(User user);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    boolean userLogout(HttpServletRequest request);

    boolean isAdmin(User user);

    com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
