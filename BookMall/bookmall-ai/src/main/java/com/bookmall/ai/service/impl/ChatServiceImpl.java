package com.bookmall.ai.service.impl;

import com.bookmall.ai.ai.BookAssistantAiService;
import com.bookmall.ai.dto.ChatRequest;
import com.bookmall.ai.dto.ChatResponse;
import com.bookmall.ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** AI 对话编排实现：负责生成会话 ID、构造记忆键，并调用 @AiService 拿到模型回复。 */
@Service
@RequiredArgsConstructor // Lombok：自动为 final 字段生成构造方法，便于注入
public class ChatServiceImpl implements ChatService {

    private final BookAssistantAiService aiService;

    @Override
    public ChatResponse chat(Long userId, ChatRequest request) {
        // 若前端没传会话 ID，就自己生成一个随机 ID，用它来做到“连续对话”
        String conversationId = request.getConversationId() == null || request.getConversationId().isBlank()
                ? UUID.randomUUID().toString()
                : request.getConversationId();
        // 记忆键 = 用户 + 会话，天然隔离不同用户与不同会话，避免串数据
        String memoryId = userId + ":" + conversationId;
        // 调用 LangChain4j 生成的代理实现：内部会带上系统提示词、历史记忆和可用的 @Tool
        String reply = aiService.chat(memoryId, request.getMessage());
        return ChatResponse.of(reply, conversationId);
    }
}
