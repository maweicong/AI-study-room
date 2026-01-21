# 智能考试系统 - 服务端

基于 Spring Boot 构建的智能考试系统后端服务，支持在线考试、智能组卷、AI辅助出题等功能。

## 项目概述

**项目名称**: 智能考试系统服务端  
**技术栈**: Java 17, Spring Boot 3.0.5, MyBatis-Plus, MySQL, Redis, MinIO, Kimi AI  
**核心功能**: 在线考试管理、智能组卷、AI题目生成、考试记录统计

## 项目架构

- **后端框架**: Spring Boot 3.0.5
- **数据访问层**: MyBatis-Plus 3.5.3.1
- **数据库**: MySQL 8.0.33
- **缓存**: Redis
- **文件存储**: MinIO
- **AI服务**: Kimi AI (Moonshot API)
- **文档**: Knife4j (Swagger UI)

## 功能模块

### 1. 轮播图管理模块
- [上传轮播图图片](src/main/java/com/mwc/exam/controller/BannerController.java) - 支持图片上传至 MinIO
- [获取启用的轮播图](src/main/java/com/mwc/exam/controller/BannerController.java) - 供前台首页展示
- [获取所有轮播图](src/main/java/com/mwc/exam/controller/BannerController.java) - 管理后台使用
- [轮播图 CRUD 操作](src/main/java/com/mwc/exam/controller/BannerController.java) - 增删改查功能
- [切换轮播图状态](src/main/java/com/mwc/exam/controller/BannerController.java) - 启用/禁用轮播图

### 2. 题目类别管理模块
- [题目分类管理](src/main/java/com/mwc/exam/controller/CategoryController.java) - 支持层级分类结构
- [分类题目数量统计](src/main/java/com/mwc/exam/service/impl/CategoryServiceImpl.java) - 统计各分类下的题目数量
- [树状结构展示](src/main/java/com/mwc/exam/service/impl/CategoryServiceImpl.java) - 层级分类展示

### 3. 题目管理模块
- [题目分页查询](src/main/java/com/mwc/exam/controller/QuestionController.java) - 支持多条件筛选和分页
- [题目详情查询](src/main/java/com/mwc/exam/controller/QuestionController.java) - 包含选项和答案信息
- [题目添加/修改/删除](src/main/java/com/mwc/exam/controller/QuestionController.java) - 支持选择题、判断题、简答题
- [热门题目获取](src/main/java/com/mwc/exam/controller/QuestionController.java) - 基于 Redis 热度统计

### 4. 题目批量管理模块
- [Excel模板下载](src/main/java/com/mwc/exam/controller/QuestionBatchController.java) - 题目导入模板
- [Excel数据预览](src/main/java/com/mwc/exam/controller/QuestionBatchController.java) - 导入前数据预览
- [批量导入题目](src/main/java/com/mwc/exam/controller/QuestionBatchController.java) - 支持失败重试机制
- [AI智能生成题目](src/main/java/com/mwc/exam/controller/QuestionBatchController.java) - 集成 Kimi AI 模型

### 5. 试卷信息模块
- [试卷条件查询](src/main/java/com/mwc/exam/controller/PaperController.java) - 支持状态和名称筛选
- [试卷详情查询](src/main/java/com/mwc/exam/controller/PaperController.java) - 包含完整的题目信息
- [手动组卷功能](src/main/java/com/mwc/exam/controller/PaperController.java) - 自定义题目组合
- [智能组卷功能](src/main/java/com/mwc/exam/controller/PaperController.java) - 按规则自动组卷
- [试卷状态管理](src/main/java/com/mwc/exam/controller/PaperController.java) - 草稿/发布/归档状态

### 6. 考试记录模块
- [考试记录分页查询](src/main/java/com/mwc/exam/controller/ExamRecordController.java) - 后台管理数据源
- [考试记录详情](src/main/java/com/mwc/exam/controller/ExamRecordController.java) - 包含试卷快照和答题详情
- [考试排行榜](src/main/java/com/mwc/exam/controller/ExamRecordController.java) - 按分数和用时排序

### 7. 用户管理模块
- [用户登录注册](src/main/java/com/mwc/exam/controller/UserController.java) - 用户认证功能
- [用户信息管理](src/main/java/com/mwc/exam/controller/UserController.java) - 用户资料维护

## AI 功能特色

### 1. AI 题目生成
- 根据指定主题、类型、难度智能生成题目
- 支持选择题、判断题、简答题多种题型
- 提供详细的题目解析

### 2. AI 题目评分
- 自动评估简答题答案质量
- 提供分数、反馈和扣分原因
- 智能判断答案准确性

### 3. AI 考试总结
- 生成个性化的考试总评
- 提供针对性学习建议
- 指出优势和不足之处

## 技术特性

### 数据持久层
- **MyBatis-Plus**: ORM 框架，提供便捷的 CRUD 操作
- **逻辑删除**: 统一的数据安全删除机制
- **自动填充**: 创建时间和更新时间自动维护

### 文件存储
- **MinIO**: 分布式对象存储，支持文件上传和访问

### 缓存机制
- **Redis**: 热门题目统计和缓存加速

### AI 集成
- **Kimi AI**: 题目智能生成功能
- **WebClient**: HTTP 客户端，调用外部 AI 服务

### 异常处理
- **全局异常处理器**: 统一异常捕获和响应

### 文档
- **Knife4j**: 美观的 API 文档界面

## 环境配置

### 系统要求
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Redis
- MinIO
- Kimi AI API Key

### 配置文件
主要配置在 `src/main/resources/application.yml` 中：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/exam_system_new
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: exam-system

kimi:
  api:
    url: https://api.moonshot.cn/v1/chat/completions
    api-key: your_api_key
    model: moonshot-v1-128k
    max-tokens: 2000
    temperature: 0.3
```

## 项目亮点

1. **智能化程度高**: 集成 AI 题目生成功能和智能评分
2. **数据安全性强**: 采用逻辑删除机制，保护数据完整性
3. **性能优化**: Redis 缓存、分页查询优化、数据库索引优化
4. **扩展性强**: 模块化设计，易于功能扩展
5. **用户体验佳**: 支持树状分类、热门题目、AI个性化建议等功能
6. **现代化架构**: 基于 Spring Boot 3 的现代化微服务架构
7. **完善的文档**: 集成 Knife4j 提供清晰的 API 文档

## 部署说明

1. 确保安装 Java 17+, Maven, MySQL, Redis, MinIO
2. 创建 MySQL 数据库并执行初始化脚本
3. 配置 application.yml 中的各项服务连接信息
4. 运行 Maven 编译打包命令
5. 启动应用服务

```bash
mvn clean package
java -jar target/exam_system_server_online-1.0-SNAPSHOT.jar
```

## API 文档

启动服务后访问 `http://localhost:8080/doc.html` 查看交互式 API 文档。

## 贡献指南

欢迎提交 Issue 和 Pull Request 来改进项目。

## 许可证

本项目仅供学习参考使用。
