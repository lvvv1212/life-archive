package com.lifearchive.service.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 内存向量存储（真实语义向量化）
 *
 * <p>内部委托 Spring AI 的 {@link SimpleVectorStore} + 本地 ONNX 向量化模型
 * （{@code spring-ai-starter-model-transformers}，默认 all-MiniLM-L6-v2）完成
 * 真正的文本 embedding 与余弦相似度检索，替代原先「词频 + 余弦」的伪语义实现。
 *
 * <p>对外 API（{@code put / search / size / clear} 与 {@link SearchResult} 字段）
 * 与旧实现保持完全一致，调用方（KnowledgeBaseService / RAGService / StoryServiceImpl）
 * 无需改动。
 */
@Component
public class VectorStore {

    /**
     * Spring AI 向量库（进程内，零外部依赖）。
     * 类型为 org.springframework.ai.vectorstore.VectorStore，因与本项目类名冲突，此处用全限定名。
     */
    private final org.springframework.ai.vectorstore.VectorStore delegate;

    /**
     * 本地索引：id → Document，用于 size() 统计与 put 时的按 id 覆盖。
     * SimpleVectorStore 本身不支持按 id 覆盖/更新，因此由本类负责去重。
     */
    private final Map<String, Document> docIndex = new HashMap<>();

    /**
     * 注入本地 ONNX 向量化模型（由 spring-ai-starter-model-transformers 自动配置）。
     */
    public VectorStore(EmbeddingModel embeddingModel) {
        this.delegate = SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 存储 / 覆盖文档。
     * 若同一 id 已存在则先删除旧文档再写入，保证更新语义。
     * 注意：Spring AI 的 Document 不允许 metadata 含 null 值，此处统一剔除 null 项
     * （调用方可能传入 location/emotion 等为 null 的字段；检索侧对缺失 key 均有兜底）。
     */
    public void put(String id, String text, Map<String, Object> metadata) {
        Map<String, Object> meta = new HashMap<>();
        if (metadata != null) {
            metadata.forEach((k, v) -> {
                if (v != null) meta.put(k, v);
            });
        }
        Document doc = new Document(id, text, meta);
        Document previous = docIndex.put(id, doc);
        if (previous != null) {
            // 已存在同名 id，先从底层库删除旧版本
            delegate.delete(List.of(id));
        }
        delegate.add(List.of(doc));
    }

    /**
     * 检索与 query 最相似的 topK 个文档，按相似度降序返回。
     * 返回的相似度为真实余弦相似度（约 [0,1]，相关越高越接近 1）。
     */
    public List<SearchResult> search(String query, int topK) {
        if (docIndex.isEmpty()) {
            return Collections.emptyList();
        }
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        List<Document> docs = delegate.similaritySearch(request);
        if (docs == null || docs.isEmpty()) {
            return Collections.emptyList();
        }
        return docs.stream()
                .map(d -> new SearchResult(d.getId(),
                        d.getScore() != null ? d.getScore() : 0.0,
                        d.getText(), d.getMetadata()))
                .collect(Collectors.toList());
    }

    /**
     * 获取文档数量。
     */
    public int size() {
        return docIndex.size();
    }

    /**
     * 清空所有数据。
     */
    public void clear() {
        if (!docIndex.isEmpty()) {
            delegate.delete(new ArrayList<>(docIndex.keySet()));
            docIndex.clear();
        }
    }

    // ========== 对外结果结构（与旧实现保持兼容） ==========

    /**
     * 检索结果：调用方依赖的字段 id / similarity / text / metadata 全部保留。
     */
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
