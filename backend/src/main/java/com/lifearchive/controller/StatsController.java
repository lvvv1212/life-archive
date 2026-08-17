package com.lifearchive.controller;

import com.lifearchive.common.Result;
import com.lifearchive.service.StatsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据统计控制器
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    /**
     * 获取用户数据面板
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> data = statsService.getDashboard(userId);
        return Result.success(data);
    }
}
