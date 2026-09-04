package com.bookmall.ai.controller;

import com.bookmall.ai.dto.ChatRequest;
import com.bookmall.ai.dto.ChatResponse;
import com.bookmall.ai.service.ChatService;
import com.bookmall.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 助手对外 REST Controller：只暴露“健康检查”和“AI 对话”两个接口。 */
@RestController // 返回值自动写成 JSON
@RequestMapping("/ai") // 统一接口前缀；经网关访问时是 /api/ai
@RequiredArgsConstructor // Lombok：自动注入 final 的 ChatService
public class AiAssistantController {

    private final ChatService chatService;

    /** 健康检查，用于确认 ai 服务已启动。 */
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("ai-assistant is running");
    }

    /**
     * AI 对话入口。
     *
     * @param userId 网关注入的 X-User-Id，代表当前登录用户
     * @param request 请求体：用户消息 + 可选会话 ID
     * @return Result 包装的 AI 回复与会话 ID
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestHeader("X-User-Id") Long userId,
                                     @Valid @RequestBody ChatRequest request) { // @Valid 触发 @NotBlank 校验
        return Result.success(chatService.chat(userId, request));
    }
}
