package com.bookmall.ai.service.impl;

import com.bookmall.ai.ai.BookAssistantAiService;
import com.bookmall.ai.dto.ChatRequest;
import com.bookmall.ai.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private BookAssistantAiService aiService;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(aiService);
    }

    @Test
    void chat_generatesConversationIdAndReturnsReply_whenConversationIdMissing() {
        when(aiService.chat(anyString(), anyString())).thenReturn("你好，需要我帮您搜书或查订单吗？");

        ChatRequest request = new ChatRequest();
        request.setMessage("你好");

        ChatResponse response = chatService.chat(7L, request);

        assertEquals("你好，需要我帮您搜书或查订单吗？", response.getReply());
        assertNotNull(response.getConversationId());
        verify(aiService).chat(anyString(), anyString());
    }

    @Test
    void chat_keepsConversationIdAndBuildsMemoryKey_whenProvided() {
        when(aiService.chat(anyString(), anyString())).thenReturn("这是回复");

        ChatRequest request = new ChatRequest();
        request.setMessage("在吗");
        request.setConversationId("c-1");

        ChatResponse response = chatService.chat(9L, request);

        assertEquals("c-1", response.getConversationId());
        verify(aiService).chat("9:c-1", "在吗");
    }
}
