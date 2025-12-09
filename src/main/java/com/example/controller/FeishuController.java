package com.example.controller;

import com.example.dto.FeishuRequest;
import com.example.dto.FeishuResponse;
import com.example.service.FeishuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feishu")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FeishuController {

    private final FeishuService feishuService;

    @PostMapping("/fetch")
    public ResponseEntity<FeishuResponse> fetchDocument(@Valid @RequestBody FeishuRequest request) {
        try {
            String content = feishuService.fetchDocumentContent(request.getUrl());
            return ResponseEntity.ok(FeishuResponse.success(content));
        } catch (Exception e) {
            return ResponseEntity.ok(FeishuResponse.error("获取文档内容失败: " + e.getMessage()));
        }
    }
}

