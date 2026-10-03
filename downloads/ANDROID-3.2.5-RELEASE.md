# 个人课表 Android 3.2.5

- 欢迎页从黑色以柔和涟漪展开为彩色背景。
- 展开后蓝、紫、粉色流光持续缓慢流转；退到后台暂停，退出欢迎页停止。
- Android 13+ 使用独立编写的 RuntimeShader/AGSL；旧系统使用动态渐变兼容。关闭系统动画时显示静态最终状态。
- 更新后新版欢迎动画展示一次，完成后不重复；已有导入、缓存和备份不变。网页界面不变。

参考 HyperCeiler GlowController / glow.glsl 的效果思路，不包含上游源码、着色器和素材，无 Root 或模块依赖。详细说明见 android/WELCOME-ANIMATION.md。

与 3.2.0—3.2.4 正式版同签名，可覆盖安装。Release 构建、单元测试、Lint、签名和空数据检查通过。RuntimeShader 的设备编译、涟漪视觉和性能尚未实机验证。
