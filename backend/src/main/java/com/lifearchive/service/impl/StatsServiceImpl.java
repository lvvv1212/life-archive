package com.lifearchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifearchive.entity.Memory;
import com.lifearchive.mapper.MemoryMapper;
import com.lifearchive.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计服务实现
 */
@Service
public class StatsServiceImpl implements StatsService {

    @Autowired
    private MemoryMapper memoryMapper;

    @Override
    public Map<String, Object> getDashboard(Long userId) {
        List<Memory> all = memoryMapper.selectList(
                new LambdaQueryWrapper<Memory>().eq(Memory::getUserId, userId)
        );

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("totalMemories", all.size());
        dashboard.put("yearlyStats", buildYearlyStats(all));
        dashboard.put("monthlyStats", buildMonthlyStats(all));
        dashboard.put("locationStats", buildLocationStats(all));
        dashboard.put("emotionStats", buildEmotionStats(all));
        dashboard.put("typeStats", buildTypeStats(all));
        dashboard.put("recentMemories", buildRecentMemories(all));

        return dashboard;
    }

    // ========== 年度记忆数量 ==========
    private List<Map<String, Object>> buildYearlyStats(List<Memory> memories) {
        Map<Integer, Long> yearCounts = new LinkedHashMap<>();
        for (Memory m : memories) {
            int year = getYear(m);
            yearCounts.merge(year, 1L, Long::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : yearCounts.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("year", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        result.sort(Comparator.comparingInt(m -> (int) m.get("year")));
        return result;
    }

    // ========== 月度活跃度 ==========
    private List<Map<String, Object>> buildMonthlyStats(List<Memory> memories) {
        // 按 年-月 统计
        Map<String, Long> monthCounts = new LinkedHashMap<>();
        for (Memory m : memories) {
            LocalDateTime t = m.getEventTime() != null ? m.getEventTime() : m.getCreatedAt();
            String key = t.getYear() + "-" + String.format("%02d", t.getMonthValue());
            monthCounts.merge(key, 1L, Long::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : monthCounts.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", entry.getKey());
            item.put("count", entry.getValue());
            result.add(item);
        }
        result.sort(Comparator.comparing(m -> (String) m.get("month")));
        return result;
    }

    // ========== 地点分布 ==========
    private List<Map<String, Object>> buildLocationStats(List<Memory> memories) {
        Map<String, Long> locCounts = new LinkedHashMap<>();
        for (Memory m : memories) {
            if (m.getLocation() != null && !m.getLocation().isEmpty()) {
                locCounts.merge(m.getLocation(), 1L, Long::sum);
            }
        }

        return locCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    // ========== 情绪分布 ==========
    private List<Map<String, Object>> buildEmotionStats(List<Memory> memories) {
        Map<String, Long> emotionCounts = new LinkedHashMap<>();
        for (Memory m : memories) {
            if (m.getEmotion() != null && !m.getEmotion().isEmpty()) {
                emotionCounts.merge(m.getEmotion(), 1L, Long::sum);
            }
        }

        // 按指定顺序排列
        List<String> order = List.of("开心", "兴奋", "感动", "温馨", "平静", "思考", "低落");
        List<Map<String, Object>> result = new ArrayList<>();
        for (String emotion : order) {
            if (emotionCounts.containsKey(emotion)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", emotion);
                item.put("value", emotionCounts.get(emotion));
                result.add(item);
                emotionCounts.remove(emotion);
            }
        }
        // 剩余未匹配的
        for (Map.Entry<String, Long> e : emotionCounts.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", e.getKey());
            item.put("value", e.getValue());
            result.add(item);
        }
        return result;
    }

    // ========== 类型分布 ==========
    private List<Map<String, Object>> buildTypeStats(List<Memory> memories) {
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        for (Memory m : memories) {
            String type = m.getMemoryType() != null ? m.getMemoryType() : "other";
            typeCounts.merge(type, 1L, Long::sum);
        }

        return typeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    // ========== 最近记忆（供概览） ==========
    private List<Map<String, Object>> buildRecentMemories(List<Memory> memories) {
        return memories.stream()
                .sorted(Comparator.comparing(
                        m -> m.getCreatedAt() != null ? m.getCreatedAt() : LocalDateTime.MIN,
                        Comparator.reverseOrder()))
                .limit(5)
                .map(m -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", m.getId());
                    item.put("title", m.getTitle());
                    item.put("type", m.getMemoryType());
                    item.put("emotion", m.getEmotion());
                    item.put("date", m.getCreatedAt() != null ? m.getCreatedAt().toString() : "");
                    return item;
                })
                .collect(Collectors.toList());
    }

    private int getYear(Memory m) {
        LocalDateTime t = m.getEventTime() != null ? m.getEventTime() : m.getCreatedAt();
        return t != null ? t.getYear() : 0;
    }
}
