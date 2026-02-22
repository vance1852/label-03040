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

## 安全机制

### 密码加密存储

签名参数中的 `storePassword` 和 `keyPassword` 使用 AES-128-GCM 加密后存入数据库，不存储明文。

- 加密密钥通过环境变量 `ENCRYPT_KEY` 注入（必须为 16 字节）
- 启动时 `SecurityConfig` 校验密钥是否配置，未配置则阻止启动
- 每次加密使用随机 IV，相同明文产生不同密文

### API 响应脱敏

- 密码字段统一返回 `******`，不回显明文
- `filePath` / `signedFilePath` 等内部路径不返回给前端
- 异常信息经过清洗，不暴露内部路径、命令输出等敏感信息

### 环境变量配置

在 `docker-compose.yml` 中已配置默认值，生产环境请替换：

```bash
# 自定义加密密钥（必须 16 字节）
ENCRYPT_KEY=YourCustomKey16!  docker-compose up --build -d
```

## 自动化测试

项目包含 25 个自动化测试用例，覆盖加密工具、文件服务和签名接口。

### 运行方式

```bash
# 使用测试专用 Dockerfile（内含 H2 内存数据库，无需 MySQL）
docker build -f backend/Dockerfile.test -t apk-signer-test backend/
```

### 测试覆盖

| 测试类 | 用例数 | 覆盖范围 |
|--------|--------|----------|
| CryptoUtilTest | 8 | 加解密、随机IV、空值处理、Unicode、篡改检测、密钥校验、脱敏 |
| FileStorageServiceTest | 7 | 上传、格式校验、空文件、路径生成、清理 |
| SignControllerTest | 10 | 上传接口、参数校验、404、历史查询、密码脱敏、路径脱敏 |

## 如何测试

### 前置条件

- 已通过 `docker-compose up --build -d` 启动所有服务
- 确认三个容器均正常运行：

```bash
docker-compose ps
```

预期输出：

| 容器名 | 状态 |
|--------|------|
| apk-signer-mysql | Up (healthy) |
| apk-signer-backend | Up |
| apk-signer-frontend | Up |

### 一、界面测试（浏览器）

#### 1. 访问首页

打开浏览器访问 http://localhost:8081 ，应看到：
- 顶部蓝色渐变 Hero Banner，标题"Android APK 签名平台"
- 步骤指引条（上传文件 → 选择签名类型 → 配置参数 → 下载结果）
- 文件上传区（支持点击和拖拽）
- 5种签名类型选择卡片（Debug / Release / V1 / V2 / V3）
- 签名参数配置表单
- 顶部导航栏包含"签名工具"和"历史记录"两个入口

#### 2. 上传 APK 文件

1. 准备一个 `.apk` 文件（可使用任意 Android APK，或用下方命令生成测试文件）
2. 点击上传区域或将文件拖拽到上传区
3. 验证：
   - 上传进度条正常显示
   - 上传成功后文件出现在"已上传文件"列表中，状态为"待签名"
   - 上传非 `.apk` 文件时弹出错误提示"仅支持 .apk 格式文件"
   - 上传超过 200MB 的文件时弹出大小限制提示

#### 3. 选择签名类型并执行签名

1. 点击选择一种签名类型（如 "Release 签名"），卡片高亮显示
2. 可选：修改签名参数（密钥别名、有效期、密码）
3. 点击"开始签名"按钮
4. 验证：
   - 按钮显示 Loading 状态
   - 签名成功后出现绿色成功结果，包含"直接下载"和"扫码下载"按钮
   - 签名失败时显示红色错误信息和"重新签名"按钮

#### 4. 下载签名文件

- 点击"直接下载"：浏览器开始下载签名后的 APK 文件
- 点击"扫码下载"：弹出二维码弹窗，使用手机扫码可下载

#### 5. 批量签名

1. 连续上传多个 APK 文件
2. 点击"批量签名"按钮
3. 验证所有文件均被签名，列表中状态更新为"已完成"

#### 6. 历史记录

1. 点击顶部导航"历史记录"
2. 验证：
   - 表格展示所有签名记录（文件名、大小、签名类型、状态、时间）
   - 成功的记录可点击"下载"
   - 可勾选多条记录进行"批量删除"
   - 分页功能正常

#### 7. 响应式测试

