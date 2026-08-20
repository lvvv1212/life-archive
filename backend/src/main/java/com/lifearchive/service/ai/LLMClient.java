package com.lifearchive.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 大语言模型 API 客户端 — DeepSeek 优化版
 */
@Slf4j
@Component
public class LLMClient {

    @Value("${app.ai.llm.enabled:false}")
    private boolean enabled;

    @Value("${app.ai.llm.api-key:}")
    private String apiKey;

    @Value("${app.ai.llm.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${app.ai.llm.model:deepseek-chat}")
    private String model;

    @Value("${app.ai.llm.max-tokens:500}")
    private int maxTokens;

    @Value("${app.ai.llm.temperature:0.7}")
    private double temperature;

    // 简易 token 计数器（粗略估算：1 中文 ≈ 1.5 token，1 英文 ≈ 0.7 token）
    private long estimatedTokensUsed = 0;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isEmpty();
    }

    public long getEstimatedTokensUsed() { return estimatedTokensUsed; }

    /**
     * 发送聊天请求
     */
    public String chat(List<Map<String, String>> messages) {
        return chat(messages, maxTokens);
    }

    /**
     * 发送聊天请求（指定 max_tokens）
     */
    public String chat(List<Map<String, String>> messages, int customMaxTokens) {
        if (!isEnabled()) return null;

        // 估算输入 token
        int inputTokens = estimateInputTokens(messages);
        estimatedTokensUsed += inputTokens;

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", messages,
                    "temperature", temperature,
                    "max_tokens", customMaxTokens
            );

            String json = objectMapper.writeValueAsString(body);
            log.debug("LLM request: input~{}t, max_tokens={}", inputTokens, customMaxTokens);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                int outputTokens = root.path("usage").path("completion_tokens").asInt();
                estimatedTokensUsed += outputTokens;
                log.info("LLM tokens: in={} out={} total≈{}",
                        root.path("usage").path("prompt_tokens").asInt(),
                        outputTokens,
                        estimatedTokensUsed);

                return root.path("choices").get(0)
                        .path("message").path("content").asText();
            } else {
                log.warn("LLM API returned status {}: {}", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            log.warn("LLM API call failed: {}", e.getMessage());
            return null;
        }
    }

    /** 粗略估算输入 token 数 */
    private int estimateInputTokens(List<Map<String, String>> messages) {
        int chars = 0;
        for (var m : messages) {
            String c = m.getOrDefault("content", "");
            chars += c != null ? c.length() : 0;
        }
        return chars / 3;  // 中文比例高，约 3 字符/token
    }
}
