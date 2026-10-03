# 网页欢迎引导 3.3.0

流光与涟漪着色器来源：[HyperCeiler](https://github.com/ReChronoRain/HyperCeiler)，library/provision 的 glow.glsl / GlowPainter.java。原 shader 文件保持原样，默认参数由 GlowPainter 提取。浏览器适配层新增 WebGL 顶点程序、坐标翻转、浮点常量兼容、分辨率限制、暂停与资源释放。界面使用本项目个人课表图标，不使用 HyperCeiler 商标。

原项目版权归 HyperCeiler Contributions。保留 AGPL-3.0 许可于 guide-assets/AGPL-3.0.txt。衍生的 guide-flow.js、web-guide.js 与 web-guide.css 按 AGPL-3.0 提供；对应源代码在本仓库公开。

首次使用流程：欢迎 → 校区与文件导入 → 设置完毕 → 开始使用。设置中可重新引导。官方登录网站、解析书签和文件导入保持独立；不在本网站收集登录密码。个人课表、自建课程保存在浏览器本地。完成前刷新会重播欢迎页，已有用户不强制进入引导。

桌面自动检查覆盖流程、重播、旧缓存、个人课表分组、WebGL / 渐变兼容与减少动态效果。手机实际动画观感和浏览器兼容需用户真机确认。
