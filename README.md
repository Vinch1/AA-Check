# AA-Check · 旅行 AA 记账 App

几个人一起旅行时的多人协作记账工具：每人随时记录花销，旅行结束时自动算出每人该 A 多少钱，并给出「谁转给谁多少」的最少转账方案。

## 目录结构

```
AA-Check/
├── backend/     Kotlin + Spring Boot 3 + PostgreSQL 后端
└── android/     Kotlin + Jetpack Compose Android 客户端
```

## 一、启动后端

### 1. 启动 PostgreSQL

需要 Docker Desktop 已启动：

```bash
cd backend
docker compose up -d
```

> `docker-compose.yml` 中的镜像已改用 `docker.m.daocloud.io/library/postgres:16-alpine`，
> 若你的网络可直连 Docker Hub，可改回 `postgres:16-alpine`。

### 2. 启动后端

```bash
cd backend
./gradlew bootRun
```

服务默认跑在 `http://localhost:8080`。首次启动会用 Hibernate 自动建表（`ddl-auto: update`）。

关键配置（`src/main/resources/application.yml`）：

| 项 | 默认值 | 说明 |
|---|---|---|
| `app.jwt.secret` | 示例值 | **生产务必用环境变量覆盖** |
| `app.upload-dir` | `./uploads` | 图片存储目录 |
| 数据库 | `aacheck` / `aacheck123` | 与 docker-compose 一致 |

## 二、构建 & 运行 Android

### 1. 配置后端地址

打开 `android/app/src/main/kotlin/com/aacheck/app/core/AppContainer.kt`：

```kotlin
object ServerConfig {
    var baseUrl: String = "http://10.0.2.2:8080/"   // 模拟器访问宿主机
}
```

- **模拟器**：保持 `10.0.2.2:8080`（模拟器访问 Mac 本机）。
- **真机**：改成 Mac 的局域网 IP，例如 `http://192.168.1.10:8080/`，且手机与 Mac 同一 Wi-Fi。
- 后端无需额外跨域配置（客户端请求，非浏览器）。

### 2. 构建安装

```bash
cd android
./gradlew assembleDebug          # 产物在 app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # 已连接设备/模拟器时直接安装
```

## 三、功能说明

- **账号**：手机号 + 密码注册登录（JWT）。
- **旅行**：创建者建旅行，生成 6 位邀请码 + 二维码，朋友输码/扫码加入。
- **记账**：金额、付款人（默认参与分摊、可取消勾选）、参与人多选、分类（餐饮/交通/门票/住宿/购物/娱乐/其他）、备注、最多 2 张图片。
- **时间线**：按天分组、倒序展示，支持按成员 / 分类过滤。
- **修改与追认**：金额/参与人/分类可被他人修改，**乐观生效**；原作者收到「原始 vs 当前」对比弹窗，可「同意」或「不同意」（标记争议、提示线下沟通）；修改人可「撤回」恢复原状。备注/图片/删除仅作者本人可操作。
- **结算**：创建者结束旅行后（也可随时查看实时结算），给出每人净额、最少转账方案、每人的分类金额组成（垫付/分摊分列）。

## 四、已知取舍（MVP）

- 单币种人民币；均摊（不支持自定义金额）。
- 还款转账在 App 外进行，不记录。
- 不支持踢人 / 退出旅行；图片存本地磁盘。
- 离线仅可浏览（本地缓存），写操作需在线。
- 无系统级推送（通知以 App 内弹窗 + 打开后补弹实现）。

## 五、常见问题

- **构建报 Java 版本错误**：本工程后端/客户端均使用 JDK 21，若本机无 JDK 21 请先安装。
- **Gradle 下载失败**：`android/gradle/wrapper/gradle-wrapper.properties` 与 `backend/...` 已指向腾讯镜像分发地址；依赖仓库用阿里云镜像（见各 `settings.gradle.kts`）。
