package com.lifearchive;

import com.lifearchive.common.Result;
import com.lifearchive.controller.MemoryController;
import com.lifearchive.entity.Memory;
import com.lifearchive.mapper.MemoryMapper;
import com.lifearchive.service.MemoryService;
import com.lifearchive.service.ai.AIServiceImpl;
import com.lifearchive.service.ai.LLMClient;
import com.lifearchive.service.ai.rules.RuleBasedAnalyzer;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 越权防护：他人的记忆不可被详情查看 / AI 分析。
 */
@ExtendWith(MockitoExtension.class)
class OwnershipTest {

    @Mock private MemoryMapper memoryMapper;
    @Mock private MemoryService memoryService;
    @Mock private LLMClient llmClient;
    @Mock private RuleBasedAnalyzer ruleAnalyzer;
    @InjectMocks private AIServiceImpl aiService;
    @InjectMocks private MemoryController memoryController;

    @Test
    void aiAnalyzeRejectsOtherUsersMemory() {
        Memory mem = new Memory();
        mem.setId(10L);
        mem.setUserId(99L);
        when(memoryMapper.selectById(10L)).thenReturn(mem);

        assertThrows(RuntimeException.class, () -> aiService.analyze(1L, 10L));
    }

    @Test
    void memoryDetailHidesOtherUsersMemory() {
        Memory mem = new Memory();
        mem.setId(10L);
        mem.setUserId(99L);
        when(memoryService.getMemoryById(10L)).thenReturn(mem);

        Result<Memory> result = memoryController.detail(10L, request(1L));
        assertEquals(500, result.getCode());
    }

    private HttpServletRequest request(Long userId) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("userId")).thenReturn(userId);
        return req;
    }
}
