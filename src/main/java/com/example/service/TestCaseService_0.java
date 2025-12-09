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
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestCaseService_0 {
    // 注入 Jackson 工具类（解析大模型返回的 JSON 为 TestCase）
    private final ObjectMapper objectMapper;
    private final TestCaseServiceOld fallbackService;
    @Value("classpath:prompts/testcase-system-prompt_02.txt")
    private Resource systemPromptResource;
    private String systemPrompt;

    // 从配置文件读取火山 SDK 必要配置
    @Value("${volc.ark.api-key:}")
    private String apiKey; // 火山 API Key（从应用管理获取）
    @Value("${volc.ark.model:}")
    private String model; // 模型名（如 doubao-seed-1-6-251015）
    @Value("${volc.ark.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String baseUrl; // 火山 API 基础地址（默认北京地域）
    @Value("${volc.ark.enabled:}")
    private boolean llmEnabled;

    // 初始化 ArkService（官方 SDK 核心服务类）
    private ArkService getArkService() {
        return ArkService.builder()
                .apiKey(apiKey) // 传入 API Key（官方 SDK 已封装签名逻辑，无需手动处理）
                .baseUrl(baseUrl) // 传入基础地址
                .build();
    }

    @PostConstruct
    public void initPrompt() {
        this.systemPrompt = loadSystemPrompt();
    }

    /**
     * 核心逻辑：调用火山官方 SDK 生成测试用例（单次调用）
     */
    public List<TestCase> generateTestCases(String prd) {
        if (!llmEnabled) {
            log.warn("未启用火山大模型配置，自动回退为旧版规则引擎。可在 application.yml 中将 volc.ark.enabled 设为 true 以启用。");
            return fallbackService.generateTestCases(prd);
        }

        if (!isArkConfigReady()) {
            log.error("火山大模型配置不完整（apiKey 或 model 为空），回退为旧版规则引擎。");
            return fallbackService.generateTestCases(prd);
        }

        log.info("开始调用火山大模型生成测试用例，PRD长度: {}", prd.length());

        // 1. 构建 ArkService（官方 SDK 实例）
        ArkService arkService = null;

        try {
            arkService = getArkService();
            // 2. 构建大模型请求（系统提示词 + 用户 PRD）
            List<ChatMessage> messages = new ArrayList<>();

            // 系统提示词（指导大模型生成结构化测试用例）
            ChatMessage systemMessage = ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(systemPrompt)
                    .build();

            // 用户消息（传入 PRD 内容）
            ChatMessage userMessage = ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content("PRD内容：" + prd)
                    .build();

            messages.add(systemMessage);
            messages.add(userMessage);

            // 3. 构建 SDK 请求对象
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model) // 指定模型名
                    .messages(messages) // 消息列表（系统提示词 + 用户 PRD）
                    .temperature(0.2) // 精准度优先（0.0~1.0）
                    .maxTokens(32000) // 最大返回长度
                    .build();

            // 4. 调用火山大模型 API（官方 SDK 已封装 HTTP 调用、签名、重试）
//            String llmResponse = Objects.requireNonNullElse(
//                    arkService.createChatCompletion(request)
//                            .getChoices()
//                            .get(0)
//                            .getMessage()
//                            .getContent(), // 可能为 null 的字段
//                    "" // 兜底值：如果为 null，返回空字符串
//            ).trim(); // 空字符串调用 trim() 无问题
//            log.info("火山大模型返回结果: {}", llmResponse);

            String llmResponse = "";
            try {
                List<ChatCompletionChoice> choices = arkService.createChatCompletion(request).getChoices();
                if (choices != null && !choices.isEmpty()) {
                    // 强制转换为 String，空则返回 ""
                    String content = (String) Objects.requireNonNullElse(
                            choices.get(0).getMessage().getContent(),
                            ""
                    );
                    llmResponse = content.trim();
                }
                log.info("火山大模型返回结果: {}", llmResponse);
            } catch (Exception e) {
                log.error("调用失败", e);
                return fallbackService.generateTestCases(prd);
            }

            // 5. 解析 JSON 为 TestCase 列表
            List<TestCase> testCases = parseLlmResponse(llmResponse);

            // 6. 有效用例返回，无效则返回兜底用例
            return testCases != null && !testCases.isEmpty()
                    ? testCases
                    : fallbackService.generateTestCases(prd);

        } catch (Exception e) {
            log.error("火山大模型调用或解析失败，使用规则引擎兜底", e);
            return fallbackService.generateTestCases(prd);
        } finally {
            // 关闭 SDK 执行器（避免资源泄露）
            if (arkService != null) {
                try {
                    arkService.shutdownExecutor();
                } catch (Exception ex) {
                    log.warn("关闭 ArkService 资源失败", ex);
                }
            }
        }
    }

    private boolean isArkConfigReady() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(model);
    }

    private String loadSystemPrompt() {
        try (InputStream inputStream = systemPromptResource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("无法读取测试用例系统提示词文件", e);
        }
    }

    /**
     * 解析大模型返回的 JSON 为 TestCase 列表
     */
    private List<TestCase> parseLlmResponse(String llmResponse) throws Exception {
        if (llmResponse == null || llmResponse.isEmpty()) {
            return null;
        }

        // 清洗大模型返回的多余字符（避免格式错误）
        llmResponse = llmResponse
                .replaceAll("^```json|```$", "") // 去除代码块标记（如 ```json ... ```）
                .replaceAll("^\"|\"$", "") // 去除前后多余双引号
                .replaceAll("//.*", "") // 去除注释
                .trim();

        // 解析 JSON 数组为 List<TestCase>
        return objectMapper.readValue(llmResponse, new TypeReference<List<TestCase>>() {});
    }

    /**
     * 兜底：生成默认测试用例（大模型调用失败时使用）
     */
}