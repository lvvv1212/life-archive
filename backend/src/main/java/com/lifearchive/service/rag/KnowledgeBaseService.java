package com.lifearchive.service.rag;

import com.lifearchive.entity.Memory;
import com.lifearchive.mapper.MemoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAG 知识库服务
 * 负责将用户记忆数据索引到向量存储中，供检索使用
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    @Autowired
    private MemoryMapper memoryMapper;

    @Autowired
    private VectorStore vectorStore;

    /**
     * 为指定用户构建知识库索引
     */
    public int buildIndex(Long userId) {
        vectorStore.clear();

        List<Memory> memories = memoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Memory>()
                        .eq(Memory::getUserId, userId)
        );

        for (Memory mem : memories) {
            // 拼接文档文本：标题 + 描述 + 内容 + AI标签
            StringBuilder text = new StringBuilder();
            if (mem.getTitle() != null) text.append(mem.getTitle()).append("。");
            if (mem.getDescription() != null) text.append(mem.getDescription()).append("。");
            if (mem.getContent() != null) text.append(mem.getContent()).append("。");
            if (mem.getAiSummary() != null) text.append(mem.getAiSummary()).append("。");
            if (mem.getTags() != null) text.append(mem.getTags()).append("。");
            if (mem.getLocation() != null) text.append("地点在").append(mem.getLocation()).append("。");
            if (mem.getEmotion() != null) text.append("情绪是").append(mem.getEmotion()).append("。");
            if (mem.getMemoryType() != null) text.append("类型是").append(mem.getMemoryType()).append("。");

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("id", mem.getId());
            metadata.put("title", mem.getTitle());
            metadata.put("time", mem.getEventTime() != null ? mem.getEventTime().toString() : "");
            metadata.put("type", mem.getMemoryType());
            metadata.put("location", mem.getLocation());
            metadata.put("emotion", mem.getEmotion());

            vectorStore.put("mem_" + mem.getId(), text.toString(), metadata);
        }

        log.info("Knowledge base built: {} documents indexed for user {}", vectorStore.size(), userId);
        return vectorStore.size();
    }
}
