package com.lifearchive.service.ai;

import com.lifearchive.entity.Memory;

/**
 * AI 分析服务接口
 */
public interface AIService {

    /**
     * 分析图片记忆
     */
    Memory analyzeImage(Long userId, Long memoryId);

    /**
     * 分析文本记忆
     */
    Memory analyzeText(Long userId, Long memoryId);

    /**
     * 自动分析（根据记忆类型选择分析方式）
     */
    Memory analyze(Long userId, Long memoryId);
}
