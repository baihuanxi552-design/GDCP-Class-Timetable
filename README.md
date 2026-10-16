# 广交个人课表

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](LICENSE)
[![Android build](https://github.com/baihuanxi552-design/GDCP-Class-Timetable/actions/workflows/build-empty.yml/badge.svg)](https://github.com/baihuanxi552-design/GDCP-Class-Timetable/actions/workflows/build-empty.yml)
[![Latest release](https://img.shields.io/github/v/release/baihuanxi552-design/GDCP-Class-Timetable)](https://github.com/baihuanxi552-design/GDCP-Class-Timetable/releases/latest)

面向广东交通职业技术学院的本地课表工具，包含静态网页版和 Android 客户端。仓库、网页和 APK 不内置任何个人课表、账号、密码或教师名单。项目并非学校官方应用，课表结果应以教务系统为准。

- 在线使用：<https://baihuanxi552-design.github.io/GDCP-Class-Timetable/>
- Android 下载：<https://github.com/baihuanxi552-design/GDCP-Class-Timetable/releases/latest>
- 当前已发布版本：网页 3.3.2；Android 3.2.5
- Android 源码版本：3.2.20，支持开启后每次打开应用立即检查、手动刷新；正式版需原签名覆盖安装；CI 另提供与正式版并行安装的验证版。

## 功能

- 日课表、周课表、课程详情和桌面小部件
- 小部件独立选择今天、明天或固定日期，长教室分行／轮播，按实际尺寸排列彩色课程卡片
- 完整欢迎引导、按钮展开、设置完成页及重新引导
- 本机自建课程以及 JSON 备份导入、导出
- 通过学校官方教务页面导入个人课表
- 深色模式、系统字体缩放和减少动态效果支持
- 网页数据保存在浏览器本地；Android 数据保存在应用私有存储

教务系统的页面结构和课表可能变化。解析失败、学期不匹配或登录失效时，不应使用不完整结果覆盖已有数据。Android 开启自动更新后可在前台检查变化，也可在设置立即刷新。登录过期时需重新登录。具体行为见 [android/JW-REFRESH.md](android/JW-REFRESH.md)。

## 隐私与数据

本项目不运营课表后端，也不收集账号密码。Android 登录发生在学校官方页面中；网页版导入结果和备份保存在本机。具体的数据范围、网络请求和删除方式见 [PRIVACY.md](PRIVACY.md)。

提交 Issue、测试文件或 Pull Request 前，请移除姓名、学号、教师、教室、Cookie、ticket、课表和签名密钥等信息。测试数据应使用虚构内容。

## 本地运行

网页版是静态文件，无构建步骤：

```bash
python -m http.server 8000
```

然后访问 <http://localhost:8000>。

Android 构建与 Miuix 页面测试需要 JDK 21（应用字节码目标仍为 Java 17） 和 Android SDK 35：

```bash
cd android
./gradlew assembleRelease testDebugUnitTest lintRelease
```

构建得到的是未签名 Release APK。正式发布必须使用维护者自己的签名密钥；密钥及密码不得提交到仓库。CI 还会检查 APK 中的 `school-data.json` 不含预置课表。

## 参与项目

提交代码前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 和 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。安全问题按 [SECURITY.md](SECURITY.md) 报告。

## 许可证

除另有明确标注的第三方内容外，本项目原创代码和文档采用 [GNU Affero General Public License v3.0 only](LICENSE)（SPDX：`AGPL-3.0-only`）。运行在网络上的修改版本也必须向用户提供对应源代码。

第三方组件、衍生文件和完整归属见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。其中 Apache-2.0、MIT 等组件仍按各自许可证提供，不因本项目许可证而改变。

Copyright © 2026 baihuanxi552-design and contributors.
