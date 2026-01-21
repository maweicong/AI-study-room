# 智能考试系统 - 服务端

基于 Spring Boot 构建的智能考试系统后端服务，支持在线考试、智能组卷、AI辅助出题等功能。

## 项目概述

**项目名称**: 智能考试系统服务端  
**技术栈**: Java, Spring Boot, MyBatis-Plus, MySQL, Redis, MinIO  
**核心功能**: 在线考试管理、智能组卷、AI题目生成、考试记录统计

## 功能模块

### 1. 轮播图管理模块
- [查询所有轮播图](#file)
- [查询激活轮播图](#file)
- [切换轮播图状态](#file)
- [删除轮播图信息](#file)
- [获取轮播图详情](#file)
- [文件上传业务](#file) - 集成 MinIO 文件存储
- [上传轮播图信息](#file)
- [保存轮播图信息](#file)
- [更新轮播图信息](#file)

### 2. 题目类别管理模块
- [查询所有题目分类](#file) - 支持分类及题目数量统计
- [查询题目分类树状结构](#file) - 层级分类展示
- [添加题目分类](#file) - 同父分类下标题唯一性校验
- [更新题目分类](#file)
- [删除题目分类](#file) - 防御性编程，检查关联数据

### 3. 题目管理模块
- [题目分页查询](#file) - 支持多条件筛选和分页
- [题目详情查询](#file) - 包含选项和答案信息
- [题目添加功能](#file) - 支持选择题、判断题、简答题
- [题目修改功能](#file)
- [题目删除功能](#file) - 关联数据检查
- [热门题目获取](#file) - 基于 Redis 热度统计

### 4. 题目批量管理模块
- [Excel模板下载](#file) - 题目导入模板
- [Excel数据预览](#file) - 导入前数据预览
- [批量导入题目](#file) - 支持失败重试机制
- [AI智能生成题目](#file) - 集成 Kimi AI 模型

### 5. 试卷信息模块
- [试卷条件查询](#file) - 支持状态和名称筛选
- [试卷详情查询](#file) - 包含完整的题目信息
- [手动组卷功能](#file) - 自定义题目组合
- [智能组卷功能](#file) - 按规则自动组卷
- [试卷更新功能](#file)
- [试卷状态管理](#file)
- [试卷删除功能](#file)

### 6. 考试记录模块
- [考试记录分页查询](#file) - 后台管理数据源
- [考试记录详情](#file) - 包含试卷快照和答题详情
- [考试记录删除](#file)
- [考试排行榜](#file) - 按分数和用时排序

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

### 数据验证
- **参数校验**: 前后端数据一致性保障

## 配置说明

### 文件上传限制
```yaml
spring:
  servlet:
    multipart:
      max-request-size: 150MB
      max-file-size: 100MB
```


### Jackson 时间格式化
```yaml
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
```


### Kimi AI 配置
```yaml
kimi:
  api:
    model: moonshot-v1-32k
    uri: https://api.moonshot.cn/v1/chat/completions
    api-key: your-api-key
    max-tokens: 2048
    temperature: 0.3
```


### MinIO 配置
```yaml
minio:
  endpoint: http://localhost:9000
  username: your-username
  password: your-password
  bucket-name: your-bucket
```


## 项目亮点

1. **智能化程度高**: 集成 AI 题目生成功能
2. **数据安全性强**: 采用逻辑删除机制
3. **性能优化**: Redis 缓存、分页查询优化
4. **扩展性强**: 模块化设计，易于功能扩展
5. **用户体验佳**: 支持树状分类、热门题目等功能

## 部署说明

本项目为标准 Spring Boot 应用，需配合 MySQL、Redis、MinIO 等服务部署使用。
