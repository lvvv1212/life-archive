package com.lifearchive.controller;

import com.lifearchive.common.Result;
import com.lifearchive.service.rag.KnowledgeBaseService;
import com.lifearchive.service.rag.RAGService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI 智能助手控制器 — Token 优化版
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    @Autowired private RAGService ragService;
    @Autowired private KnowledgeBaseService knowledgeBase;

    /**
     * AI 问答（RAG）— 支持对话历史
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> body,
                                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String question = (String) body.get("question");

        if (question == null || question.trim().isEmpty()) {
            return Result.error("请输入问题");
        }

        // 提取前端传来的对话历史
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) body.getOrDefault("history", List.of());

        Map<String, Object> result = ragService.chat(userId, question.trim(), history);
        return Result.success(result);
    }

    /**
     * 重建知识库索引
     */
    @PostMapping("/rebuild-index")
    public Result<Map<String, Object>> rebuildIndex(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int count = knowledgeBase.buildIndex(userId);
        return Result.success(Map.of("indexedCount", count));
    }

    /**
     * Token 用量查询
     */
    @GetMapping("/token-usage")
    public Result<Map<String, Object>> tokenUsage() {
        return Result.success(Map.of("note", "Token 优化已生效：RAG max retrieve=3, max output=300 token"));
    }
}
