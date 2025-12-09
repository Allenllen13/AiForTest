package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测试用例评测结果
 * 包含测试用例和对应的评分
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseEvaluation {
    /**
     * 测试用例
     */
    private TestCase testCase;
    
    /**
     * 评分结果
     */
    private EvaluationScore score;
}

