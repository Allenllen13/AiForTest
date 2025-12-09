package com.example.dto;

import com.example.model.TestCase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResponse {
    private boolean success;
    private String message;
    private List<TestCase> testCases;

    public static GenerateResponse success(List<TestCase> testCases) {
        return new GenerateResponse(true, "生成成功", testCases);
    }

    public static GenerateResponse error(String message) {
        return new GenerateResponse(false, message, null);
    }
}

