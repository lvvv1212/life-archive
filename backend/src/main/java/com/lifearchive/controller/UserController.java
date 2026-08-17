package com.lifearchive.controller;

import com.lifearchive.common.Result;
import com.lifearchive.entity.User;
import com.lifearchive.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        User user = userService.register(
                body.get("username"),
                body.get("password"),
                body.get("email")
        );
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        return Result.success("注册成功", data);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String token = userService.login(
                body.get("username"),
                body.get("password")
        );
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        return Result.success("登录成功", data);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        return Result.success(user);
    }
}
