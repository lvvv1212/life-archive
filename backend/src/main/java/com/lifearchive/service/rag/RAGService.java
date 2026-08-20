package com.lifearchive.service.rag;

import com.lifearchive.service.ai.LLMClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.time.Year;

/**
 * RAG 对话服务 — Token 优化 + 闲聊兜底
 */
@Slf4j
@Service
public class RAGService {

    @Autowired private KnowledgeBaseService knowledgeBase;
    @Autowired private VectorStore vectorStore;
    @Autowired private LLMClient llmClient;

    @Value("${app.ai.rag.max-retrieve:3}")
    private int maxRetrieve;

    @Value("${app.ai.rag.max-context-chars:600}")
    private int maxContextChars;

    @Value("${app.ai.rag.max-history:10}")
    private int maxHistory;

    // ====== 闲聊问答（0 token） ======
    private static final Map<String, String> CHITCHAT = new LinkedHashMap<>() {{
        put("你是谁",    "你好！我是你的 **AI 记忆助手** 🧠✨\n\n"
                         + "我会从你上传的照片、日记和记忆数据中寻找答案，帮你回忆过去的经历、整理人生故事。\n\n"
                         + "你可以问我：\n"
                         + "- 「我去年最开心的事是什么？」\n"
                         + "- 「总结一下我的大学生活」\n"
                         + "- 「我第一次旅行是什么时候？」\n\n"
                         + "先上传一些照片或日记，我就能更好地为你服务啦～");
        put("你好",      "你好呀！👋 我是你的 AI 记忆助手。有什么关于你的记忆想聊聊吗？");
        put("谢谢",      "不客气！能帮到你我也很开心 😊");
        put("再见",      "再见！随时回来，你的记忆我一直帮你保管着～");
        put("你能做什么", "我可以：\n"
                         + "🔍 **检索记忆** — 从你的个人档案中找答案\n"
                         + "📝 **分析记忆** — 自动识别照片地点、情绪、标签\n"
                         + "📊 **数据统计** — 你的记忆数量、情绪分布等\n"
                         + "✍️ **回忆生成** — 撰写你的人生故事\n\n"
                         + "试试问我一些关于你过去的问题吧！");
    }};

