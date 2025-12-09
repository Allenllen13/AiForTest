package com.example.service;

import com.example.model.TestCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestCaseService{

    private final ObjectMapper objectMapper;
    private final TestCaseServiceOld fallbackService;

    @Value("classpath:prompts/system-prompt-step1.txt")
    private Resource fdpPromptResource;
    @Value("classpath:prompts/system-prompt-step2.txt")
    private Resource tggPromptResource;
    @Value("classpath:prompts/system-prompt-step3.txt")
    private Resource tvvPromptResource;

    private String fdpPrompt;
    private String tggPrompt;
    private String tvvPrompt;

    @Value("${volc.ark.api-key:}")
    private String apiKey;
    @Value("${volc.ark.model:}")
    private String model;
    @Value("${volc.ark.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String baseUrl;
    @Value("${volc.ark.enabled:}")
    private boolean llmEnabled;

    @PostConstruct
    public void initPrompts() {
        this.fdpPrompt = loadPrompt(fdpPromptResource);
        this.tggPrompt = loadPrompt(tggPromptResource);
        this.tvvPrompt = loadPrompt(tvvPromptResource);
    }

    public List<TestCase> generateTestCases(String prd) {
        if (!llmEnabled || !isArkConfigReady()) {
            log.warn("火山大模型未启用或配置不完整，回退旧版规则引擎");
            return fallbackService.generateTestCases(prd);
        }

        try {
            ArkService arkService = getArkService();

            // 阶段1：功能点解析
            log.info("调用火山大模型进行功能点解析");
            String fdpResponse = callLlm(prd, fdpPrompt, arkService);
            if (fdpResponse == null) return fallbackService.generateTestCases(prd);
            List<Map<String, Object>> functionPoints = objectMapper.readValue(
                    fdpResponse, new TypeReference<List<Map<String, Object>>>(){}
            );
            log.info("提取功能点数量: {}", functionPoints.size());

            // 阶段2：场景覆盖规划
            log.info("调用火山大模型进行场景规划");
            String tggResponse = callLlm(fdpResponse, tggPrompt, arkService);
            if (tggResponse == null) return fallbackService.generateTestCases(prd);

            // 阶段3：测试用例生成
            log.info("调用火山大模型进行测试用例生成");
            String tvvResponse = callLlm(tggResponse, tvvPrompt, arkService);
            if (tvvResponse == null) return fallbackService.generateTestCases(prd);

            // 解析最终 JSON 为 TestCase 列表
            log.info("开始解析测试用例");
            List<TestCase> testCases = parseLlmResponse(tvvResponse);
            return (testCases != null && !testCases.isEmpty()) ? testCases : fallbackService.generateTestCases(prd);

        } catch (Exception e) {
            log.error("三阶段生成失败，使用规则引擎兜底", e);
            return fallbackService.generateTestCases(prd);
        }
    }

    private String callLlm(String content, String systemPrompt, ArkService arkService) {
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.builder().role(ChatMessageRole.SYSTEM).content(systemPrompt).build());
            messages.add(ChatMessage.builder().role(ChatMessageRole.USER).content(content).build());
            // 构建 thinking 对象
            ChatCompletionRequest.ChatCompletionRequestThinking thinking =
                    new ChatCompletionRequest.ChatCompletionRequestThinking("disabled");

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(0.2)
                    .maxTokens(32000)
                    .thinking(thinking)
                    .build();

            List<ChatCompletionChoice> choices = arkService.createChatCompletion(request).getChoices();
//            if (choices != null && !choices.isEmpty()) {
//                return Objects.requireNonNullElse(choices.get(0).getMessage().getContent(), "").trim();
//            }
            if (choices != null && !choices.isEmpty()) {
                return String.valueOf(choices.get(0).getMessage().getContent()).trim();
            }

        } catch (Exception e) {
            log.error("调用大模型失败", e);
        }
        return null;
    }

    private List<TestCase> parseLlmResponse(String llmResponse) throws Exception {
        if (llmResponse == null || llmResponse.isEmpty()) return null;
        llmResponse = llmResponse.replaceAll("^```json|```$", "")
                .replaceAll("^\"|\"$", "")
                .replaceAll("//.*", "")
                .trim();
        return objectMapper.readValue(llmResponse, new TypeReference<List<TestCase>>() {});
    }

    private boolean isArkConfigReady() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(model);
    }

    private ArkService getArkService() {
        return ArkService.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    private String loadPrompt(Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("无法读取系统提示词文件：" + resource.getFilename(), e);
        }
    }
}
