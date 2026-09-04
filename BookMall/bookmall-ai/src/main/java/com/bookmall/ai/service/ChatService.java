package com.bookmall.ai.service;

import com.bookmall.ai.dto.ChatRequest;
import com.bookmall.ai.dto.ChatResponse;

/** AI 对话服务接口：定义“用户提问 → 返回 AI 回复”的能力，由 ChatServiceImpl 实现。 */
public interface ChatService {

    /**
     * 发起一次 AI 对话。
     *
     * @param userId  当前登录用户 ID（来自网关注入的 X-User-Id）
     * @param request 用户消息 + 可选会话 ID
     * @return 包含 AI 回复和会话 ID 的响应
     */
    ChatResponse chat(Long userId, ChatRequest request);
}
