package com.lifearchive.controller;

import com.lifearchive.common.Result;
import com.lifearchive.entity.Memory;
import com.lifearchive.service.ai.AIService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 分析控制器
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    /**
     * 自动分析记忆（根据类型选择分析方式）
     */
    @PostMapping("/analyze")
    public Result<Memory> analyze(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long memoryId = Long.valueOf(body.get("memoryId").toString());
        Long userId = (Long) request.getAttribute("userId");
        Memory memory = aiService.analyze(userId, memoryId);
        return Result.success("分析完成", memory);
    }

    /**
     * 分析图片记忆
     */
    @PostMapping("/analyze/image")
    public Result<Memory> analyzeImage(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long memoryId = Long.valueOf(body.get("memoryId").toString());
        Long userId = (Long) request.getAttribute("userId");
        Memory memory = aiService.analyzeImage(userId, memoryId);
        return Result.success("图片分析完成", memory);
    }

    /**
     * 分析文本记忆
     */
    @PostMapping("/analyze/text")
    public Result<Memory> analyzeText(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long memoryId = Long.valueOf(body.get("memoryId").toString());
        Long userId = (Long) request.getAttribute("userId");
        Memory memory = aiService.analyzeText(userId, memoryId);
        return Result.success("文本分析完成", memory);
    }
}
