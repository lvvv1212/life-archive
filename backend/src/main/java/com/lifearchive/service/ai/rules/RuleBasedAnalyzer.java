package com.lifearchive.service.ai.rules;

import com.lifearchive.entity.Memory;
import com.lifearchive.service.ai.AIServiceImpl.AnalysisResult;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 基于规则的记忆分析器
 * 当 LLM 不可用时作为回退方案，基于关键词和类型进行智能推断
 */
@Component
public class RuleBasedAnalyzer {

    // ====== 情绪关键词映射 ======
    private static final Map<String, String> EMOTION_KEYWORDS = new LinkedHashMap<>() {{
        put("开心|快乐|高兴|欢笑|笑容|愉快|欢乐|喜悦|兴奋|庆祝", "开心");
        put("感动|流泪|温暖|温馨|感恩|感谢|珍贵|美好|幸福", "感动");
        put("考试|学习|读书|图书馆|教室|上课|作业|论文|毕业", "思考");
        put("旅行|旅游|出发|到达|风景|山水|海边|山川|日出|日落|景点", "兴奋");
        put("安静|独处|休息|放松|悠闲|散步|咖啡|看书|听歌", "平静");
        put("难过|悲伤|离别|告别|失落|遗憾|结束|离开", "低落");
        put("家人|妈妈|爸爸|父母|孩子|家庭|团聚|团圆|亲情", "温馨");
    }};

    // ====== 地点关键词映射 ======
    private static final Map<String, String> LOCATION_KEYWORDS = new LinkedHashMap<>() {{
        put("西湖|雷峰塔|灵隐寺|杭州", "杭州");
        put("故宫|长城|天安门|颐和园|鸟巢|北京", "北京");
        put("外滩|东方明珠|陆家嘴|南京路|上海|迪士尼", "上海");
        put("鼓浪屿|厦门|曾厝垵|南普陀", "厦门");
        put("大雁塔|兵马俑|钟楼|西安", "西安");
        put("春熙路|宽窄巷子|熊猫基地|成都|都江堰", "成都");
        put("解放碑|洪崖洞|磁器口|重庆", "重庆");
        put("博物馆|展览|画廊|艺术馆", "博物馆");
        put("大学|校园|教室|图书馆|宿舍|食堂", "学校");
        put("海边|沙滩|海浪|大海|海洋|海岛", "海边");
        put("山上|山顶|登山|爬山|山峰|山间", "山上");
        put("公园|花园|植物园|草坪|树林", "公园");
        put("餐厅|饭店|美食|小吃|火锅|烧烤", "餐厅");
        put("家|家里|回家|家门口", "家");
    }};

    // ====== 事件类型关键词 ======
    private static final Map<String, String> EVENT_KEYWORDS = new LinkedHashMap<>() {{
        put("旅行|旅游|出发|游玩|景点|风景区|度假", "旅行");
        put("聚会|聚餐|朋友|同学|一起|吃|喝|唱|社团", "聚会");
        put("美食|好吃|美味|饭店|餐厅|晚餐|午餐|早餐|火锅|蛋糕", "美食");
        put("学习|读书|考试|上课|作业|论文|毕业|图书馆|教室", "学习");
        put("运动|跑步|健身|篮球|足球|游泳|瑜伽|比赛", "运动");
        put("生日|蛋糕|礼物|许愿|祝福|庆祝", "生日");
        put("毕业|毕业照|学士服|离开校园|答辩|离校", "毕业");
        put("工作|上班|面试|入职|加班|项目|会议|同事", "工作");
    }};

    // ====== 标签题库 ======
    private static final List<String> TAG_POOL = List.of(
            "青春", "友情", "成长", "旅行", "美食", "学习", "运动", "自然",
            "城市", "校园", "家庭", "节日", "日常", "回忆", "毕业", "工作",
            "夏日", "冬日", "春天", "秋天", "夜晚", "清晨", "电影", "音乐"
    );

    /**
     * 基于规则的图片分析
     */
    public AnalysisResult analyzeImage(Memory memory) {
        String text = buildText(memory);
        AnalysisResult result = new AnalysisResult();

        result.location = matchKeywords(text, LOCATION_KEYWORDS, "未知地点");
        result.emotion = matchKeywords(text, EMOTION_KEYWORDS, "平静");
        result.event = matchKeywords(text, EVENT_KEYWORDS, "日常");
        result.tags = extractTags(text, memory.getMemoryType());
        result.summary = generateSummary(memory, result);

        return result;
    }

    /**
     * 基于规则的文本分析
     */
    public AnalysisResult analyzeText(Memory memory) {
        AnalysisResult result = new AnalysisResult();

        String text = buildText(memory);
        result.emotion = matchKeywords(text, EMOTION_KEYWORDS, "平静");
        result.tags = extractTags(text, memory.getMemoryType());
        result.summary = generateSummary(memory, result);

        return result;
    }

    // ========== 私有工具方法 ==========

    private String buildText(Memory memory) {
        StringBuilder sb = new StringBuilder();
        if (memory.getTitle() != null) sb.append(memory.getTitle()).append(" ");
        if (memory.getDescription() != null) sb.append(memory.getDescription()).append(" ");
        if (memory.getContent() != null) sb.append(memory.getContent());
        return sb.toString();
    }

    /**
     * 关键词匹配
     */
    private String matchKeywords(String text, Map<String, String> mapping, String defaultVal) {
        String lower = text.toLowerCase();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            for (String kw : entry.getKey().split("\\|")) {
                if (lower.contains(kw)) {
                    return entry.getValue();
                }
            }
        }
        return defaultVal;
    }

    /**
     * 标签提取
     */
    private String extractTags(String text, String memoryType) {
        Set<String> matched = new LinkedHashSet<>();

        for (String tag : TAG_POOL) {
            if (text.contains(tag)) {
                matched.add(tag);
            }
        }

        // 根据类型添加默认标签
        if (memoryType != null) {
            switch (memoryType) {
                case "travel" -> { matched.add("旅行"); matched.add("风景"); }
                case "study" -> { matched.add("学习"); matched.add("成长"); }
                case "diary" -> { matched.add("日常"); matched.add("记录"); }
                case "photo" -> matched.add("摄影");
                case "video" -> matched.add("影像");
            }
        }

        // 保证至少3个标签
        if (matched.size() < 3) {
            List<String> pool = new ArrayList<>(TAG_POOL);
            Collections.shuffle(pool, new Random(42));
            for (String tag : pool) {
                if (matched.size() >= 5) break;
                matched.add(tag);
            }
        }

        return String.join(",", matched);
    }

    /**
     * 生成摘要
     */
    private String generateSummary(Memory memory, AnalysisResult result) {
        String title = memory.getTitle() != null ? memory.getTitle() : "这条记忆";
        String emotion = result.emotion != null ? result.emotion : "日常";
        String location = result.location != null ? result.location : "";
        String event = result.event != null ? result.event : "记录";

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("，记录了");
        if (!location.isEmpty()) {
            sb.append("在").append(location);
        }
        sb.append("的一次").append(event).append("经历");
        if (!"日常".equals(event)) {
            sb.append("，充满了").append(emotion).append("的情绪");
        }
        sb.append("。");

        return sb.toString();
    }
}
