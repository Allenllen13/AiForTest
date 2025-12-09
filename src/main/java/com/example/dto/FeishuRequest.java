package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeishuRequest {
    @NotBlank(message = "飞书文档链接不能为空")
    private String url;
}

