package com.mwc.exam.service.impl;

import com.mwc.exam.entity.User;
import com.mwc.exam.mapper.UserMapper;
import com.mwc.exam.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

/**
 * 用户Service实现类
 * 实现用户相关的业务逻辑
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

} 