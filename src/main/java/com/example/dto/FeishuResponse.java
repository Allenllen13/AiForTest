package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeishuResponse {
    private boolean success;
    private String message;
    private String content;

    public static FeishuResponse success(String content) {
        return new FeishuResponse(true, "获取成功", content);
    }

    public static FeishuResponse error(String message) {
        return new FeishuResponse(false, message, null);
    }
}

