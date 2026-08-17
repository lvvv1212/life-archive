package com.lifearchive.service;

import com.lifearchive.entity.Memory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 记忆服务接口
 */
public interface MemoryService {

    /**
     * 上传并创建记忆
     */
    Memory uploadMemory(Long userId, MultipartFile file, String title, String description, String memoryType, String emotion, String location);

    /**
     * 创建纯文本记忆
     */
    Memory createTextMemory(Long userId, String title, String content, String memoryType);

    /**
     * 获取记忆列表
     */
    List<Memory> getMemoryList(Long userId, String memoryType, int page, int size);

    /**
     * 获取记忆总数
     */
    long countMemories(Long userId);

    /**
     * 获取记忆详情
     */
    Memory getMemoryById(Long id);

    /**
     * 更新记忆
     */
    Memory updateMemory(Long id, Long userId, Memory updateData);

    /**
     * 删除记忆
     */
    void deleteMemory(Long id, Long userId);
}
