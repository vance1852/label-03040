# APK Signer - Android 应用签名平台

## How to Run

### 使用 Docker Compose（推荐）

```bash
# 启动所有服务
docker-compose up --build -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

### 手动启动

1. 启动 MySQL 并执行建表脚本：
```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

2. 启动后端：
```bash
cd backend
mvn spring-boot:run
```

3. 启动前端：
```bash
cd frontend-admin
npm install
npm run dev
```

## Services

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost:8081 | APK签名平台主页 |
| 后端API | http://localhost:8080 | REST API 服务 |
| MySQL | localhost:3306 | 数据库 (apk_signer) |

## 测试账号

本项目无需登录认证，直接访问即可使用。

## 题目内容

设计并开发一个功能完善的Android应用签名网站，该网站应满足以下具体要求：

1. 核心功能实现：
   - 提供多种Android应用签名类型选择功能，至少包含Debug签名、Release签名、V1签名、V2签名及V3签名选项，并清晰展示各签名类型的特点与适用场景
   - 实现文件上传功能，支持通过点击"选择文件"按钮上传APK文件，同时支持拖放文件至指定区域完成上传
   - 提供签名后的APK文件下载功能，支持直接下载和扫码下载两种方式

2. 用户界面与交互：
   - 设计直观友好的用户操作流程，包含文件上传区、签名类型选择区、签名参数配置区和结果展示区
   - 实现上传进度显示、签名状态提示和操作成功/失败的视觉反馈
   - 确保界面响应式设计，适配桌面端和移动端不同屏幕尺寸

3. 技术实现要求：
   - 前端采用现代Web技术栈构建（Vue 3 + Ant Design Vue）
   - 后端实现安全可靠的签名服务（Spring Boot 3 + MyBatis-Plus）
   - 实现文件上传大小限制（不超过200MB）和格式验证（仅允许.apk文件）
   - 采用安全的文件处理机制，设置24小时文件自动清理策略

4. 安全与性能：
   - 全局异常处理，AOP日志记录
   - 参数校验（@Valid）
   - 文件格式和大小验证

5. 附加功能：
   - 签名历史记录功能
   - 批量签名功能
   - 签名参数自定义配置（密钥别名、有效期、密码等）
   - 扫码下载（QR Code）
