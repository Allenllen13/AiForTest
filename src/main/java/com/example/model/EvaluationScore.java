package com.example.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测试用例评分模型
 * 包含6个评分维度，每个维度0-5分
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationScore {
    /**
     * 覆盖性（0-5）：完全未覆盖PRD需求=0；完全覆盖对应需求点=5
     */
    private Integer coverage;
    private Integer quality;
    
    /**
     * 正确性（0-5）：步骤、预期与PRD一致，错误越少分数越高
     */
//    private Integer correctness;
//
    /**
     * 逻辑性（0-5）：步骤是否执行得通；是否缺少关键步骤；是否存在不可执行描述
     */
//    private Integer logic;
    
    /**
     * 完整性（0-5）：包含前置、输入、步骤、预期结果为5分
     */
//    private Integer completeness;
    
    /**
     * 精确性（0-5）：预期结果可验证、具体（如"页面跳转成功""提示XXX"）
     */
//    private Integer precision;
    
    /**
     * 非冗余度（0-5）：5=无冗余内容；0=冗余或与PRD无关内容严重
     */
    private Integer nonRedundancy;
    
    /**
     * 计算总分（6个维度的平均值）
     */
    @JsonGetter("totalScore")
//    public Double getTotalScore() {
//        if (coverage == null || correctness == null || logic == null ||
//            completeness == null || precision == null || nonRedundancy == null) {
//            return null;
//        }
//        return (coverage + correctness + logic + completeness + precision + nonRedundancy) / 6.0;
//    }
    public Double getTotalScore() {
        if (coverage == null ||quality == null || nonRedundancy == null) {
            return null;
        }
        return (coverage + quality + nonRedundancy) / 3.0;
    }
}

