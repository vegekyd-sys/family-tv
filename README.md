# Family TV · 家庭电视桌面

[![Android CI](https://github.com/vegekyd-sys/family-tv/actions/workflows/android.yml/badge.svg)](https://github.com/vegekyd-sys/family-tv/actions/workflows/android.yml)

原生 Android TV 桌面，面向遥控器操作与家庭常用内容。基于 [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) 的 MIT 开源桌面持续开发。

当前源码版本 **0.4.11**，最近稳定源码 Release 为 **0.4.9**。输入源、最近打开、网络、应用库、设置共用 Liquid Glass 材质和遥控行为，按各自内容采用不同紧凑布局，设计细则见 [UI 规范](docs/ui-spec.md)。

首页首行内容卡与应用行共用一个 RecyclerView、卡片布局、焦点高亮和滚动路径。向下逐行浏览，向上回到第一行时恢复底部四卡；所有行使用相同落点与 300ms 动画。

## 功能

- 四个家庭内容入口，后接已安装应用；同列上下导航、快速反向和 Home 复位。
- 时钟、输入源、最近打开、网络、应用库和系统设置；最近打开位于输入源右侧。
- 原应用图标卡片、独立背景、顶栏背景模糊与玻璃焦点。
- 通过第三方应用自己的入口打开内容；播放、会员及语音服务仍由原应用负责。
- 独立的可选 ADB 维护模块，默认不会自动安装或启用。

## 构建

需要 JDK 17 或 21、Android SDK Platform 34 / Build Tools 34.0.0 和 Python 3。

```bash
export ANDROID_HOME=/path/to/android-sdk
cd launcher
./gradlew --no-daemon :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease
```

APK 位于 `launcher/app/build/outputs/apk/release/app-release.apk`，默认包名为 `live.tianyi.familytv.dev`。它是开发构建，使用本机构建时生成的 debug 签名；不同机器/CI 的签名不能保证覆盖已有安装。项目不分发个人电视签名、设备专用生产包或连接凭据。

## 本地模拟器

```bash
python3 tools/test-local-guard.py
python3 tools/dev.py up
python3 tools/smoke-ui.py
python3 tools/check-row-motion.py
python3 tools/check-home-menus.py
```

这些工具只允许 `FamilyTV_UI / emulator-5580`，并核对模拟器标识，不会自动寻找真实电视。默认镜像为 ARM64，适合 Apple Silicon；其他主机可预先创建同名、同端口的适用 TV AVD。第三方测试应用是本地替身，只检查入口、参数和返回，不代表真实播放结果；不要将 fixtures 安装到电视。

## 素材与配置

公开源码不携带个人影视海报、角色图片、品牌图标拷贝或生成账户记录。缺失素材时，代码会显示内置渐变与通用图形，应用卡仍从已安装应用读取真实图标，因此源码可以直接构建运行。你可以在本地加入有权使用的素材；目录说明见 [素材说明](docs/assets.md)。公开构建与个人电视上的图片外观可能不同，导航实现相同。

主题配置为 `theme/config.json`。第三方包名、内容链接和 OEM Home 行为具有设备与版本差异；开发脚本不会替你接管真实电视，也不包含一键关闭系统组件的流程。

## 持续维护

- `main`：持续维护的公开源码；每次 push / PR 自动执行单元测试、lint 和 APK 构建。
- GitHub Actions 保留开发 APK、测试及 lint 报告，保留期 7 天；不自动部署设备。
- 版本变化记录在 [CHANGELOG](CHANGELOG.md)；协作与验收方式见 [CONTRIBUTING](CONTRIBUTING.md)。
- 公开初始版本从当前实现导出，历史设备操作记录保留在本地，不随源码发布。

## 许可证与来源

代码遵循 [MIT License](LICENSE)，保留上游作者 Gathin 的版权声明。上游基线提交：`f9f0c5353e65c5003d5f2f45784a30c5dee19ecb`。Outfit 字体遵循 [SIL Open Font License](launcher/FONT-LICENSE-OFL.txt)。第三方应用名称与标识归各自权利人；源码许可证不授予第三方图片或品牌的权利。
