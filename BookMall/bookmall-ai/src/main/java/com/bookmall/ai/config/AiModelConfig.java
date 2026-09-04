package com.bookmall.ai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** DashScope（通义千问）模型配置。@ConfigurationProperties 自动把配置文件中 dashscope.* 绑定到这个类。 */
@Configuration
@ConfigurationProperties(prefix = "dashscope")
@Data
public class AiModelConfig {

    private String apiKey; // API Key，配置里取环境变量 ${DASHSCOPE_API_KEY}
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1"; // OpenAi 兼容模式的地址
    private String model = "qwen-plus"; // 模型名，可换 qwen-max 等
    private Double temperature = 0.7;  // 温度：越高越随机，越低越确定
    private Integer maxTokens = 1024;  // 单次回复最大 token 数
    private Duration timeout = Duration.ofSeconds(30); // 调用超时

    /** 构建一个 LangChain4j 的 ChatModel Bean，内部用它调用通义千问。 */
    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .build();
    }
}