- 使用浏览器开发者工具（F12）切换到移动端视图
- 验证页面布局自适应，无溢出或错位

### 二、API 接口测试

以下使用 `curl` 命令测试（Linux/Mac），Windows 用户可使用 PowerShell 的 `Invoke-RestMethod` 或 Postman。

#### 1. 生成测试 APK 文件

```bash
# Linux/Mac - 在容器内生成
docker exec apk-signer-backend bash -c \
  "mkdir -p /tmp/testapk && \
   echo '<manifest package=\"com.test\"/>' > /tmp/testapk/AndroidManifest.xml && \
   cd /tmp/testapk && jar cf /tmp/test-app.apk AndroidManifest.xml"

# 从容器复制到本地
docker cp apk-signer-backend:/tmp/test-app.apk ./test-app.apk
```

```powershell
# Windows PowerShell - 在容器内生成并复制
docker exec apk-signer-backend bash -c "mkdir -p /tmp/testapk && echo '<manifest package=""com.test""/>' > /tmp/testapk/AndroidManifest.xml && cd /tmp/testapk && jar cf /tmp/test-app.apk AndroidManifest.xml"
docker cp apk-signer-backend:/tmp/test-app.apk ./test-app.apk
```

#### 2. 上传 APK 文件

```bash
curl -X POST http://localhost:8080/api/sign/upload \
  -F "file=@test-app.apk"
```

预期响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "originalFilename": "test-app.apk",
    "fileSize": 176,
    "status": "PENDING",
    "downloadCode": "xxxxxxxxxxxxxxxx"
  }
}
```

#### 3. 执行签名（V1 签名）

```bash
curl -X POST http://localhost:8080/api/sign/execute \
  -H "Content-Type: application/json" \
  -d '{
    "historyId": 1,
    "signType": "V1",
    "keyAlias": "test-key",
    "validityYears": 10,
    "storePassword": "123456",
    "keyPassword": "123456"
  }'
```

预期响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "status": "SUCCESS",
    "signType": "V1",
    "signedFilename": "signed_test-app.apk"
  }
}
```

#### 4. 执行签名（Debug 签名）

```bash
curl -X POST http://localhost:8080/api/sign/execute \
  -H "Content-Type: application/json" \
  -d '{
    "historyId": 1,
    "signType": "DEBUG",
    "keyAlias": "debug-key",
    "validityYears": 25,
    "storePassword": "android",
    "keyPassword": "android"
  }'
```

#### 5. 批量签名

先上传多个文件获取各自的 `id`，然后：

```bash
curl -X POST http://localhost:8080/api/sign/batch \
  -H "Content-Type: application/json" \
  -d '{
    "historyIds": [1, 2],
    "signType": "DEBUG",
    "keyAlias": "debug-key",
    "validityYears": 25,
    "storePassword": "android",
    "keyPassword": "android"
  }'
```

#### 6. 查询签名状态

```bash
curl http://localhost:8080/api/sign/status/1
```

#### 7. 下载签名后的 APK

```bash
# 直接下载
curl -OJ http://localhost:8080/api/file/download/1

# 通过下载码下载（downloadCode 从上传响应中获取）
curl -OJ http://localhost:8080/api/file/download/code/xxxxxxxxxxxxxxxx
```

#### 8. 获取下载二维码

```bash
curl http://localhost:8080/api/file/qrcode/1
```

预期响应：
```json
{
  "code": 200,
  "data": "data:image/png;base64,iVBORw0KGgo..."
}
```

#### 9. 查询历史记录

```bash
curl "http://localhost:8080/api/history/list?page=1&size=10"
```

#### 10. 删除历史记录

```bash
# 单条删除
curl -X DELETE http://localhost:8080/api/history/1

# 批量删除
curl -X DELETE http://localhost:8080/api/history/batch \
  -H "Content-Type: application/json" \
  -d '[1, 2, 3]'
```

### 三、异常场景测试

#### 1. 文件格式校验

```bash
# 上传非 APK 文件，应返回 400 错误
echo "not an apk" > test.txt
curl -X POST http://localhost:8080/api/sign/upload -F "file=@test.txt"
```

预期响应：`{"code":400,"message":"仅支持 .apk 格式文件"}`

#### 2. 参数校验

```bash
# 缺少必填参数，应返回 400 错误
curl -X POST http://localhost:8080/api/sign/execute \
  -H "Content-Type: application/json" \
  -d '{"historyId": null, "signType": ""}'
```

