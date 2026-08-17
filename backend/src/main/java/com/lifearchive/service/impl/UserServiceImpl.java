package com.lifearchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifearchive.common.JwtUtils;
import com.lifearchive.entity.User;
import com.lifearchive.mapper.UserMapper;
import com.lifearchive.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public User register(String username, String password, String email) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (email != null && !email.isEmpty()) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, email);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("邮箱已被注册");
            }
        }

        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(md5Password(password));
        user.setEmail(email);
        user.setNickname(username);

        userMapper.insert(user);
        // 不返回密码
        user.setPassword(null);
        return user;
    }

    @Override
    public String login(String username, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);

        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!user.getPassword().equals(md5Password(password))) {
            throw new RuntimeException("用户名或密码错误");
        }

        return jwtUtils.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    /**
     * MD5 密码加密（生产环境建议使用 BCrypt）
     */
    private String md5Password(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
}
