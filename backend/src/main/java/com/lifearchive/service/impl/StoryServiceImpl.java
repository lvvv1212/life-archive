package com.lifearchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifearchive.entity.Memory;
import com.lifearchive.mapper.MemoryMapper;
import com.lifearchive.service.StoryService;
import com.lifearchive.service.ai.LLMClient;
import com.lifearchive.service.rag.VectorStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 回忆故事生成服务实现
 */
@Slf4j
@Service
public class StoryServiceImpl implements StoryService {

    @Autowired
    private MemoryMapper memoryMapper;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private LLMClient llmClient;

    @Value("${app.ai.story.max-memories:10}")
    private int maxMemories;

    // 预设主题及其对应的检索关键词
    private static final Map<String, String> THEME_KEYWORDS = new LinkedHashMap<>() {{
        put("我的大学生活", "大学 校园 毕业 考试 学习 同学 图书馆 宿舍 社团 比赛");
        put("我的旅行回忆", "旅行 旅游 出游 出发 景点 风景 海边 山川 美食");
        put("我的成长故事", "成长 第一次 生日 毕业 面试 入职 获奖 比赛");
        put("我的美食之旅", "美食 餐厅 火锅 小吃 晚餐 午餐 好吃 美味");
        put("我的2024年", "2024");
        put("我的2025年", "2025");
        put("珍贵的友情", "朋友 同学 聚会 聚餐 一起 合照 合影 社团");
        put("我的学习之路", "学习 考试 读书 论文 项目 比赛 课程 讲座");
    }};

    @Override
    public Map<String, Object> generateStory(Long userId, String theme) {
        // Step 1: 检索相关记忆
        List<Memory> relevantMemories = retrieveMemories(userId, theme);

        // Step 2: 构建故事
        Story story;
        if (llmClient.isEnabled()) {
            story = generateWithLLM(theme, relevantMemories);
        } else {
            story = generateWithTemplate(theme, relevantMemories);
        }

        // Step 3: 返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", story.title);
        result.put("content", story.content);
        result.put("wordCount", countChineseWords(story.content));
        result.put("memoryCount", relevantMemories.size());
        return result;
    }

    // ========== 记忆检索 ==========

    /**
     * 基于关键词检索相关记忆，结合向量检索
     */
    private List<Memory> retrieveMemories(Long userId, String theme) {
        // 按类型关键词精确匹配
        LambdaQueryWrapper<Memory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Memory::getUserId, userId)
                .orderByAsc(Memory::getEventTime);

        List<Memory> allMemories = memoryMapper.selectList(wrapper);

        // 向量检索相关记忆
        String keywords = THEME_KEYWORDS.getOrDefault(theme, theme);
        List<VectorStore.SearchResult> vectorResults = new ArrayList<>();

        // 如果向量库为空则先建索引
        if (vectorStore.size() == 0) {
            buildIndex(allMemories);
        }

        vectorResults = vectorStore.search(keywords, Math.min(20, allMemories.size()));

        // 合并结果
        Set<Long> seenIds = new HashSet<>();
        List<Memory> relevant = new ArrayList<>();

        for (VectorStore.SearchResult r : vectorResults) {
            // 真实余弦相似度过滤：低于 0.10 视为无关，不纳入故事素材（原 0.03 为词频伪向量旧值）
            if (r.similarity < 0.10) continue;
            String id = r.id.replace("mem_", "");
            try {
                Long memId = Long.parseLong(id);
                if (seenIds.add(memId)) {
                    memoryMapper.selectById(memId);
                    Memory mem = memoryMapper.selectById(memId);
                    if (mem != null) relevant.add(mem);
                }
            } catch (NumberFormatException ignored) {}
        }

        // 关键词后备匹配
        if (relevant.size() < 3) {
            for (Memory mem : allMemories) {
                if (seenIds.contains(mem.getId())) continue;
                String text = buildMemoryText(mem).toLowerCase();
                for (String kw : keywords.split("\\s+")) {
                    if (text.contains(kw)) {
                        relevant.add(mem);
                        seenIds.add(mem.getId());
                        break;
                    }
                }
            }
        }

