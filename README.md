# PRD测试用例生成器

基于Spring Boot和AI的测试用例自动生成系统，支持从PRD（产品需求文档）自动生成结构化的测试用例。

## 功能特性

- ✅ **PRD文本输入**：支持直接粘贴PRD全文内容
- ✅ **飞书文档集成**：支持通过飞书文档链接获取内容（需要配置飞书API）
- ✅ **智能测试用例生成**：基于PRD内容自动生成测试用例
- ✅ **结构化展示**：以表格形式展示测试用例（标题-前置条件-操作步骤-预期结果）
- ✅ **导出功能**：支持导出测试用例为JSON格式
- ✅ **现代化UI**：美观易用的前端界面

## 技术栈

- **后端**：Spring Boot 3.1.5
- **前端**：HTML5 + CSS3 + JavaScript (Vanilla JS)
- **构建工具**：Maven
- **Java版本**：17

## 项目结构

```
AiForTest/
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── AiForTestApplication.java      # 主应用类
│   │   │   ├── controller/                    # 控制器层
│   │   │   │   ├── TestCaseController.java   # 测试用例生成接口
│   │   │   │   └── FeishuController.java     # 飞书文档接口
│   │   │   ├── service/                       # 服务层
│   │   │   │   ├── TestCaseService.java      # 测试用例生成服务
│   │   │   │   └── FeishuService.java        # 飞书文档服务
│   │   │   ├── model/                         # 实体类
│   │   │   │   └── TestCase.java             # 测试用例模型
│   │   │   ├── dto/                           # 数据传输对象
│   │   │   │   ├── GenerateRequest.java
│   │   │   │   ├── GenerateResponse.java
│   │   │   │   ├── FeishuRequest.java
│   │   │   │   └── FeishuResponse.java
│   │   │   └── config/                        # 配置类
│   │   │       └── WebConfig.java
│   │   └── resources/
│   │       ├── static/                        # 静态资源
│   │       │   ├── index.html                # 前端页面
│   │       │   ├── styles.css                # 样式文件
│   │       │   └── app.js                    # 前端逻辑
│   │       └── application.yml               # 应用配置
│   └── test/                                  # 测试代码
├── pom.xml                                    # Maven配置
└── README.md                                  # 项目说明
```

## 快速开始

### 1. 环境要求

- JDK 17 或更高版本
- Maven 3.6 或更高版本

### 2. 编译运行

```bash
# 进入项目目录
cd AiForTest

# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run
```

### 3. 访问应用

启动成功后，在浏览器中访问：`http://localhost:8080`

## 使用说明

### 方式一：粘贴PRD文本

1. 点击"粘贴PRD文本"标签页
2. 在文本框中粘贴PRD全文内容
3. 点击"生成测试用例"按钮
4. 系统会自动解析PRD并生成测试用例
5. 在下方表格中查看生成的测试用例

### 方式二：使用飞书文档链接

1. 点击"飞书文档链接"标签页
2. 输入飞书文档的完整URL
3. 点击"获取文档内容"按钮（需要配置飞书API）
4. 文档内容会自动填充到文本区域
5. 点击"生成测试用例"按钮

### 测试用例格式

生成的测试用例包含以下字段：

- **标题**：测试用例的名称
- **前置条件**：执行测试前需要满足的条件
- **操作步骤**：详细的测试操作步骤（支持多步骤）
- **预期结果**：测试执行后预期的结果

### 导出功能

- **导出为JSON**：将测试用例导出为JSON文件
- **复制到剪贴板**：将测试用例复制为文本格式

## 配置说明

### 飞书API配置（可选）

如果需要使用飞书文档功能，需要在 `application.yml` 中配置：

```yaml
feishu:
  app：
    id: your-app-id
    secret: your-app-secret
  enabled: true
```

或者在环境变量中设置：
```bash
export FEISHU_APP_ID=your-app-id
export FEISHU_APP_SECRET=your-app-secret
```

### LLM配置（可选）

当前版本使用规则引擎生成测试用例。如需集成真实的AI模型（如OpenAI、Claude等），可以配置：

```yaml
llm:
  provider: openai
  api-key: your-api-key
  model: gpt-3.5-turbo
  enabled: true
```

## API接口

### 生成测试用例

**POST** `/api/testcase/generate`

请求体：
```json
{
  "prd": "PRD全文内容..."
}
```

响应：
```json
{
  "success": true,
  "message": "生成成功",
  "testCases": [
    {
      "title": "测试用例标题",
      "precondition": "前置条件",
      "steps": ["步骤1", "步骤2"],
      "expectedResult": "预期结果"
    }
  ]
}
```

### 获取飞书文档

**POST** `/api/feishu/fetch`

请求体：
```json
{
  "url": "https://example.feishu.cn/docs/..."
}
```

响应：
```json
{
  "success": true,
  "message": "获取成功",
  "content": "文档内容..."
}
```

## 开发计划

### 已完成功能
- ✅ PRD文本输入和解析
- ✅ 测试用例生成（基于规则引擎）
- ✅ 测试用例表格展示
- ✅ 导出功能
- ✅ 飞书文档API完整集成
- ✅ 集成真实的LLM API（采用火山引擎）

### 待实现功能
- [ ] 支持UI图和流程图的解析
- [ ] 思维导图形式的测试用例展示
- [ ] 测试用例编辑功能
- [ ] 测试用例模板自定义