#### 3. 不存在的记录

```bash
# 查询不存在的 ID，应返回 404
curl http://localhost:8080/api/sign/status/99999
```

预期响应：`{"code":404,"message":"记录不存在"}`

#### 4. 无效下载码

```bash
curl http://localhost:8080/api/file/download/code/invalid_code
```

预期响应：`{"code":404,"message":"下载码无效"}`

### 四、测试结果汇总

| # | 测试项 | 预期结果 |
|---|--------|----------|
| 1 | APK 文件上传 | 返回 200，生成 PENDING 记录 |
| 2 | V1 签名 (jarsigner) | 返回 200，状态变为 SUCCESS |
| 3 | Debug 签名 | 返回 200，使用 debug keystore 签名成功 |
| 4 | Release/V2/V3 签名 | 返回 200，使用 apksigner 签名成功 |
| 5 | 批量签名 | 返回 200，所有文件签名完成 |
| 6 | 直接下载 | 返回文件流，Content-Disposition 包含文件名 |
| 7 | 下载码下载 | 通过 downloadCode 成功下载 |
| 8 | 二维码生成 | 返回 base64 编码的 PNG 图片 |
| 9 | 历史记录查询 | 返回分页数据，按创建时间倒序 |
| 10 | 删除记录 | 记录和关联文件均被删除 |
| 11 | 非 APK 文件拒绝 | 返回 400，提示格式错误 |
| 12 | 参数校验 | 返回 400，提示具体字段错误 |
| 13 | 前端页面访问 | 返回 200，页面正常渲染 |
| 14 | Nginx API 代理 | 通过 8081 端口访问 API 正常 |

</text>
</invoke>

### 五、Windows PowerShell 测试脚本

如果你在 Windows 环境下，可以使用以下 PowerShell 脚本进行完整测试：

```powershell
# 1. 生成测试 APK 并复制到本地
docker exec apk-signer-backend bash -c "mkdir -p /tmp/testapk && echo '<manifest/>' > /tmp/testapk/AndroidManifest.xml && cd /tmp/testapk && jar cf /tmp/test-app.apk AndroidManifest.xml"
docker cp apk-signer-backend:/tmp/test-app.apk ./test-app.apk

# 2. 上传文件
Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$content = New-Object System.Net.Http.MultipartFormDataContent
$fileStream = [System.IO.File]::OpenRead("$(Resolve-Path test-app.apk)")
$fileContent = New-Object System.Net.Http.StreamContent($fileStream)
$fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/octet-stream")
$content.Add($fileContent, "file", "test-app.apk")
$result = $client.PostAsync("http://localhost:8080/api/sign/upload", $content).Result
$body = $result.Content.ReadAsStringAsync().Result
$fileStream.Close(); $client.Dispose()
Write-Host "上传结果: $body"

# 3. 执行签名（将 historyId 替换为上传返回的 id）
$signBody = '{"historyId":1,"signType":"V1","keyAlias":"test-key","validityYears":10,"storePassword":"123456","keyPassword":"123456"}'
$resp = Invoke-RestMethod -Uri "http://localhost:8080/api/sign/execute" -Method POST -ContentType "application/json; charset=utf-8" -Body $signBody
Write-Host "签名结果: $($resp | ConvertTo-Json -Depth 3)"

# 4. 查询历史
$hist = Invoke-RestMethod -Uri "http://localhost:8080/api/history/list?page=1&size=10" -Method GET
Write-Host "历史记录: total=$($hist.data.total)"

# 5. 获取二维码
$qr = Invoke-RestMethod -Uri "http://localhost:8080/api/file/qrcode/1" -Method GET
Write-Host "二维码长度: $($qr.data.Length) chars"
```

### 六、查看后端日志

```bash
# 实时查看后端日志（含 AOP 日志、签名过程日志）
docker logs -f apk-signer-backend

# 查看最近 50 行
docker logs --tail 50 apk-signer-backend
```

日志中可以观察到：
- `[API] SignController.upload` - 文件上传请求日志
- `[API] SignController.execute` - 签名执行日志及耗时
- `开始签名: type=V1` - 签名引擎工作日志
- `签名完成: output=...` - 签名成功日志
- `开始执行过期文件清理任务` - 定时清理任务日志

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
