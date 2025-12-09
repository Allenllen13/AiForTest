package com.example.controller;

import com.example.dto.EvaluationRequest;
import com.example.dto.EvaluationResponse;
import com.example.dto.GenerateRequest;
import com.example.dto.GenerateResponse;
import com.example.service.EvaluationService;
import com.example.service.TestCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/testcase")
//自动生成构造方法
@RequiredArgsConstructor
//允许「所有前端域名」跨域访问
@CrossOrigin(origins = "*")

public class TestCaseController {

//    private final TestCaseServiceOld testCaseService;
    private final TestCaseService testCaseService;
    private final EvaluationService evaluationService;

    @PostMapping("/generate")
    public ResponseEntity<GenerateResponse> generateTestCases(@Valid @RequestBody GenerateRequest request) {
        try{
            var testCases = testCaseService.generateTestCases(request.getPrd());
            return ResponseEntity.ok(GenerateResponse.success(testCases));
        }catch(Exception e){
            return ResponseEntity.ok(GenerateResponse.error("生成测试用例失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/evaluate")
    public ResponseEntity<EvaluationResponse> evaluateTestCases(@Valid @RequestBody EvaluationRequest request) {
        try {
            var score = evaluationService.evaluateTestCases(request.getPrd(), request.getTestCases());
            return ResponseEntity.ok(EvaluationResponse.success(score));
        } catch (Exception e) {
            return ResponseEntity.ok(EvaluationResponse.error("评测失败: " + e.getMessage()));
        }
    }
}

