# 本地素材扩展

公开构建无需额外图片即可运行。`ArtCatalog` 为缺失卡面和背景生成渐变回退；应用图标通过 Android PackageManager 从本机已安装应用读取。

可在本地加入已获相应使用权的文件：

| 目录 | 用途 |
| --- | --- |
| `launcher/app/src/main/assets/art/<key>.jpg` | 内容入口卡面 |
| `launcher/app/src/main/assets/backdrops/<key>.jpg` | 独立全屏背景 |
| `launcher/app/src/main/assets/card/nba.png` | 可选卡片图标 |

当前 key 包括 `nba`、`pokemon`、`spongebob`、`filmly`、`aurora`、`bili`、`music`、`smarttube`、`voice`、`chrome`、`cloud`。图片与卡片/导航逻辑解耦；图片缺失不会阻止构建。目录默认被 gitignore 忽略，避免把个人素材自动提交。

测试 fixtures 的图标是仓库内通用图形，不是实际应用标志。真实设备的应用图标由对应应用提供。
