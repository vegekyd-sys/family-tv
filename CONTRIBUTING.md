# 持续开发

1. 从 `main` 创建分支；保持改动聚焦，说明影响的操作路径。
2. 在 `launcher/` 运行 `./gradlew :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease`，在根目录运行 `python3 tools/test-local-guard.py`。
3. UI 改动使用专用模拟器检查 Home、上下/左右、同列返回、应用返回与快速连续按键。滚动改动录制原生连续视频，不能把定时截屏当成帧率验收。
4. 通过 Pull Request 描述问题、最终行为、验证与限制；更新 CHANGELOG。GitHub Actions 只构建测试，不触碰设备。
5. 真实电视的包名、签名、固件兼容和回滚另行验收。CI 生成的是开发 APK，不能替代已部署设备的签名包。

请勿提交个人设备地址、ADB 信任密钥、签名密钥、备份、观看记录、账户调用日志或来源不明确的影视图片。应用名/深链可以作为互操作代码保留，但不能把测试替身通过写成真实播放成功。
