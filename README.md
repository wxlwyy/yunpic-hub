# 云图工坊

> 一个功能完善的图片管理 SaaS 平台，支持图片上传、在线编辑、空间管理、智能分析等功能。

## 项目简介

云图片是一个前后端分离的图片管理系统，提供图片存储、分类、编辑、分享等功能，支持多用户空间隔离和 VIP 会员体系。

## 技术栈

### 后端
- **框架**: Spring Boot 2.7.6
- **数据库**: MySQL + Redis
- **权限**: Sa-Token
- **对象存储**: 腾讯云 COS
- **分库分表**: ShardingSphere
- **本地缓存**: Caffeine
- **高性能队列**: Disruptor
- **实时通信**: WebSocket

### 前端
- **框架**: Vue 3 + TypeScript
- **UI 组件**: Ant Design Vue
- **状态管理**: Pinia
- **构建工具**: Vite
- **数据可视化**: ECharts
- **图片处理**: vue-cropper

## 核心功能

- 图片上传（URL/本地文件）
- 在线图片编辑（裁剪、滤镜、扩图等）
- 空间管理（多空间隔离、权限控制）
- 智能分析（图片分类、标签提取）
- VIP 会员体系
- 用户中心

## 快速启动

### 后端启动
```bash
cd backend-sc
mvn spring-boot:run
```

### 前端启动
```bash
cd frontend-sc
npm install
npm run dev
```

## 项目结构

```
yun-picture/
├── backend-sc/          # 后端服务
│   ├── src/
│   ├── sql/             # 数据库脚本
│   └── pom.xml
├── frontend-sc/         # 前端应用
│   ├── src/
│   └── package.json
└── README.md
```

## 开发者

- 开发者: wxlwyy
