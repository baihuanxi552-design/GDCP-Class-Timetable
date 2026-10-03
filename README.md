# 班级课表 · 无内置数据版 3.1.17

网页和 Android 均不包含班级课表、教师名单或个人课表。首次使用可直接自建课程，或在「关于 → 设置 → 从本地导入」导入完整备份。

备份使用 `gdcp-timetable-backup` v1；可选 `catalog` 字段包含完整班级课表及体育项目。导出会包含已导入数据，备份只保存在用户选择的本地文件中。旧版仅设置备份仍受支持，但班级必须存在于当前导入的数据中。

网页为静态文件，无编译步骤，执行 `python -m http.server 8000` 可预览。Android 需要 JDK17+、SDK35，执行 `./gradlew assembleRelease testDebugUnitTest lintRelease`。发布版本需要签名。

旧 APK、含个人数据的快捷指令示例、旧 Release 和旧标签已删除。主分支已从无课表数据的新根提交开始；原课表及历史备份仅保存在用户工作区。GitHub 缓存中的旧对象需要平台另行处理。

自动构建产物在 GitHub Actions 的 `timetable-empty-3.1.17` 中，包括网页 ZIP、可安装的调试签名 APK 和未签名 release APK。调试签名 APK 无法覆盖旧正式签名应用，迁移前先导出本机备份。

第三方许可保留在工程中。

## 在线访问与下载

- 网页：https://baihuanxi552-design.github.io/GDCP-Class-Timetable/
- 安卓安装包与网页离线包：https://github.com/baihuanxi552-design/GDCP-Class-Timetable/releases/tag/empty-v3.1.18

安卓安装包使用调试签名，不能覆盖旧正式签名版本；卸载旧版前请先导出设备中的备份。

## Android 3.1.18 性能优化

APK 采用非调试 Release 构建，减少切换页面时的重组，复用文字模糊效果。保留原滑动时长和动画风格。使用测试签名；覆盖安装失败时，卸载前先导出备份。编译、单元测试、Lint 和 APK 调试标志检查已通过，实机帧耗时待验证。
