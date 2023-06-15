

![1](./app/src/main/res/mipmap-xhdpi/ic_launcher.png)
# FingerprintPay
让微信、支付宝、淘宝、腾讯QQ、云闪付在支持指纹识别的手机上使用指纹支付.

## 注意: 支付宝支持刷脸支付, 体验感官跟苹果的Face ID差不多, 请优先使用支付宝自带的刷脸支付

## 最低要求
* 有指纹硬件
* Android 6.0+
* Android 5.1+(部分魅族机型)
* Android 4.4+(部分三星机型)
* [Magisk](https://github.com/topjohnwu/Magisk)、[Zygisk](https://github.com/topjohnwu/Magisk) 或 [Xposed](https://github.com/ElderDrivers/EdXposed)

## 他怎么工作呢？
1. 利用 [Magisk](https://github.com/topjohnwu/Magisk) 的 [Riru](https://github.com/RikkaApps/Riru) 注入 zygote 进程
2. 加载指纹支付代码

## 国内镜像
- [点这里](https://file.xdow.net/fingerprintpay/)

## 使用步骤 Magisk
1. 下载插件: [riru-release.zip](https://github.com/RikkaApps/Riru/releases)
2. 下载插件: [riru-module-xfingerprint-pay-qq-release.zip](https://github.com/eritpchy/FingerprintPay/releases)
3. 下载插件: [riru-module-xfingerprint-pay-alipay-release.zip](https://github.com/eritpchy/FingerprintPay/releases)
4. 下载插件: [riru-module-xfingerprint-pay-wechat-release.zip](https://github.com/eritpchy/FingerprintPay/releases)
5. 下载插件: [riru-module-xfingerprint-pay-taobao-release.zip](https://github.com/eritpchy/FingerprintPay/releases)
6. 下载插件: [riru-module-xfingerprint-pay-unionpay-release.zip](https://github.com/eritpchy/FingerprintPay/releases)
7. 进入 Magisk Manager, 模块, 安装这几个模块, 不要重启
8. 确认启用模块, 重启手机
9. Enjoy

## 使用步骤 Zygisk
1. 确认 Magisk Manager 应用设置中启用 Zygisk功能
2. 下载插件: [zygisk-module-xfingerprint-pay-qq-release.zip](https://github.com/eritpchy/FingerprintPay/releases)
3. 下载插件: [zygisk-module-xfingerprint-pay-alipay-release.zip](https://github.com/eritpchy/FingerprintPay/releases)
4. 下载插件: [zygisk-module-xfingerprint-pay-wechat-release.zip](https://github.com/eritpchy/FingerprintPay/releases)
5. 下载插件: [zygisk-module-xfingerprint-pay-taobao-release.zip](https://github.com/eritpchy/FingerprintPay/releases)
6. 下载插件: [zygisk-module-xfingerprint-pay-unionpay-release.zip](https://github.com/eritpchy/FingerprintPay/releases)
7. 进入 Magisk Manager, 模块, 安装这几个模块, 不要重启
8. 确认启用模块, 重启手机
9. Enjoy

## 使用步骤 Xposed
1. 下载并安装插件: [xposed.com.surcumference.fingerprintpay.release.apk](https://github.com/eritpchy/FingerprintPay/releases/latest)
2. 启用插件
3. 重启手机
4. Enjoy

## 详细教程
1. [支付宝](https://github.com/eritpchy/FingerprintPay/tree/main/doc/Alipay)
2. [淘宝](https://github.com/eritpchy/FingerprintPay/tree/main/doc/Taobao)
3. [微信](https://github.com/eritpchy/FingerprintPay/tree/main/doc/WeChat)
4. [QQ](https://github.com/eritpchy/FingerprintPay/tree/main/doc/QQ)
5. [云闪付](https://github.com/eritpchy/FingerprintPay/tree/main/doc/UnionPay)

## 常见问题
1. 因Xposed 造成的开机卡住, 可按电源键禁用Xposed (多次振动后重启手机)
2. 可以解锁手机但提示系统指纹未启用\
   2.1 QQ请确认版本在7.2.5以上\
   2.2 说明您的手机系统版本过低不支持, 请升级至安卓6.0以上
3. 插件已安装, 但在微信或支付宝中看不见菜单?\
   3.1 请逐个检查支付宝、淘宝、微信的菜单项， 是否有任何一个已激活\
   3.2 请同时安装其它插件, 比如微x 确保Xposed是正常的工作的\
   3.3 尝试, 取消勾选插件, 再次勾选插件, 关机, 再开机
4. Xposed版只能使用play版本云闪付, 否则打开闪退! riru, zygisk版本暂未发现相关问题

## 致谢
* [Riru](https://github.com/RikkaApps/Riru)
* [EdXposed](https://github.com/ElderDrivers/EdXposed)
* [Magisk](https://github.com/topjohnwu/Magisk)
* [WechatFp](https://github.com/dss16694/WechatFp)

## 提示
1. 本软件的网络功能仅限检查自己软件更新功能, 如不放心, 欢迎REVIEW代码.
2. 支付宝、淘宝、微信、QQ、云闪付均可沿用市场中的最新版.
3. 云闪付请用play版本
4. 支付宝10.3.80.9100及以上版本请前往 设置-->支付设置 中查找入口

![qq](./doc/qqGroup.png)
#### QQ交流群: [665167891](http://shang.qq.com/wpa/qunwpa?idkey=91c2cd8f14532413701607c364f03f43afa1539a24b96b8907c92f3c018894e5)
