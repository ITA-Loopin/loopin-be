package com.loopone.loopinbe.global.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiConfig {
    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.chat.options.model}") // GPT 모델명
    private String openAiModelName;

    @Value("${spring.ai.openai.chat.options.temperature}")
    private Double openAiTemperature;

    @Value("${spring.ai.gemini.api-key}")
    private String geminiApiKey;

    @Value("${spring.ai.gemini.base-url}")
    private String geminiBaseUrl;

    @Value("${spring.ai.gemini.chat.options.model}")
    private String geminiModel;

    @Value("${spring.ai.openai.chat.options.temperature}")
    private Double geminiTemperature;

    @Bean(name = "geminiChatModel")
    public ChatModel geminiChatModel(
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder,
            ResponseErrorHandler responseErrorHandler
    ) {

        OpenAiApi openAiApi = new OpenAiApi(
                geminiBaseUrl,
                () -> geminiApiKey,
                new LinkedMultiValueMap<>(),
                "/chat/completions",
                "/embeddings",
                restClientBuilder,
                webClientBuilder,
                responseErrorHandler
        );

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(geminiModel)
                .temperature(geminiTemperature)
                .build();

        RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

        ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

        return new OpenAiChatModel(
                openAiApi,
                options,
                ToolCallingManager.builder().build(),
                retryTemplate,
                observationRegistry
        );
    }
}
