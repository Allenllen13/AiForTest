package com.example.service;

import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.docx.v1.model.RawContentDocumentReq;
import com.lark.oapi.service.docx.v1.model.RawContentDocumentResp;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 飞书文档服务（移除无效QueryFile依赖）
 */
@Service
@Slf4j
public class FeishuService {

    // 飞书应用配置
    @Value("${feishu.app.id}")
    private String appId;

    @Value("${feishu.app.secret}")
    private String appSecret;

    // 飞书SDK客户端（单例）
    private Client feishuClient;
    private final ReentrantLock clientLock = new ReentrantLock();

    /**
     * 获取飞书文档内容（核心方法）
     */
    public String fetchDocumentContent(String url) {
        log.info("开始获取飞书文档内容，URL: {}", url);

        try {
            // 1. 预处理URL并提取文档ID（直接作为file_token使用）
            String cleanUrl = url.trim().replaceAll("\\\\", "");
            String fileToken = extractDocId(cleanUrl);
            if (fileToken == null) {
                throw new RuntimeException("无法从URL中提取文档ID，请检查URL格式");
            }
            log.info("使用file_token: {}", fileToken);

            // 2. 初始化飞书SDK客户端
            Client client = getFeishuClient();

            // 3. 直接调用文档内容接口（跳过无效的file_token查询）
            return getDocumentRawContent(client, fileToken);

        } catch (Exception e) {
            log.error("获取飞书文档内容失败", e);
            throw new RuntimeException("获取飞书文档内容失败: " + e.getMessage(), e);
        }
    }

    /**
     * 提取文档ID（兼容所有飞书URL格式）
     */
    private String extractDocId(String url) {
        if (url == null || url.isEmpty()) {
            log.error("URL为空，无法提取文档ID");
            return null;
        }

        try {
            // 支持的URL格式匹配
            String[] patterns = {
                    "docx/([a-zA-Z0-9]+)",          // https://xxx.feishu.cn/docx/xxx
                    "docs/([a-zA-Z0-9]+)",          // https://xxx.feishu.cn/docs/xxx
                    "file_token=([a-zA-Z0-9]+)",    // ?file_token=xxx
                    "open_file=([a-zA-Z0-9]+)",     // ?open_file=xxx
                    "[^/]+/([a-zA-Z0-9]+)$"         // 兜底：最后一个/后的字符串
            };

            for (String patternStr : patterns) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternStr);
                java.util.regex.Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                    String id = matcher.group(1);
                    log.info("匹配格式[{}]，提取到ID: {}", patternStr, id);
                    return id;
                }
            }

            log.warn("所有格式匹配失败，URL: {}", url);
            return null;

        } catch (Exception e) {
            log.error("提取文档ID失败", e);
            return null;
        }
    }

    /**
     * 初始化飞书SDK客户端
     */
    private Client getFeishuClient() {
        if (feishuClient != null) {
            return feishuClient;
        }

        clientLock.lock();
        try {
            if (feishuClient == null) {
                // 构建飞书客户端（官方SDK方式）
                feishuClient = Client.newBuilder(appId, appSecret)
                        .build();
                log.info("飞书SDK客户端初始化成功");
            }
            return feishuClient;
        } finally {
            clientLock.unlock();
        }
    }

    /**
     * 获取文档原始内容（仅保留核心接口，无无效依赖）
     */
    private String getDocumentRawContent(Client client, String fileToken) {
        try {
            // 创建获取文档原始内容请求
            RawContentDocumentReq req = RawContentDocumentReq.newBuilder()
                    .documentId(fileToken)  // 直接使用提取的ID作为documentId
                    .lang(0)                // 语言：0-中文，1-英文
                    .build();

            // 自定义请求选项（超时时间）
//            RequestOptions options = RequestOptions.newBuilder()
//                    .timeout(30_000)  // 30秒超时
//                    .build();

            // 调用官方SDK核心接口
//            RawContentDocumentResp resp = client.docx().v1().document().rawContent(req, options);
            RawContentDocumentResp resp = client.docx().v1().document().rawContent(req);
            // 处理服务端错误
            if (!resp.success()) {
                String errorMsg = String.format(
                        "获取文档内容失败 - code:%s, msg:%s, reqId:%s, resp:%s",
                        resp.getCode(), resp.getMsg(), resp.getRequestId(),
                        Jsons.createGSON(true, false).toJson(
                                JsonParser.parseString(
                                        new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)
                                )
                        )
                );
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            // 返回文档内容
            if (resp.getData() != null) {
                return "飞书文档内容：\n" + Jsons.DEFAULT.toJson(resp.getData());
            } else {
                return "文档内容为空";
            }

        } catch (Exception e) {
            log.error("获取文档原始内容失败，file_token: {}", fileToken, e);
            throw new RuntimeException("调用飞书SDK失败: " + e.getMessage());
        }
    }

    /**
     * 备用Token缓存类（无实际使用，仅兼容代码结构）
     */
    @Getter
    @Setter
    private static class TokenCache {
        private String accessToken;
        private long expireTime;

        public boolean isValid() {
            return accessToken != null && !accessToken.isEmpty()
                    && System.currentTimeMillis() < expireTime;
        }
    }
}