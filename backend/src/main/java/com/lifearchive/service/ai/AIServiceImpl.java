package com.lifearchive.service.ai;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lifearchive.entity.Memory;
import com.lifearchive.mapper.MemoryMapper;
import com.lifearchive.service.ai.rules.RuleBasedAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 分析服务实现
 * 优先使用 LLM 分析，不可用时回退到规则引擎
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    @Autowired
    private MemoryMapper memoryMapper;

    @Autowired
    private LLMClient llmClient;

    @Autowired
    private RuleBasedAnalyzer ruleAnalyzer;

    @Override
    public Memory analyzeImage(Long userId, Long memoryId) {
        Memory memory = getOwnedMemory(userId, memoryId);

        // 构建分析提示词
        String prompt = buildImagePrompt(memory);
        String llmResult = callLLM(prompt);

        // 解析分析结果
        AnalysisResult result = llmResult != null
                ? parseJsonResult(llmResult)
                : ruleAnalyzer.analyzeImage(memory);

        // 更新记忆
        updateMemoryWithAnalysis(memoryId, result);

        return memoryMapper.selectById(memoryId);
    }

    @Override
    public Memory analyzeText(Long userId, Long memoryId) {
        Memory memory = getOwnedMemory(userId, memoryId);

        // 构建分析提示词
        String prompt = buildTextPrompt(memory);
        String llmResult = callLLM(prompt);

        // 解析分析结果
        AnalysisResult result = llmResult != null
                ? parseJsonResult(llmResult)
                : ruleAnalyzer.analyzeText(memory);

        // 更新记忆
        updateMemoryWithAnalysis(memoryId, result);

        return memoryMapper.selectById(memoryId);
    }

    @Override
    public Memory analyze(Long userId, Long memoryId) {
        Memory memory = getOwnedMemory(userId, memoryId);

        String fileType = memory.getFileType();
        if ("image".equals(fileType)) {
            return analyzeImage(userId, memoryId);
        } else {
            return analyzeText(userId, memoryId);
        }
    }

    // ========== 私有方法 ==========

    /**
     * 查询并校验记忆归属，防止越权访问他人记忆
     */
    private Memory getOwnedMemory(Long userId, Long memoryId) {
        Memory memory = memoryMapper.selectById(memoryId);
        if (memory == null) {
            throw new RuntimeException("记忆不存在");
        }
        if (!memory.getUserId().equals(userId)) {
            throw new RuntimeException("记忆不存在");
        }
        return memory;
    }

    /**
     * 调用 LLM API（默认 max_tokens）
     */
    private String callLLM(String prompt) {
        return callLLM(prompt, 200);
    }

    private String callLLM(String prompt, int maxTokens) {
        if (!llmClient.isEnabled()) {
            log.info("LLM not configured, using rule-based analysis");
            return null;
        }

        try {
            Map<String, String> systemMsg = Map.of(
                    "role", "system",
                    "content", "你是记忆分析助手。只输出JSON，不要其他内容。"
            );
            Map<String, String> userMsg = Map.of("role", "user", "content", prompt);
            return llmClient.chat(List.of(systemMsg, userMsg), maxTokens);
        } catch (Exception e) {
            log.warn("LLM call failed, fallback to rules: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建图片分析 Prompt
     */
    private String buildImagePrompt(Memory memory) {
        return String.format("""
                分析记忆，输出JSON：
                标题：%s | 描述：%s
                输出：{"location":"地点","emotion":"开心/感动/平静/兴奋/低落/温馨","event":"类型","tags":["tag1","tag2"],"summary":"<50字摘要"}
                只输出JSON""",
                memory.getTitle() != null ? memory.getTitle() : "未知",
                memory.getDescription() != null ? memory.getDescription() : "无");
    }

    /**
     * 构建文本分析 Prompt
     */
    private String buildTextPrompt(Memory memory) {
        return String.format("""
                请分析以下文本记忆，输出JSON格式的结果：

                分析文本，输出JSON：
                标题：%s | 内容：%s
                输出：{"emotion":"开心/感动/平静/兴奋/低落/温馨/思考","tags":["t1","t2","t3"],"summary":"<60字摘要"}
                只输出JSON""",
                memory.getTitle() != null ? memory.getTitle() : "未知",
                memory.getContent() != null ? truncateText(memory.getContent(), 500) : "无");
    }

    /**
     * 解析 LLM 返回的 JSON 结果
     */
    private AnalysisResult parseJsonResult(String raw) {
        try {
            // 提取 JSON 块
            String json = raw.trim();
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("```json") + 7, json.lastIndexOf("```"));
            } else if (json.contains("```")) {
                json = json.substring(json.indexOf("```") + 3, json.lastIndexOf("```"));
            }
            json = json.trim();

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            AnalysisResult result = new AnalysisResult();
            var node = mapper.readTree(json);

            if (node.has("location")) result.location = node.get("location").asText();
            if (node.has("emotion")) result.emotion = node.get("emotion").asText();
            if (node.has("event")) result.event = node.get("event").asText();
            if (node.has("summary")) result.summary = node.get("summary").asText();

            if (node.has("tags")) {
                var tagsNode = node.get("tags");
                if (tagsNode.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (var t : tagsNode) {
                        if (!sb.isEmpty()) sb.append(",");
                        sb.append(t.asText());
                    }
                    result.tags = sb.toString();
                }
            }

            if (node.has("keywords")) {
                var kwNode = node.get("keywords");
                if (kwNode.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (var t : kwNode) {
                        if (!sb.isEmpty()) sb.append(",");
                        sb.append(t.asText());
                    }
                    result.tags = sb.toString();
                }
            }

            return result;
        } catch (Exception e) {
            log.warn("Failed to parse LLM result: {}", e.getMessage());
            return new AnalysisResult();
        }
    }

    /**
     * 更新记忆的 AI 分析结果
     */
    private void updateMemoryWithAnalysis(Long memoryId, AnalysisResult result) {
        LambdaUpdateWrapper<Memory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Memory::getId, memoryId);

        if (result.location != null && !result.location.isEmpty()) {
            wrapper.set(Memory::getLocation, result.location);
        }
        if (result.emotion != null && !result.emotion.isEmpty()) {
            wrapper.set(Memory::getEmotion, result.emotion);
        }
        if (result.tags != null && !result.tags.isEmpty()) {
            wrapper.set(Memory::getTags, result.tags);
        }
        if (result.summary != null && !result.summary.isEmpty()) {
            wrapper.set(Memory::getAiSummary, result.summary);
        }

        wrapper.set(Memory::getUpdatedAt, LocalDateTime.now());
        memoryMapper.update(null, wrapper);
    }

    private String truncateText(String text, int maxLen) {
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /**
     * 分析结果内部类
     */
    public static class AnalysisResult {
        public String location;
        public String emotion;
        public String event;
        public String tags;
        public String summary;
    }
}
