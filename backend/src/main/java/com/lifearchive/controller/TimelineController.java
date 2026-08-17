package com.lifearchive.controller;

import com.lifearchive.common.Result;
import com.lifearchive.service.TimelineService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 时间轴控制器
 */
@RestController
@RequestMapping("/api/timeline")
public class TimelineController {

    @Autowired
    private TimelineService timelineService;

    /**
     * 获取用户时间轴（全部年份）
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> timeline = timelineService.getTimeline(userId);
        return Result.success(timeline);
    }

    /**
     * 获取指定年份时间轴
     */
    @GetMapping("/year/{year}")
    public Result<List<Map<String, Object>>> byYear(@PathVariable int year, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> timeline = timelineService.getTimelineByYear(userId, year);
        return Result.success(timeline);
    }
}
