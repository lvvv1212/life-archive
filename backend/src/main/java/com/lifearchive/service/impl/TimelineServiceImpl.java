package com.lifearchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifearchive.entity.Memory;
import com.lifearchive.mapper.MemoryMapper;
import com.lifearchive.service.TimelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 时间轴服务实现
 */
@Service
public class TimelineServiceImpl implements TimelineService {

    @Autowired
    private MemoryMapper memoryMapper;

    @Override
    public List<Map<String, Object>> getTimeline(Long userId) {
        // 查询用户所有记忆，按事件时间排序
        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Memory::getUserId, userId)
                .orderByAsc(Memory::getEventTime);

        List<Memory> memories = memoryMapper.selectList(wrapper);
        return buildTimeline(memories);
    }

    @Override
    public List<Map<String, Object>> getTimelineByYear(Long userId, int year) {
        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(year, 12, 31, 23, 59);

        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Memory::getUserId, userId)
                .ge(Memory::getEventTime, start)
                .le(Memory::getEventTime, end)
                .orderByAsc(Memory::getEventTime);

        List<Memory> memories = memoryMapper.selectList(wrapper);
        return buildTimeline(memories);
    }

    /**
     * 将记忆列表构建为按年份分组的时间轴结构
     */
    private List<Map<String, Object>> buildTimeline(List<Memory> memories) {
        DateTimeFormatter yearFmt = DateTimeFormatter.ofPattern("yyyy");
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MM");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 按年份分组
        Map<Integer, List<Memory>> yearGroups = new LinkedHashMap<>();
        for (Memory memory : memories) {
            LocalDateTime eventTime = memory.getEventTime() != null
                    ? memory.getEventTime()
                    : memory.getCreatedAt();
            int year = eventTime.getYear();
            yearGroups.computeIfAbsent(year, k -> new ArrayList<>()).add(memory);
        }

        // 构建时间轴
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (Map.Entry<Integer, List<Memory>> entry : yearGroups.entrySet()) {
            Map<String, Object> yearNode = new LinkedHashMap<>();
            yearNode.put("year", entry.getKey());

            // 构建该年份的事件列表
            List<Map<String, Object>> events = new ArrayList<>();
            for (Memory memory : entry.getValue()) {
                Map<String, Object> event = new LinkedHashMap<>();
                LocalDateTime eventTime = memory.getEventTime() != null
                        ? memory.getEventTime()
                        : memory.getCreatedAt();

                event.put("memoryId", memory.getId());
                event.put("time", eventTime.format(dateFmt));
                event.put("month", Integer.parseInt(eventTime.format(monthFmt)));
                event.put("title", memory.getTitle());
                event.put("description", memory.getDescription() != null
                        ? memory.getDescription()
                        : memory.getAiSummary());
                event.put("memoryType", memory.getMemoryType());

                // 关联图片
                if (memory.getFileUrl() != null && "image".equals(memory.getFileType())) {
                    event.put("image", memory.getFileUrl());
                } else {
                    event.put("image", null);
                }

                // 标签
                if (memory.getTags() != null) {
                    event.put("tags", Arrays.asList(memory.getTags().split(",")));
                } else {
                    event.put("tags", Collections.emptyList());
                }

                // 情绪
                event.put("emotion", memory.getEmotion() != null ? memory.getEmotion() : "");

                // 地点
                event.put("location", memory.getLocation() != null ? memory.getLocation() : "");

                // AI摘要
                event.put("aiSummary", memory.getAiSummary() != null ? memory.getAiSummary() : "");

                events.add(event);
            }

            yearNode.put("events", events);
            yearNode.put("count", events.size());
            timeline.add(yearNode);
        }

        return timeline;
    }
}
