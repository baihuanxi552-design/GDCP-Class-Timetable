# 个人教务课表自动更新方案

第一阶段已在 Android 3.2.6 源码中实现：用户开启后加密保留本机登录会话，回到前台时按一小时限频检查；支持立即刷新、较大变化确认、上一份课表恢复，以及包含个人导入数据的备份。

具体行为、会话安全和验收范围见 [JW-REFRESH.md](JW-REFRESH.md)。

## 下一阶段：后台检查（尚未实现）

只有在确认本校可使用持久会话直接 HTTP 拉取完整个人课表后，才考虑 WorkManager 唯一周期任务，每 6 小时尝试一次，要求网络可用，失败采用退避。现有解析依赖 WebView DOM，不能直接放进普通 Worker，需先把后台解析器与当前解析器用同一批虚构夹具核对。

WorkManager 受 Android 省电策略影响，不能保证准点，也不能保证教务系统一改就收到。后台会话过期则提示重新登录；不保存密码、自动填写密码或绕过验证码。

## 参考

- [jlu-mobile-schedule 强智适配器](https://github.com/con9uer666/jlu-mobile-schedule/blob/39f2b96d3ec73ab07c42c4e8c3b4e8359e8831bc/lib/data/providers/hhu_jsxsd.dart)：同源 fetch 与 HTML 解析方式参考，未复制实现。其他学校的接口不代表本校已验证接口。
- [Android 官方 WorkManager 示例](https://github.com/android/architecture-components-samples/tree/main/WorkManagerSample)。
