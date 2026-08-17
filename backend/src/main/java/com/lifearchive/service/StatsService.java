package com.lifearchive.service;

import java.util.Map;

/**
 * 数据统计服务接口
 */
public interface StatsService {

    /**
     * 获取用户完整数据面板
     * 包含：年度记忆数量、月度活跃度、地点分布、情绪分布、类型分布
     */
    Map<String, Object> getDashboard(Long userId);
}
