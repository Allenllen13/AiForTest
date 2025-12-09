package com.example.dto;

import com.example.model.TestCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评测请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRequest {
    /**
     * PRD内容
     */
    @NotBlank(message = "PRD内容不能为空")
    private String prd;
    
    /**
     * 待评测的测试用例列表
     */
    @NotEmpty(message = "测试用例列表不能为空")
    private List<TestCase> testCases;
}

