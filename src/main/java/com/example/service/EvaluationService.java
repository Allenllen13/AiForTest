package com.example.service;

import com.example.model.EvaluationScore;
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

/**
 * 测试用例评测服务
 * 调用大模型对测试用例进行评分
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {
    private final ObjectMapper objectMapper;
    
    @Value("classpath:prompts/evaluation-system-prompt.txt")
    private Resource systemPromptResource;
    private String systemPrompt;
    
    // 从配置文件读取火山 SDK 必要配置
    @Value("${volc.ark.api-key:}")
    private String apiKey;
    @Value("${volc.ark.model:}")
    private String model;
    @Value("${volc.ark.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String baseUrl;
    @Value("${volc.ark.enabled:}")
    private boolean llmEnabled;
    
    @PostConstruct
    public void initPrompt() {
        this.systemPrompt = loadSystemPrompt();
    }
    
    /**
     * 评测测试用例列表（对整个测试用例集合进行整体评估）
     * @param prd PRD内容
     * @param testCases 待评测的测试用例列表
     * @return 整体评测结果
     */
    public EvaluationScore evaluateTestCases(String prd, List<TestCase> testCases) {
        if (!llmEnabled) {
            log.warn("未启用火山大模型配置，无法进行评测");
            return createDefaultScore();
        }
        
        if (!isArkConfigReady()) {
            log.error("火山大模型配置不完整（apiKey 或 model 为空），无法进行评测");
            return createDefaultScore();
        }
        
        log.info("开始调用火山大模型评测测试用例，PRD长度: {}, 测试用例数量: {}", prd.length(), testCases.size());
        
        ArkService arkService = null;
        
        try {
            arkService = getArkService();
            
            // 构建评测请求消息
            List<ChatMessage> messages = new ArrayList<>();

            // 系统提示词
            ChatMessage systemMessage = ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(systemPrompt)
                    .build();
            
            // 用户消息：包含PRD和测试用例
            String userContent = buildUserMessage(prd, testCases);
            ChatMessage userMessage = ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(userContent)
                    .build();
            
            messages.add(systemMessage);
            messages.add(userMessage);
            ChatCompletionRequest.ChatCompletionRequestThinking thinking =
                    new ChatCompletionRequest.ChatCompletionRequestThinking("disabled");
            // 构建请求
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(0.2) // 精准度优先
                    .maxTokens(32000)
                    .thinking(thinking)
                    .build();
            
            // 调用大模型
            String llmResponse = "";
            try {
                List<ChatCompletionChoice> choices = arkService.createChatCompletion(request).getChoices();
                if (choices != null && !choices.isEmpty()) {
                    String content = (String) Objects.requireNonNullElse(
                            choices.get(0).getMessage().getContent(),
                            ""
                    );
                    llmResponse = content.trim();
                }
                log.info("火山大模型返回评测结果: {}", llmResponse);
            } catch (Exception e) {
                log.error("调用大模型评测失败", e);
                return createDefaultScore();
            }
            
            // 解析评测结果（返回单个整体评分）
            EvaluationScore score = parseEvaluationResponse(llmResponse);
            
            return score;
            
        } catch (Exception e) {
            log.error("评测过程发生异常", e);
            return createDefaultScore();
        } finally {
            if (arkService != null) {
                try {
                    arkService.shutdownExecutor();
                } catch (Exception ex) {
                    log.warn("关闭 ArkService 资源失败", ex);
                }
            }
        }
    }
    
    /**
     * 构建用户消息内容
     */
    private String buildUserMessage(String prd, List<TestCase> testCases) {
        StringBuilder sb = new StringBuilder();
        sb.append("PRD内容：\n").append(prd).append("\n\n");
        sb.append("待评测的测试用例：\n");
        
        try {
            String testCasesJson = objectMapper.writeValueAsString(testCases);
            sb.append(testCasesJson);
        } catch (Exception e) {
            log.error("序列化测试用例失败", e);
            // 降级为文本格式
            for (int i = 0; i < testCases.size(); i++) {
                TestCase tc = testCases.get(i);
                sb.append("\n测试用例 ").append(i + 1).append(":\n");
                sb.append("标题: ").append(tc.getTitle()).append("\n");
                sb.append("前置条件: ").append(tc.getPrecondition()).append("\n");
                sb.append("操作步骤: ").append(tc.getSteps()).append("\n");
                sb.append("预期结果: ").append(tc.getExpectedResult()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 解析大模型返回的评测结果（返回单个整体评分对象）
     */
    private EvaluationScore parseEvaluationResponse(String llmResponse) throws Exception {
        if (llmResponse == null || llmResponse.isEmpty()) {
            return createDefaultScore();
        }
        
        // 清洗返回内容
        llmResponse = llmResponse
                .replaceAll("^```json|```$", "")
                .replaceAll("^\"|\"$", "")
                .replaceAll("//.*", "")
                .trim();
        
        try {
            // 尝试解析为单个JSON对象
            EvaluationScore score = objectMapper.readValue(llmResponse, EvaluationScore.class);
            
            // 验证和修正分数范围
            validateAndFixScore(score);
            
            return score;
        } catch (Exception e) {
            log.error("解析评测结果失败，尝试解析为数组格式", e);
            // 如果解析为对象失败，尝试解析为数组（取第一个元素）
            try {
                List<EvaluationScore> scores = objectMapper.readValue(
                        llmResponse, 
                        new TypeReference<List<EvaluationScore>>() {}
                );
                if (scores != null && !scores.isEmpty()) {
                    EvaluationScore score = scores.get(0);
                    validateAndFixScore(score);
                    return score;
                }
            } catch (Exception e2) {
                log.error("解析为数组也失败", e2);
            }
            // 如果都失败，返回默认评分
            return createDefaultScore();
        }
    }
    
    /**
     * 验证和修正分数范围（0-5）
     */
    private void validateAndFixScore(EvaluationScore score) {
        if (score.getCoverage() == null || score.getCoverage() < 0 || score.getCoverage() > 100) {
            score.setCoverage(0);
        }
        if (score.getQuality() == null || score.getQuality() < 0 || score.getQuality() > 100) {
            score.setQuality(0);
        }
        if (score.getNonRedundancy() == null || score.getNonRedundancy() < 0 || score.getNonRedundancy() > 100) {
            score.setNonRedundancy(0);
        }
    }
    
    /**
     * 创建默认评分（全0分）
     */
    private EvaluationScore createDefaultScore() {
        return new EvaluationScore(0, 0, 0);
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
    
    private String loadSystemPrompt() {
        try (InputStream inputStream = systemPromptResource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("无法读取评测系统提示词文件，使用默认提示词", e);
            return getDefaultPrompt();
        }
    }
    
    /**
     * 默认提示词（如果文件不存在）
     */
    private String getDefaultPrompt() {
        return "你是一个专业的测试用例评测专家。请根据PRD需求文档，对整个测试用例集合进行整体评分。\n" +
                "评分标准：\n" +
                "1. 覆盖性（0-5）：0=完全未覆盖PRD需求；5=完全覆盖对应需求点\n" +
                "2. 正确性（0-5）：步骤、预期与PRD一致，错误越少分数越高\n" +
                "3. 逻辑性（0-5）：步骤是否执行得通；是否缺少关键步骤；是否存在不可执行描述\n" +
                "4. 完整性（0-5）：包含前置、输入、步骤、预期结果为5分\n" +
                "5. 精确性（0-5）：预期结果可验证、具体（如\"页面跳转成功\"\"提示XXX\"）\n" +
                "6. 非冗余度（0-5）：5=无冗余内容；0=冗余或与PRD无关内容严重\n" +
                "请返回JSON对象格式，包含coverage、correctness、logic、completeness、precision、nonRedundancy六个字段，每个字段都是0-5的整数。";
    }
}

