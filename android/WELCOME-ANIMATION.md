# 首次欢迎动画（Android 3.2.5）

参考 HyperCeiler 的 StartupFragment、AnimHelper 与 GlowPainter：
- https://github.com/ReChronoRain/HyperCeiler/blob/main/library/provision/src/main/java/com/sevtinge/hyperceiler/provision/fragment/StartupFragment.java
- https://github.com/ReChronoRain/HyperCeiler/blob/main/library/provision/src/main/java/com/sevtinge/hyperceiler/provision/utils/AnimHelper.java
- https://github.com/ReChronoRain/HyperCeiler/blob/main/library/provision/src/main/java/com/sevtinge/hyperceiler/provision/renderengine/GlowPainter.java

参考提交：5e4686069dd7ab1f3697e256d5fc7d68fb73e317

原实现使用 Folme、RuntimeShader 及小米私有模糊接口。此处独立使用 Compose 实现相似的图标分段放大、延迟淡入按钮与柔和发光背景，未复制上游代码、图标、着色器及其他素材，也不引入模块或 Root 依赖。原 AnimHelper 标注 AGPL-3.0；本实现不作为上游源文件的移植。

首次安装或第一次更新到此版本显示欢迎页，点击“开始使用”后记录独立 welcomeAnimationSeenV2 标志；已导入数据不变。之后启动直接读取课表，未配置用户继续进入教务导入引导。旋转后已完成的动画保留最终状态，未完成的动画重播。所有动画遵循 Compose 系统时长设置；系统关闭动画时直接显示最终状态。Android 13 及以上使用独立编写的 RuntimeShader/AGSL 实现移动色场、柔和涟漪边缘与从黑色展开的圆形遮罩；旧系统使用移动径向渐变和圆形黑色遮罩兼容。未复制原项目着色器。背景随时间持续流转，仅 RESUMED 状态更新，退出后停止。

本次仅修改安卓应用，网页界面与导入流程不变。构建、单元测试、Lint 为自动验证；小米、vivo 等设备上的视觉、旋转、辅助功能和首次运行交互需实机确认。

进一步参考：
- https://github.com/ReChronoRain/HyperCeiler/blob/main/library/provision/src/main/res/raw/glow.glsl
- https://github.com/ReChronoRain/HyperCeiler/blob/main/library/provision/src/main/java/com/sevtinge/hyperceiler/provision/renderengine/GlowController.java

3.2.5 会重新显示一次新版欢迎动画，以供已体验上一版的用户查看；仅更新欢迎页标志，不重置导入和课程数据。RuntimeShader 的设备编译与实际流畅度待真机验证。
