package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {
    private String title;
    private String precondition;
    private Object steps; // 可以是String或List<String>
    private String expectedResult;
}

