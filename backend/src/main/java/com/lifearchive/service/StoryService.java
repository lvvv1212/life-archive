package com.lifearchive.service;

import java.util.Map;

/**
 * AI 回忆故事生成服务接口
 */
public interface StoryService {

    /**
     * 根据主题生成回忆文章
     * @param userId 用户ID
     * @param theme  主题（如"我的大学生活"）
     * @return { title, content, wordCount, memoryCount }
     */
    Map<String, Object> generateStory(Long userId, String theme);
}
