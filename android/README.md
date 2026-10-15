# Android 客户端

源码版本：3.2.6（versionCode 53），包名 `cn.gdcp.timetable`。

客户端不内置个人课表，支持教务个人课表导入、JSON 备份、自建课程、日/周视图和桌面小部件。

## 构建

需要 JDK 17 和 Android SDK 35：

```bash
./gradlew assembleRelease testDebugUnitTest lintRelease
```

Release 默认未签名。正式发布应使用维护者自己的签名密钥；`*.jks`、`*.keystore`、`keystore.properties` 和 `signing.properties` 已被忽略，禁止提交密钥或密码。

## 隐私和许可

登录发生在学校官方 WebView 页面，课程数据仅保存在本机。详见根目录 [PRIVACY.md](../PRIVACY.md)。

项目原创部分采用 `AGPL-3.0-only`。Miuix、Compose、AndroidX 和 Haze 等第三方组件保留各自许可证；完整说明见 [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) 和 `app/src/main/assets/THIRD_PARTY_NOTICES.txt`。

前台自动检查、手动刷新及会话管理见 [JW-REFRESH.md](JW-REFRESH.md)。
