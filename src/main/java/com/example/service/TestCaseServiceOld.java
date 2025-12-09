package com.example.service;

import com.example.model.TestCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestCaseServiceOld {

    /**
     * 根据PRD内容生成测试用例
     * 这里使用规则引擎生成测试用例，实际项目中可以集成OpenAI、Claude等LLM API
     */
    public List<TestCase> generateTestCases(String prd) {
        log.info("开始生成测试用例，PRD长度: {}", prd.length());
        
        List<TestCase> testCases = new ArrayList<>();
        
        // 解析PRD，提取功能点
        List<String> features = extractFeatures(prd);
        
        // 为每个功能点生成测试用例
        for (String feature : features) {
            List<TestCase> featureTestCases = generateTestCasesForFeature(feature);
            testCases.addAll(featureTestCases);
        }
        
        // 如果没有提取到功能点，生成通用测试用例
        if (testCases.isEmpty()) {
            testCases = generateDefaultTestCases(prd);
        }
        
        log.info("生成测试用例数量: {}", testCases.size());
        return testCases;
    }

    /**
     * 从PRD中提取功能点
     */
    private List<String> extractFeatures(String prd) {
        List<String> features = new ArrayList<>();
        
        // 使用正则表达式提取功能点
        // 匹配常见的功能描述模式
        Pattern pattern = Pattern.compile(
            "(?:功能|需求|特性|模块)[：:](.*?)(?=\\n|$|(?:功能|需求|特性|模块)[：:])",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        
        Matcher matcher = pattern.matcher(prd);
        while (matcher.find()) {
            String feature = matcher.group(1).trim();
            if (!feature.isEmpty() && feature.length() > 5) {
                features.add(feature);
            }
        }
        
        // 如果没有匹配到，尝试按段落分割
        if (features.isEmpty()) {
            String[] paragraphs = prd.split("\n\n");
            for (String para : paragraphs) {
                para = para.trim();
                if (para.length() > 20 && para.length() < 500) {
                    features.add(para);
                }
            }
        }
        
        return features.size() > 10 ? features.subList(0, 10) : features;
    }

    /**
     * 为单个功能生成测试用例
     */
    private List<TestCase> generateTestCasesForFeature(String feature) {
        List<TestCase> testCases = new ArrayList<>();
        
        // 正常流程测试用例
        TestCase normalCase = new TestCase();
        normalCase.setTitle("验证" + extractFeatureName(feature) + "正常流程");
        normalCase.setPrecondition("系统已登录，具备相应权限");
        normalCase.setSteps(extractSteps(feature, true));
        normalCase.setExpectedResult("功能正常执行，结果符合预期");
        testCases.add(normalCase);
        
        // 异常流程测试用例
        TestCase exceptionCase = new TestCase();
        exceptionCase.setTitle("验证" + extractFeatureName(feature) + "异常处理");
        exceptionCase.setPrecondition("系统已登录");
        exceptionCase.setSteps(extractSteps(feature, false));
        exceptionCase.setExpectedResult("系统正确提示错误信息，功能未执行");
        testCases.add(exceptionCase);
        
        // 边界值测试用例
        TestCase boundaryCase = new TestCase();
        boundaryCase.setTitle("验证" + extractFeatureName(feature) + "边界值");
        boundaryCase.setPrecondition("系统已登录，准备边界值数据");
        boundaryCase.setSteps(List.of(
            "输入边界值数据",
            "执行功能操作",
            "观察系统响应"
        ));
        boundaryCase.setExpectedResult("系统正确处理边界值，无异常");
        testCases.add(boundaryCase);
        
        return testCases;
    }

    /**
     * 提取功能名称
     */
    private String extractFeatureName(String feature) {
        // 提取前30个字符作为功能名称
        String name = feature.substring(0, Math.min(30, feature.length()));
        if (name.length() < feature.length()) {
            name += "...";
        }
        return name;
    }

    /**
     * 提取操作步骤
     */
    private Object extractSteps(String feature, boolean isNormal) {
        List<String> steps = new ArrayList<>();
        
        if (isNormal) {
            steps.add("打开相关功能页面");
            steps.add("输入有效数据");
            steps.add("点击提交/确认按钮");
            steps.add("验证操作结果");
        } else {
            steps.add("打开相关功能页面");
            steps.add("输入无效数据（如空值、特殊字符等）");
            steps.add("点击提交/确认按钮");
            steps.add("验证错误提示");
        }
        
        // 如果feature中包含具体的步骤描述，可以解析出来
        Pattern stepPattern = Pattern.compile("(?:步骤|操作)[：:]?\\s*([^。；\\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher stepMatcher = stepPattern.matcher(feature);
        int stepIndex = 0;
        while (stepMatcher.find() && stepIndex < 3) {
            String step = stepMatcher.group(1).trim();
            if (step.length() > 5 && step.length() < 100) {
                if (steps.size() > stepIndex) {
                    steps.set(stepIndex, step);
                } else {
                    steps.add(step);
                }
                stepIndex++;
            }
        }
        
        return steps.size() == 1 ? steps.get(0) : steps;
    }

    /**
     * 生成默认测试用例（当无法解析PRD时）
     */
    private List<TestCase> generateDefaultTestCases(String prd) {
        List<TestCase> testCases = new ArrayList<>();
        
        // 基础功能测试
        TestCase case1 = new TestCase();
        case1.setTitle("基础功能验证");
        case1.setPrecondition("系统正常运行，用户已登录");
        case1.setSteps(List.of("访问功能页面", "执行基本操作", "验证结果"));
        case1.setExpectedResult("功能正常执行");
        testCases.add(case1);
        
        // 数据验证测试
        TestCase case2 = new TestCase();
        case2.setTitle("数据输入验证");
        case2.setPrecondition("系统正常运行");
        case2.setSteps(List.of("输入各种数据类型", "提交数据", "验证系统响应"));
        case2.setExpectedResult("系统正确验证数据格式");
        testCases.add(case2);
        
        // 权限验证测试
        TestCase case3 = new TestCase();
        case3.setTitle("权限验证");
        case3.setPrecondition("使用不同权限的账户");
        case3.setSteps(List.of("尝试访问功能", "执行操作", "验证权限控制"));
        case3.setExpectedResult("权限控制正确生效");
        testCases.add(case3);
        
        return testCases;
    }
}

