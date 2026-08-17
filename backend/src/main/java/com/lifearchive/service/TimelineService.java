package com.lifearchive.service;

import java.util.List;
import java.util.Map;

/**
 * 时间轴服务接口
 */
public interface TimelineService {

    /**
     * 获取用户时间轴（按年份组织）
     * 返回结构：[{ year, events: [{ time, title, description, images, tags, emotion, memoryId }] }]
     */
    List<Map<String, Object>> getTimeline(Long userId);

    /**
     * 获取用户时间轴（按年份筛选）
     */
    List<Map<String, Object>> getTimelineByYear(Long userId, int year);
}
