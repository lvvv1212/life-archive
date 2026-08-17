package com.lifearchive.controller;

import com.lifearchive.common.Result;
import com.lifearchive.service.StoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 回忆故事生成控制器
 */
@RestController
@RequestMapping("/api/story")
public class StoryController {

    @Autowired
    private StoryService storyService;

    /**
     * 生成回忆文章
     */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody Map<String, String> body,
                                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String theme = body.get("theme");

        if (theme == null || theme.trim().isEmpty()) {
            return Result.error("请选择生成主题");
        }

        Map<String, Object> story = storyService.generateStory(userId, theme.trim());
        return Result.success("生成成功", story);
    }
}
