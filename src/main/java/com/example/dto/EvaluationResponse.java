package com.example.dto;

import com.example.model.EvaluationScore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评测响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponse {
    private boolean success;
    private String message;
    
    /**
     * 整体评测结果（对整个测试用例集合的评分）
     */
    private EvaluationScore score;
    
    public static EvaluationResponse success(EvaluationScore score) {
        EvaluationResponse response = new EvaluationResponse();
        response.setSuccess(true);
        response.setMessage("评测成功");
        response.setScore(score);
        return response;
    }
    
    public static EvaluationResponse error(String message) {
        EvaluationResponse response = new EvaluationResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}

