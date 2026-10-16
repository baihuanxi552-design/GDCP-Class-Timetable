# 欢迎引导移植 Android 3.2.13

从 HyperCeiler 移植 glow.glsl（逐字保留）、GlowPainter 的全部默认参数、GlowController 的时间轴及往返流光，Compose 改写生命周期与绘制。图标与标题分段缩放（0.5→0.95，440ms；0.95→1，700ms），按钮延迟 1340ms 后 450ms 淡入放大，欢迎布局采用原版 30/40/20 权重、90dp 图标和圆形箭头按钮。品牌图标与名称替换为个人课表，后续页面仍为应用已有校区与教务导入引导；未复制 Root、模块权限或 HyperCeiler 的设置功能。

来源：ReChronoRain/HyperCeiler
https://github.com/ReChronoRain/HyperCeiler
参考提交：5e4686069dd7ab1f3697e256d5fc7d68fb73e317
源路径：library/provision/src/main/res/raw/glow.glsl；renderengine/GlowPainter.java、GlowController.java；fragment/StartupFragment.java；utils/AnimHelper.java；res/layout/provision_startup_layout.xml。

Copyright (C) 2023-2026 HyperCeiler Contributions。
此次使用上游 AGPL-3.0 代码，安卓组合源码按 AGPL-3.0 提供，许可见 LICENSE、APK assets/AGPL-3.0.txt。第三方依赖保留各自许可。修改：用 Compose Canvas/RuntimeShader 替换上游 View/Folme，RESUMED 状态驱动帧时间，设备旋转与关闭动画的兼容、进入导入页面的圆形展开、应用品牌替换；原着色器不改。

Android 13+ 使用原着色器，旧系统或着色器初始化失败使用兼容动态渐变，兼容方案不能完全复制原效果。小米私有 MiuiBlurUtils 的前景混色未直接调用，故图标与标题混色和原机型可能不同；不能声称像素级一致。原版 shader 已包含噪声、OKLab 混色、屏幕混合、环形变形与遮罩，不使用上一版近似 shader。

welcomeAnimationSeenV3 在新版首次打开后记录；已有课程数据不变。系统关闭动画跳过涟漪，静态背景保留最终状态。构建、单元测试、Lint、签名、APK 内上游 shader 一致性和敏感信息检查为自动验证；RuntimeShader 设备编译、布局及转场观感待实机确认。

本机源码包包含修改后的完整安卓工程、Gradle 构建文件和依赖许可；不包含签名私钥和用户课表。本次未向 GitHub 发布，APK 从本机交付。


3.2.7：完成标记改为 guideCompleteV1，仅最后“设置完毕 → 开始使用”写入；未完成且应用停止后再次进入会重播，教务登录子页面不触发重播。原欢迎页按钮改为记录真实坐标、插值位置/宽高/圆角/背景色的容器展开，后半段预先展示下一页；过渡期间阻止点击。导入或跳过后展示流光完成页，品牌图标、标题、“设置完毕”和底部半透明开始按钮。点击开始后进入已有主界面，缓存、备份和小部件接口不改。此次仅本机编译，不发布 GitHub。

3.2.8：重新实现欢迎页展开。点击冻结按钮 boundsInRoot 并换算根坐标；下一页始终按最终窗口尺寸排布，使用全屏固定图层上的 RoundRect 遮罩插值四边及圆角，避免前版小容器中的内容重新测量与偏移。箭头初段淡出，背景轻微缩小压暗，前景后段淡入，临近全屏时阴影退去，动画结束才切换引导步骤。实机视觉仍需验证。

设置页新增“重新查看欢迎引导”，可在保留课程数据的情况下主动重看。已完成引导的用户不会因本次更新被强制重播。

3.2.9：将添加课程的展开实现原样提取为 ButtonPageExpansion，欢迎按钮与加号调用同一组件，使用 440ms/FastOutSlowInEasing、同一圆角/阴影/位置宽高插值/后半段内容淡入及背景 depthSurface。完成页点击开始时，主界面渐显、完成页同步淡出，480ms 后卸载完成页，动画期间阻止重复操作。仅按钮起始坐标、品牌色及目标内容不同。仍本机交付。

3.2.10：参考 HyperCeiler StartupFragment 的 bitmap capture → makeScaleUpAnim 进入路径，改为 Compose 缓存渲染层。下一页提前按全屏尺寸排布并录制一次，按钮展开只在 Canvas 中计算圆角遮罩和位置并绘制缓存；欢迎流光在展开时暂停，背景绘制复用缓存，不再对实时全屏着色器叠加多阶段模糊。保留440ms曲线、同位置尺寸插值及后半段淡入。隐藏预加载页放在触摸区域之外，并清除语义；完成后切换到真实可交互页面。使用公开Compose接口替代小米私有ActivityOptionsHelper。未取得实机帧率/卡顿统计，性能收益需真机核验。

3.2.11：设置完毕进入主界面的480ms渐变衔接同步加入居中缩放，主界面0.94→1，完成页1→0.94，均使用同一homeReveal进度与圆角过渡；完成页转场时暂停流光计算。正常进入应用时进度初值1，不额外播放此动画。

3.2.11 同时将设置入口改为“重新引导”，写入独立待设置标记，完整重走欢迎、校区、教务导入和完成页。已有本机数据不删除，取消登录不视为导入成功；导入Activity只有保存成功才返回RESULT_OK，最后完成时清除待设置标记。退出再进仍从完整引导开始。

3.2.12：引导到设置完毕改为复用 layeredTransition 的500ms右侧平移进入，前页保留至动画结束并同步 depthSurface 缩放/模糊，不再条件分支硬切。登录保存和稍后导入均经过该动画；动画期间两页禁用点击与无障碍焦点，完成后激活设置完毕页面。完成页进入主页的渐显缩放保持不变。

3.2.13：设置完毕前景页补齐devicePageShape圆角裁剪及深灰环境/聚光阴影；阴影随EnterExitState预进入→可见进度44dp→0变化，与500ms横向滑入同速。前景完全覆盖后阴影消失，背景原有缩放模糊仍保留。

3.2.13同批修正：欢迎按钮展开时背景缓存图层采用devicePageShape圆角裁剪；图标/标题透明度改为涟漪启动450ms后900ms缓慢淡入，图标缩放同步延迟450ms启动。保留展开期间流光暂停及缓存绘制。
