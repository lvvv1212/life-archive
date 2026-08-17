package com.lifearchive.service;

import com.lifearchive.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    User register(String username, String password, String email);

    /**
     * 用户登录，返回 JWT Token
     */
    String login(String username, String password);

    /**
     * 根据 ID 获取用户信息
     */
    User getUserById(Long userId);

    /**
     * 根据用户名查询用户
     */
    User getByUsername(String username);
}
