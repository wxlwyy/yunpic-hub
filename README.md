# 云图工坊

## 项目简介

基于 Spring Boot + Redis + COS + AI + WebSocket + Vue 3 + Ant Design 的智能协同云图库平台。分为公共图库、私有图库和团队共享图库三大模块。用户可在平台公开上传和检索图片；管理员可以上传、审核和管理分析图片。用户可将图片上传至私有空间进行批量管理、多维检索、编辑和分析；用户还可通过兑换码升级为会员角色使用 AI 扩图、通过图片链接上传图片等高级功能。用户还可开通团队空间并邀请成员，共享和实时协同编辑图片。

项目已部署上线，应用场景广泛，可作为公开素材网站、个人相册、企业素材库等。

## 技术栈

### 后端
- **框架**: Spring Boot 2.7.6 + MyBatis-Plus
- **数据库**: MySQL + Redis
- **对象存储**: 腾讯云 COS
- **本地缓存**: Caffeine
- **AI 服务**: 阿里云 AI（图片扩图）
- **高性能队列**: Disruptor
- **实时通信**: WebSocket

### 前端
- **框架**: Vue 3 + TypeScript
- **UI 组件**: Ant Design Vue
- **状态管理**: Pinia
- **构建工具**: Vite
- **数据可视化**: ECharts

## 核心功能

- 图片上传（本地文件 / URL / 批量抓取）
- AI 智能扩图
- 以图搜图 + 颜色检索
- 标签分类体系（热门标签、动态管理）
- 空间管理（公共图库 / 私有空间 / 团队协作）
- VIP 会员体系（兑换码机制）
- 图片审核流程
- 空间数据分析（ECharts 可视化）

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