    /**
     * RAG 问答
     */
    public Map<String, Object> chat(Long userId, String question, List<Map<String, String>> history) {
        // --- 零层：闲聊匹配 ---
        String chitChatAnswer = matchChitChat(question);
        if (chitChatAnswer != null) {
            return Map.of("answer", chitChatAnswer, "sources", List.of());
        }

        // --- 一层：知识库就绪 ---
        if (vectorStore.size() == 0) knowledgeBase.buildIndex(userId);

        // --- 二层：简单记忆查询 → 本地结构化回答 ---
        if (isSimpleMemoryQuery(question)) {
            List<VectorStore.SearchResult> hit = vectorStore.search(question, maxRetrieve);
            if (!hit.isEmpty()) {
                // 年份/情绪等结构化限定问题（今年/去年/开心/难忘）直接尝试本地回答，不依赖语义相似度；
                // 模糊记忆查询需较高语义相关度(>0.45)才本地回答，避免答非所问
                boolean structured = question.contains("今年") || question.contains("去年")
                        || question.contains("开心") || question.contains("高兴")
                        || question.contains("快乐") || question.contains("难忘");
                if (structured || hit.get(0).similarity > 0.45) {
                    String localAnswer = answerLocal(question, hit);
                    if (localAnswer != null && localAnswer.length() > 20) {
                        return Map.of("answer", localAnswer, "sources", buildSources(hit));
                    }
                }
            }
        }

        // --- 三层：RAG 检索 ---
        List<VectorStore.SearchResult> retrieved = vectorStore.search(question, maxRetrieve);
        String context = buildCompactContext(retrieved);

        // --- 四层：如果记忆数据为空，跳过 LLM 直接返回引导 ---
        // 真实余弦相似度：0.20 为「是否命中相关记忆」的宽松门槛（保召回，避免误报“没有找到”）
        boolean hasRelevantMemory = !retrieved.isEmpty() && retrieved.get(0).similarity > 0.20;
        if (!hasRelevantMemory) {
            return Map.of("answer",
                    "我在你的记忆档案中还没有找到相关信息 🧐\n\n"
                    + "**可能的原因：**\n"
                    + "1. 还没有上传相关记忆 → 去「上传记忆」添加照片或日记\n"
                    + "2. 记忆还未 AI 分析 → 去「记忆列表」点击 AI 分析按钮\n\n"
                    + "**提示：** 上传并分析足够多的记忆后，我就能更好地回答你的问题啦！",
                    "sources", List.of());
        }

        // --- 五层：调用 LLM ---
        List<Map<String, String>> compactHistory = compressHistory(history);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "你是 LifeArchive 个人记忆助手。请严格遵循以下规则：\n"
                + "1. 只能依据下方『记忆数据』回答，严禁使用你自己的知识或编造内容。\n"
                + "2. 每条记忆前有『相关度XX%』标注，请优先采信相关度高的；相关度低于40%的可能是噪声，请忽略。\n"
                + "3. 若『记忆数据』为空，或所有条目都与问题无关，请如实说『我在你的记忆里没有找到相关内容』，并引导用户上传更多记忆。\n"
                + "4. 用简洁、口语化的中文回答，聚焦用户真正问的内容（如问『最开心』就回答开心的事，不要答非所问）。"));
        for (var h : compactHistory) messages.add(h);
        messages.add(Map.of("role", "user", "content",
                "记忆数据：\n" + context + "\n问题：" + question));

        String answer = llmClient.chat(messages, 500);

        // --- 六层：LLM 失败 → 本地兜底 ---
        if (answer == null || answer.isBlank()) {
            answer = answerLocal(question, retrieved);
            if (answer == null || answer.isBlank()) {
                answer = "抱歉，我暂时无法回答这个问题。\n\n" +
                         "你可以尝试：\n" +
                         "1. 换个方式提问\n" +
                         "2. 上传更多记忆丰富我的知识库\n" +
                         "3. 先用「AI 分析」功能处理已有记忆";
            }
        }

        return Map.of("answer", answer, "sources", buildSources(retrieved));
    }

    // ====== 闲聊匹配 ======
    private String matchChitChat(String q) {
        String lowerQ = q.trim().toLowerCase().replaceAll("\\s+", "");
        for (var entry : CHITCHAT.entrySet()) {
            if (lowerQ.contains(entry.getKey().replaceAll("\\s+", ""))) {
                return entry.getValue();
            }
        }
        // 纯打招呼（短且无实质内容）
        if (lowerQ.length() <= 3 && !containsChineseWord(lowerQ)) {
            return "你好！有什么我可以帮你的吗？😊";
        }
        return null;
    }

    private boolean containsChineseWord(String s) {
        for (char c : s.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) return true;
        }
        return false;
    }

    // ====== 本地回答 ======
    private String answerLocal(String q, List<VectorStore.SearchResult> hits) {
        if (hits.isEmpty()) return null;
        // 结构化限定（年份/情绪）不依赖语义相似度；模糊记忆查询需 >0.20 才采信，避免答非所问
        boolean structured = q.contains("今年") || q.contains("去年")
                || q.contains("开心") || q.contains("高兴") || q.contains("快乐") || q.contains("难忘");
        if (!structured && hits.get(0).similarity < 0.20) return null;

        List<VectorStore.SearchResult> candidates = new ArrayList<>(hits);

        // 时间限定（结构化年份，可靠）
        int nowYear = Year.now().getValue();
        boolean hasYearConstraint = false;
        if (q.contains("今年")) {
            hasYearConstraint = true;
            candidates = filterByYear(candidates, nowYear);
        } else if (q.contains("去年")) {
            hasYearConstraint = true;
            candidates = filterByYear(candidates, nowYear - 1);
        }

        // 情绪限定（语义，匹配不到不强制，退回现有候选避免答非所问）
        boolean hasEmotionConstraint = false;
        if (q.contains("开心") || q.contains("高兴") || q.contains("快乐")) {
            hasEmotionConstraint = true;
            List<VectorStore.SearchResult> positive = candidates.stream()
                    .filter(r -> isPositiveEmotion(val(r.metadata.get("emotion"))))
                    .collect(Collectors.toList());
            if (!positive.isEmpty()) candidates = positive;
        } else if (q.contains("难忘")) {
            hasEmotionConstraint = true;
            List<VectorStore.SearchResult> memorable = candidates.stream()
                    .filter(r -> "难忘".equals(val(r.metadata.get("emotion"))))
                    .collect(Collectors.toList());
            if (!memorable.isEmpty()) candidates = memorable;
        }

        // 过滤后无结果 → 直接给出明确提示，不再退回 LLM（避免 LLM 超时）
        if (candidates.isEmpty()) {
            if (hasYearConstraint && hasEmotionConstraint) {
                return "我在你的记忆里没有找到符合条件的内容 📭\n\n"
                        + "可能你还没有上传过相关时期的记忆，或者那些回忆还没有被 AI 分析。";
            } else if (hasYearConstraint) {
                // 有年份限制但没匹配到 → 告知用户记忆里最近的是哪年
                String nearestYear = findNearestYear(hits);
                if (!nearestYear.isEmpty()) {
                    return "你的记忆里没有 " + (q.contains("今年") ? "今年" : "去年") + " 的记录哦。\n"
                            + "最近的记忆来自 " + nearestYear + " 年，要聊聊那年的事吗？";
                }
                return "你的记忆里没有 " + (q.contains("今年") ? "今年" : "去年") + " 的记录。";
            }
            return null; // 无限定词导致的空结果，退回 LLM
        }

        var best = candidates.get(0);
        String title = val(best.metadata.get("title"));
        String time = val(best.metadata.get("time"));
        String loc = val(best.metadata.get("location"));
        String emotion = val(best.metadata.get("emotion"));
        StringBuilder sb = new StringBuilder("根据你的记忆，").append(title);
        if (!time.isEmpty()) sb.append("发生在").append(safeSub(time, 10));
        if (!loc.isEmpty()) sb.append("，地点是").append(loc);
        if (!emotion.isEmpty()) sb.append("，当时心情").append(emotion);
        sb.append("。");
        return sb.toString();
    }

    /** 从命中结果中找最近的年份 */
    private String findNearestYear(List<VectorStore.SearchResult> hits) {
        for (var r : hits) {
            String time = val(r.metadata.get("time"));
            if (time.length() >= 4) {
                try { return time.substring(0, 4); }
                catch (Exception ignored) {}
            }
        }
        return "";
    }

    /** 仅保留指定年份的记忆（time 字段前 4 位为年份） */
    private List<VectorStore.SearchResult> filterByYear(List<VectorStore.SearchResult> hits, int year) {
        List<VectorStore.SearchResult> result = new ArrayList<>();
        for (var r : hits) {
            String time = val(r.metadata.get("time"));
            if (time.length() >= 4) {
                try {
                    if (Integer.parseInt(time.substring(0, 4)) == year) result.add(r);
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    /** 宽松判断是否为正向情绪（emotion 取值不统一，做包含匹配） */
    private boolean isPositiveEmotion(String emotion) {
        if (emotion == null || emotion.isEmpty()) return false;
        return emotion.contains("开心") || emotion.contains("高兴") || emotion.contains("快乐")
                || emotion.contains("愉快") || emotion.contains("兴奋") || emotion.contains("激动")
                || emotion.contains("感动") || emotion.contains("幸福") || emotion.contains("满足");
    }

    private boolean isSimpleMemoryQuery(String q) {
        String[] patterns = {"什么时候", "在哪里", "什么时候的", "哪次", "第一次",
                "最开心", "最难忘", "最近一次", "今年", "去年"};
        for (String p : patterns) if (q.contains(p)) return true;
        return false;
    }

    private String buildCompactContext(List<VectorStore.SearchResult> hits) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (var r : hits) {
            // 统一阈值：仅拼接相关度 ≥ 0.20 的片段，消除 0.10~0.20 噪声漏洞
            if (r.similarity < 0.20) continue;
            String t = (String) r.metadata.getOrDefault("title", "");
            String time = (String) r.metadata.getOrDefault("time", "");
            String loc = (String) r.metadata.getOrDefault("location", "");
            int pct = (int) Math.round(r.similarity * 100);
            sb.append(i).append(". (相关度").append(pct).append("%) ").append(t)
              .append(" | ").append(time.length()>=10 ? time.substring(0,10) : time);
            if (!loc.isEmpty()) sb.append(" | ").append(loc);
            sb.append(" | ").append(truncate(r.text, maxContextChars)).append("\n");
            i++;
        }
        return sb.toString();
    }

    private List<Map<String, String>> compressHistory(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) return List.of();
        List<Map<String, String>> compressed = new ArrayList<>();
        for (var h : history) {
            compressed.add(Map.of("role", h.getOrDefault("role", "user"),
                    "content", truncate(h.getOrDefault("content", ""), 200)));
        }
        if (compressed.size() > maxHistory)
            compressed = compressed.subList(compressed.size() - maxHistory, compressed.size());
        return compressed;
    }

    private List<Map<String, Object>> buildSources(List<VectorStore.SearchResult> hits) {
        return hits.stream()
                .filter(r -> r.similarity > 0.20)
                .map(r -> Map.<String, Object>of(
                        "title", r.metadata.getOrDefault("title", ""),
                        "similarity", Math.round(r.similarity * 1000) / 10.0))
                .toList();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) : text;
    }

    /** 安全取值，null → "" */
    private String val(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    /** 安全截取，null → ""，不够长也安全 */
    private String safeSub(String s, int len) {
        if (s == null || s.isEmpty()) return "";
        return s.length() > len ? s.substring(0, len) : s;
    }
}