        return relevant;
    }

    private void buildIndex(List<Memory> memories) {
        for (Memory mem : memories) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("id", mem.getId());
            meta.put("title", mem.getTitle());
            vectorStore.put("mem_" + mem.getId(), buildMemoryText(mem), meta);
        }
    }

    private String buildMemoryText(Memory mem) {
        StringBuilder sb = new StringBuilder();
        if (mem.getTitle() != null) sb.append(mem.getTitle()).append("。");
        if (mem.getDescription() != null) sb.append(mem.getDescription()).append("。");
        if (mem.getContent() != null) sb.append(mem.getContent()).append("。");
        if (mem.getTags() != null) sb.append(mem.getTags()).append("。");
        if (mem.getLocation() != null) sb.append("地点在").append(mem.getLocation()).append("。");
        if (mem.getEmotion() != null) sb.append("情绪是").append(mem.getEmotion()).append("。");
        return sb.toString();
    }

    // ========== LLM 生成 ==========

    private Story generateWithLLM(String theme, List<Memory> memories) {
        StringBuilder memoryData = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        int limit = Math.min(memories.size(), maxMemories);
        for (int i = 0; i < limit; i++) {
            Memory m = memories.get(i);
            memoryData.append(i + 1).append(". ");
            memoryData.append("标题：").append(m.getTitle()).append("\n");
            if (m.getEventTime() != null) {
                memoryData.append("   时间：").append(m.getEventTime().format(fmt)).append("\n");
            }
            if (m.getLocation() != null) {
                memoryData.append("   地点：").append(m.getLocation()).append("\n");
            }
            if (m.getEmotion() != null) {
                memoryData.append("   情绪：").append(m.getEmotion()).append("\n");
            }
            if (m.getDescription() != null) {
                memoryData.append("   描述：").append(m.getDescription()).append("\n");
            }
            if (m.getAiSummary() != null) {
                memoryData.append("   摘要：").append(m.getAiSummary()).append("\n");
            }
        }

        String prompt = String.format("""
                用第一人称写一篇关于「%s」的回忆文章。
                要求：按时间顺序，情感真挚，Markdown格式，约400-800字。
                记忆数据：
                %s""", theme, memoryData.toString());

        Map<String, String> system = Map.of("role", "system", "content",
                "你是个人传记作家。简洁温暖。用中文。");
        Map<String, String> user = Map.of("role", "user", "content", prompt);

        String content = llmClient.chat(List.of(system, user), 2000);
        if (content == null || content.isEmpty()) {
            // Fallback to template
            return generateWithTemplate(theme, memories);
        }

        // 清理 LLM 回复中可能包裹的 Markdown 代码块
        content = cleanLLMResponse(content);

        return new Story("《" + theme + "》", content);
    }

    /**
     * 清理 LLM 回复：去除可能包裹的 Markdown 代码块、修复换行等
     */
    private String cleanLLMResponse(String content) {
        if (content == null || content.isEmpty()) return content;

        String trimmed = content.trim();

        // 去除开头的 ```markdown 或 ``` 代码块标记
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
        }
        // 去除结尾的 ``` 代码块标记
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }

        // 确保段落间有双换行（将单换行转为双换行，但保留已有的双换行）
        // 先将连续3个以上换行压缩为2个
        trimmed = trimmed.replaceAll("\n{3,}", "\n\n");
        // 将非标题行后的单换行转为双换行
        StringBuilder sb = new StringBuilder();
        String[] lines = trimmed.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            sb.append(line);
            if (i < lines.length - 1) {
                String nextLine = lines[i + 1];
                // 如果当前行或下一行是空行、标题、列表、引用，保持原样
                if (line.isEmpty() || nextLine.isEmpty()
                        || line.matches("^#{1,3}\\s.*")
                        || nextLine.matches("^#{1,3}\\s.*")
                        || nextLine.matches("^[-*]\\s.*")
                        || nextLine.matches("^\\d+\\.\\s.*")
                        || nextLine.matches("^>\\s.*")) {
                    sb.append("\n");
                } else {
                    // 段落文本：确保双换行分隔
                    sb.append("\n\n");
                }
            }
        }

        return sb.toString();
    }

    // ========== 模板生成（无LLM回退） ==========

    private Story generateWithTemplate(String theme, List<Memory> memories) {
        String title = "《" + theme + "》";
        StringBuilder article = new StringBuilder();

        // 标题
        article.append("# ").append(title).append("\n\n");

        // 引言
        article.append("> 本文由 LifeArchive AI 根据你的个人记忆自动生成。");
        article.append("基于 ").append(memories.size()).append(" 条相关记忆记录。\n\n");

        if (memories.isEmpty()) {
            article.append("暂时没有找到与「").append(theme).append("」相关的记忆。\n\n");
            article.append("💡 **建议**：上传更多相关的照片或日记，让 AI 更好地为你生成回忆文章。\n");
            return new Story(title, article.toString());
        }

        // 按年份分组
        DateTimeFormatter yearFmt = DateTimeFormatter.ofPattern("yyyy年");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

        Map<Integer, List<Memory>> yearGroups = new LinkedHashMap<>();
        for (Memory m : memories) {
            LocalDateTime t = safeTime(m);
            yearGroups.computeIfAbsent(t.getYear(), k -> new ArrayList<>()).add(m);
        }

        // 写引言段
        article.append("回首过往，那些珍贵的记忆片段构成了我人生的精彩篇章。");
        article.append("从 ").append(memories.get(0).getTitle());
        article.append(" 到 ").append(memories.get(memories.size() - 1).getTitle());
        article.append("，每一段经历都值得被铭记。\n\n");

        // 按年份写章节
        int chapterNum = 1;
        for (Map.Entry<Integer, List<Memory>> entry : yearGroups.entrySet()) {
            int year = entry.getKey();
            List<Memory> yearMemories = entry.getValue();

            // 统计情绪
            Map<String, Long> emotionCount = yearMemories.stream()
                    .filter(m -> m.getEmotion() != null)
                    .collect(Collectors.groupingBy(Memory::getEmotion, Collectors.counting()));
            String dominantEmotion = emotionCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("平静");

            article.append("## 第").append(toChinese(chapterNum)).append("章：")
                    .append(year).append("年\n\n");

            // 写记忆条目
            for (int i = 0; i < yearMemories.size(); i++) {
                Memory m = yearMemories.get(i);
                LocalDateTime t = safeTime(m);
                article.append("**").append(t.format(dateFmt)).append("**");

                if (m.getLocation() != null) {
                    article.append(" · 📍 ").append(m.getLocation());
                }
                article.append("\n\n");

                article.append(m.getTitle());
                if (m.getAiSummary() != null) {
                    article.append("——").append(m.getAiSummary());
                } else if (m.getDescription() != null) {
                    article.append("——").append(truncate(m.getDescription(), 120));
                }
                article.append("\n\n");

                if (m.getEmotion() != null) {
                    article.append("> 心情：").append(m.getEmotion()).append("\n\n");
                }

                // 每段之间加分隔
                if (i < yearMemories.size() - 1) {
                    article.append("\n");
                }
            }

            // 章节结尾
            article.append("\n那一年，我主要的心情是 **").append(dominantEmotion).append("**");
            article.append("，共记录了 ").append(yearMemories.size()).append(" 个珍贵的瞬间。\n\n");

            chapterNum++;
        }

        // 结尾
        article.append("---\n\n");
        article.append("## 写在最后\n\n");
        article.append("每一段记忆都是人生的一部分，它们共同构成了今天的我。");
        article.append("感谢 LifeArchive 帮我记录下这些珍贵的瞬间。");
        article.append("未来还有更多精彩等待着我去创造。\n\n");

        // 记忆统计
        article.append("---\n\n");
        article.append("📊 **数据统计**\n\n");
        article.append("- 相关记忆：").append(memories.size()).append(" 条\n");
        article.append("- 时间跨度：")
                .append(safeTime(memories.get(0)).getYear())
                .append("年 — ")
                .append(safeTime(memories.get(memories.size() - 1)).getYear())
                .append("年\n");

        long locationCount = memories.stream().filter(m -> m.getLocation() != null).count();
        article.append("- 涉及地点：").append(locationCount).append(" 个\n");

        return new Story(title, article.toString());
    }

    // ========== 工具方法 ==========

    /**
     * 统计中文字数（中文字符 + 英文单词 + 数字）
     */
    private int countChineseWords(String text) {
        if (text == null || text.isEmpty()) return 0;
        int count = 0;
        boolean inWord = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B) {
                count++;  // 每个中文字符计1字
                inWord = false;
            } else if (Character.isLetterOrDigit(c)) {
                if (!inWord) {
                    count++;  // 连续英文字母/数字计1词
                    inWord = true;
                }
            } else {
                inWord = false;
            }
        }
        return count;
    }

    private String truncate(String text, int maxLen) {
        return text.length() > maxLen ? text.substring(0, maxLen) + "…" : text;
    }

    private String toChinese(int n) {
        String[] map = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
        if (n <= 10) return map[n];
        if (n < 20) return "十" + map[n % 10];
        if (n < 100) return map[n / 10] + "十" + (n % 10 == 0 ? "" : map[n % 10]);
        return String.valueOf(n);
    }

    /**
     * 安全获取记忆时间：优先 eventTime，其次 createdAt，最后兜底当前时间。
     * 避免 eventTime 与 createdAt 同时为空时 NPE，导致整篇生成失败（前端表现为文章空白）。
     */
    private LocalDateTime safeTime(Memory m) {
        if (m.getEventTime() != null) return m.getEventTime();
        if (m.getCreatedAt() != null) return m.getCreatedAt();
        return LocalDateTime.now();
    }

    private static class Story {
        String title;
        String content;

        Story(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}
