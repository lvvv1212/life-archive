package com.lifearchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifearchive.entity.Memory;
import com.lifearchive.mapper.MemoryMapper;
import com.lifearchive.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 记忆服务实现
 */
@Service
public class MemoryServiceImpl implements MemoryService {

    @Autowired
    private MemoryMapper memoryMapper;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Override
    public Memory uploadMemory(Long userId, MultipartFile file, String title, String description, String memoryType, String emotion, String location) {
        // 保存文件到本地
        String fileUrl = saveFile(file);

        // 获取文件类型
        String fileType = getFileType(file.getOriginalFilename());

        Memory memory = new Memory();
        memory.setUserId(userId);
        memory.setTitle(title != null ? title : file.getOriginalFilename());
        memory.setDescription(description);
        memory.setFileUrl(fileUrl);
        memory.setFileType(fileType);
        memory.setMemoryType(memoryType != null ? memoryType : fileType);
        memory.setEmotion(emotion != null ? emotion : "");
        memory.setLocation(location != null ? location : "");
        memory.setEventTime(LocalDateTime.now());

        memoryMapper.insert(memory);
        return memory;
    }

    @Override
    public Memory createTextMemory(Long userId, String title, String content, String memoryType) {
        Memory memory = new Memory();
        memory.setUserId(userId);
        memory.setTitle(title);
        memory.setContent(content);
        memory.setMemoryType(memoryType != null ? memoryType : "diary");
        memory.setFileType("text");
        memory.setEventTime(LocalDateTime.now());

        memoryMapper.insert(memory);
        return memory;
    }

    @Override
    public List<Memory> getMemoryList(Long userId, String memoryType, int page, int size) {
        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Memory::getUserId, userId);
        if (memoryType != null && !memoryType.isEmpty()) {
            wrapper.eq(Memory::getMemoryType, memoryType);
        }
        wrapper.orderByDesc(Memory::getCreatedAt);

        Page<Memory> pageResult = memoryMapper.selectPage(new Page<>(page, size), wrapper);
        return pageResult.getRecords();
    }

    @Override
    public long countMemories(Long userId) {
        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Memory::getUserId, userId);
        return memoryMapper.selectCount(wrapper);
    }

    @Override
    public Memory getMemoryById(Long id) {
        return memoryMapper.selectById(id);
    }

    @Override
    @Transactional
    public Memory updateMemory(Long id, Long userId, Memory updateData) {
        Memory memory = memoryMapper.selectById(id);
        if (memory == null || !memory.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改此记忆");
        }

        // 构造仅包含需要更新字段的实体，避免 updateById 覆盖其他字段
        Memory entity = new Memory();
        entity.setId(id);

        if (updateData.getTitle() != null) {
            entity.setTitle(updateData.getTitle());
            memory.setTitle(updateData.getTitle());
        }
        if (updateData.getDescription() != null) {
            entity.setDescription(updateData.getDescription());
            memory.setDescription(updateData.getDescription());
        }
        if (updateData.getContent() != null) {
            entity.setContent(updateData.getContent());
            memory.setContent(updateData.getContent());
        }
        if (updateData.getEmotion() != null) {
            entity.setEmotion(updateData.getEmotion());
            memory.setEmotion(updateData.getEmotion());
        }
        if (updateData.getLocation() != null) {
            entity.setLocation(updateData.getLocation());
            memory.setLocation(updateData.getLocation());
        }
        if (updateData.getMemoryType() != null) {
            entity.setMemoryType(updateData.getMemoryType());
            memory.setMemoryType(updateData.getMemoryType());
        }
        // 允许修改记忆发生的时间（图片/日记等均可编辑）
        if (updateData.getEventTime() != null) {
            entity.setEventTime(updateData.getEventTime());
            memory.setEventTime(updateData.getEventTime());
        }

        // updateById 会自动应用 @TableLogic 条件 AND deleted = 0
        // MetaObjectHandler 会自动填充 updatedAt
        int rows = memoryMapper.updateById(entity);
        if (rows == 0) {
            throw new RuntimeException("更新失败，记录可能已被删除");
        }

        memory.setUpdatedAt(java.time.LocalDateTime.now());
        return memory;
    }

    @Override
    public void deleteMemory(Long id, Long userId) {
        Memory memory = memoryMapper.selectById(id);
        if (memory == null || !memory.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此记忆");
        }
        // 删除物理文件
        if (memory.getFileUrl() != null) {
            File file = new File(uploadPath + memory.getFileUrl().replace("/upload/", ""));
            if (file.exists()) {
                file.delete();
            }
        }
        memoryMapper.deleteById(id);
    }

    /**
     * 保存文件到本地
     */
    private String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 确保目录存在
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成唯一文件名
        String originalName = file.getOriginalFilename();
        String suffix = "";
        if (originalName != null && originalName.contains(".")) {
            suffix = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + suffix;

        try {
            File dest = new File(uploadPath + fileName);
            file.transferTo(dest);
            return "/upload/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }
    }

    /**
     * 根据文件名判断文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null) return "unknown";
        String lower = fileName.toLowerCase();
        if (lower.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) return "image";
        if (lower.matches(".*\\.(mp4|avi|mov|mkv|wmv|flv)$")) return "video";
        if (lower.matches(".*\\.(pdf|doc|docx|txt|md)$")) return "document";
        return "other";
    }
}
