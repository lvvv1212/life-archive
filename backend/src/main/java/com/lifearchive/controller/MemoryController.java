package com.lifearchive.controller;

import com.lifearchive.common.Result;
import com.lifearchive.entity.Memory;
import com.lifearchive.service.MemoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 记忆管理控制器
 */
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    @Autowired
    private MemoryService memoryService;

    /**
     * 上传记忆（图片/视频）
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "memoryType", required = false) String memoryType,
            @RequestParam(value = "emotion", required = false) String emotion,
            @RequestParam(value = "location", required = false) String location,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        Memory memory = memoryService.uploadMemory(userId, file, title, description, memoryType, emotion, location);

        Map<String, Object> data = new HashMap<>();
        data.put("id", memory.getId());
        data.put("title", memory.getTitle());
        data.put("fileUrl", memory.getFileUrl());
        data.put("fileType", memory.getFileType());
        data.put("emotion", memory.getEmotion());
        data.put("location", memory.getLocation());
        data.put("createdAt", memory.getCreatedAt());
        return Result.success("上传成功", data);
    }

    /**
     * 创建文本记忆
     */
    @PostMapping("/text")
    public Result<Memory> createText(@RequestBody Map<String, String> body,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        Memory memory = memoryService.createTextMemory(
                userId,
                body.get("title"),
                body.get("content"),
                body.get("memoryType")
        );
        return Result.success("创建成功", memory);
    }

    /**
     * 获取记忆列表
     */
    @GetMapping("/list")
    public Result<List<Memory>> list(
            @RequestParam(value = "memoryType", required = false) String memoryType,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "12") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        List<Memory> list = memoryService.getMemoryList(userId, memoryType, page, size);
        return Result.success(list);
    }

    /**
     * 获取记忆详情
     */
    @GetMapping("/{id}")
    public Result<Memory> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Memory memory = memoryService.getMemoryById(id);
        if (memory == null || !memory.getUserId().equals(userId)) {
            return Result.error("记忆不存在");
        }
        return Result.success(memory);
    }

    /**
     * 删除记忆
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        memoryService.deleteMemory(id, userId);
        return Result.success();
    }

    /**
     * 更新记忆（接收 Map 手动解析，避免 Jackson LocalDateTime 反序列化问题）
     */
    @PutMapping("/{id}")
    public Result<Memory> update(@PathVariable Long id,
                                  @RequestBody Map<String, Object> body,
                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        Memory updateData = new Memory();
        if (body.containsKey("title"))       updateData.setTitle((String) body.get("title"));
        if (body.containsKey("description")) updateData.setDescription((String) body.get("description"));
        if (body.containsKey("content"))     updateData.setContent((String) body.get("content"));
        if (body.containsKey("emotion"))     updateData.setEmotion((String) body.get("emotion"));
        if (body.containsKey("location"))    updateData.setLocation((String) body.get("location"));
        if (body.containsKey("memoryType"))  updateData.setMemoryType((String) body.get("memoryType"));

        // 手动解析 eventTime，兼容多种格式
        if (body.containsKey("eventTime") && body.get("eventTime") != null) {
            String timeStr = body.get("eventTime").toString().trim();
            if (!timeStr.isEmpty()) {
                try {
                    // 兼容 "2026-07-24T03:53:00" 和 "2026-07-24 03:53:00" 两种格式
                    timeStr = timeStr.replace(" ", "T");
                    // 截取到秒
                    if (timeStr.length() > 19) timeStr = timeStr.substring(0, 19);
                    updateData.setEventTime(java.time.LocalDateTime.parse(timeStr,
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
                } catch (Exception e) {
                    // 日期解析失败不阻断整个更新，跳过该字段
                    System.err.println("[MemoryController] eventTime 解析失败: " + timeStr + " - " + e.getMessage());
                }
            }
        }

        Memory updated = memoryService.updateMemory(id, userId, updateData);
        return Result.success("更新成功", updated);
    }

    /**
     * 获取记忆统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", memoryService.countMemories(userId));
        return Result.success(stats);
    }
}
