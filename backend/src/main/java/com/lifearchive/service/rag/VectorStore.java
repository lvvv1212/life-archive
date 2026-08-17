package com.lifearchive.service.rag;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 简易内存向量存储
 * 使用关键词频率向量 + 余弦相似度实现语义检索
 * 后期可替换为 Milvus / Pinecone / Qdrant 等专业向量数据库
 */
@Component
public class VectorStore {

    /**
     * 文档存储：id → (text, metadata)
     */
    private final Map<String, DocEntry> docs = new LinkedHashMap<>();

    /**
     * 词汇表：word → index
     */
    private final Map<String, Integer> vocabulary = new HashMap<>();

    /**
     * 文档向量缓存
     */
    private final Map<String, double[]> vectors = new HashMap<>();

    private boolean vocabBuilt = false;

    /**
     * 存储文档
     */
    public void put(String id, String text, Map<String, Object> metadata) {
        docs.put(id, new DocEntry(text, metadata));
        vocabBuilt = false; // 词汇表需要重建
    }

    /**
     * 检索最相似的 topK 个文档
     */
    public List<SearchResult> search(String query, int topK) {
        if (docs.isEmpty()) {
            return Collections.emptyList();
        }

        // 确保词汇表已构建
        if (!vocabBuilt) {
            buildVocabulary();
        }

        // 将查询转为向量
        double[] queryVec = textToVector(query);

        // 计算每个文档的余弦相似度
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : vectors.entrySet()) {
            double similarity = cosineSimilarity(queryVec, entry.getValue());
            if (similarity > 0) {
                DocEntry doc = docs.get(entry.getKey());
                results.add(new SearchResult(entry.getKey(), similarity, doc.text, doc.metadata));
            }
        }

        // 按相似度降序排列，取 topK
        results.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        return results.stream().limit(topK).collect(Collectors.toList());
    }

    /**
     * 获取文档数量
     */
    public int size() {
        return docs.size();
    }

    /**
     * 清空所有数据
     */
    public void clear() {
        docs.clear();
        vocabulary.clear();
        vectors.clear();
        vocabBuilt = false;
    }

    // ========== 私有方法 ==========

    /**
     * 构建词汇表
     */
    private void buildVocabulary() {
        vocabulary.clear();
        vectors.clear();

        // 收集所有词
        int idx = 0;
        for (DocEntry doc : docs.values()) {
            for (String word : tokenize(doc.text)) {
                if (!vocabulary.containsKey(word)) {
                    vocabulary.put(word, idx++);
                }
            }
        }

        // 为每个文档构建向量
        for (Map.Entry<String, DocEntry> entry : docs.entrySet()) {
            vectors.put(entry.getKey(), textToVector(entry.getValue().text));
        }

        vocabBuilt = true;
    }

    /**
     * 文本转向量（词频向量）
     */
    private double[] textToVector(String text) {
        double[] vec = new double[vocabulary.size()];
        List<String> words = tokenize(text);
        for (String word : words) {
            Integer idx = vocabulary.get(word);
            if (idx != null) {
                vec[idx] += 1.0;
            }
        }
        // L2 归一化
        double norm = 0;
        for (double v : vec) norm += v * v;
        if (norm > 0) {
            norm = Math.sqrt(norm);
            for (int i = 0; i < vec.length; i++) vec[i] /= norm;
        }
        return vec;
    }

    /**
     * 余弦相似度
     */
    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 中文分词（简化：按字符 bigram + 单个词切分）
     */
    private List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        List<String> tokens = new ArrayList<>();

        // 按标点和空格切分
        String[] segments = text.toLowerCase().split("[，。！？、；：\\s,.!?;:]+");
        for (String seg : segments) {
            seg = seg.trim();
            if (seg.isEmpty()) continue;

            // bigram 分词
            for (int i = 0; i < seg.length() - 1; i++) {
                tokens.add(seg.substring(i, i + 2));
            }
            // 单字也加入
            for (char c : seg.toCharArray()) {
                if (Character.isLetter(c) || Character.isDigit(c) || isChinese(c)) {
                    tokens.add(String.valueOf(c));
                }
            }
        }
        return tokens;
    }

    private boolean isChinese(char c) {
        return Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    // ========== 内部类 ==========

    static class DocEntry {
        String text;
        Map<String, Object> metadata;

        DocEntry(String text, Map<String, Object> metadata) {
            this.text = text;
            this.metadata = metadata;
        }
    }

    public static class SearchResult {
        public String id;
        public double similarity;
        public String text;
        public Map<String, Object> metadata;

        SearchResult(String id, double similarity, String text, Map<String, Object> metadata) {
            this.id = id;
            this.similarity = similarity;
            this.text = text;
            this.metadata = metadata;
        }
    }
}
