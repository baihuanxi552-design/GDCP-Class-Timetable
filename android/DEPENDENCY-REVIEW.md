# 依赖升级核验（2026-10-15）

基线：Compose 1.8.2、Miuix 0.5.1、AGP 8.9.1、Kotlin 2.2.10、compileSdk 35、JDK 17。每项升级独立运行 GitHub Actions，检查 Release 编译、单元测试、Lint 和无预置课表。

| PR | 升级 | 实测结果与处理 |
| --- | --- | --- |
| [#1](https://github.com/baihuanxi552-design/GDCP-Class-Timetable/pull/1) | Compose 1.8.2 → 1.12.1 | AAR 元数据检查失败：需要 compileSdk ≥37、AGP ≥9.1.0。关闭并暂缓，保留 1.8.2。 |
| [#2](https://github.com/baihuanxi552-design/GDCP-Class-Timetable/pull/2) | Miuix 0.5.1 → 0.8.8 | AAR 元数据检查失败：Miuix、shapes、navigationevent 依赖需要 compileSdk ≥36。关闭并暂缓，保留 0.5.1。尚未进入 Kotlin/API 兼容检查。 |
| [#3](https://github.com/baihuanxi552-design/GDCP-Class-Timetable/pull/3) | download-artifact v4 → v8 | 当前 APK 上传、v8 下载、逐字节比较通过；已合并。移除依赖过期构建产物的 3.1.17 旧发布脚本，避免重新发布过时版本。 |
| [#4](https://github.com/baihuanxi552-design/GDCP-Class-Timetable/pull/4) | Gradle 8.13 → 9.8.1 | 独立升级已通过编译、测试、Lint 和空数据检查；合并前再与 #3 的工作流一起核验。保留分发包 SHA-256 校验。 |

## 可复查的独立运行

- Compose：[失败日志](https://github.com/baihuanxi552-design/GDCP-Class-Timetable/actions/runs/38720137709)
- Miuix：[失败日志](https://github.com/baihuanxi552-design/GDCP-Class-Timetable/actions/runs/38720144489)
- Actions v8：[通过日志](https://github.com/baihuanxi552-design/GDCP-Class-Timetable/actions/runs/38720180735)
- Gradle 9.8.1：[通过日志](https://github.com/baihuanxi552-design/GDCP-Class-Timetable/actions/runs/38720151614)

Dependabot 暂时跳过已证明无法单独升级的 Compose 1.12.1 和 Miuix 0.8.8 两个精确版本，其他依赖更新继续检查。将来迁移 SDK/AGP/Kotlin/Compose/Miuix 时，需要移除这两项 ignore、适配组件 API，并在设备上验证课表布局、页面切换、文字效果和教务导入。

本轮未修改应用运行时依赖或课程数据格式。构建检查不能证明所有运行时行为均兼容；本轮没有 Android 真机视觉/性能测试，也没有替换正式签名发布 APK。
