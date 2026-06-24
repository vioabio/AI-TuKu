package com.vio.aitukuviobe.generator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vio.aitukuviobe.generator.domain.User;
import com.vio.aitukuviobe.generator.service.UserService;
import com.vio.aitukuviobe.generator.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author vivin
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2026-06-23 19:48:25
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}