# zygisk-biometric-pay

原仓库地址[eritpchy/FingerprintPay](https://github.com/eritpchy/FingerprintPay)，为了避免歧义，本仓库改成[zygisk-biometric-pay](https://github.com/ulua3809/zygisk-BiometricPay)

让微信、支付宝、淘宝、腾讯QQ、云闪付在支持指纹识别的手机上使用指纹支付

和上游仓库的区别：

1. 改进Zygisk注入方式，使用内存加载器
2. 动态配置作用域，5个应用，1个模块
3. 不再挂载dex到系统中
4. **只维护Zygisk模块，不再维护dex**

## 请注意: 支付宝支持刷脸支付, 体验感官跟苹果的Face ID差不多, 请考虑优先使用

## 最低要求

* Android 8.0+
* 支持[生物识别](https://source.android.com/docs/security/features/biometric?hl=zh-cn)——指纹，人脸，虹膜认证的设备(要求 `BIOMETRIC_STRONG`)
* [Zygisk Api 等级 &gt;= 1](https://github.com/topjohnwu/zygisk-module-sample?tab=readme-ov-file#api)(因为没用到高版本特性LOL) ~或 [Xposed运行环境](https://github.com/ElderDrivers/EdXposed)~(不推荐)

## 实现原理

1. 利用[zygisk](https://github.com/topjohnwu/Magisk/tree/master/native/src/core/zygisk)将dex加载到对应应用的内存中
2. 在指纹支付模块中录入应用的"支付密码"
3. 使用[TEE](https://source.android.com/docs/security/features/trusty?hl=zh-cn)(v5.0+)将"支付密码"加密保存
4. 对应程序在支付界面时, 验证手机指纹, 验证成功解密"支付密码"
5. 自动替代用户输入"支付密码", 完成支付操作


## 使用步骤

1. 确保设备上Zygisk环境正常
2. 去[Release](https://github.com/ulua3809/zygisk-BiometricPay/releases)下载模块
3. 进入 root 管理器安装模块，按安装提示操作
4. 用音量键选择作用域，选错了没关系，可以安装完成后修改，见*作用域*

## 使用步骤 Xposed

不推荐使用本仓库的xopsed模块，因为和上游没有区别

[点我去上游仓库](https://github.com/eritpchy/FingerprintPay?tab=readme-ov-file#%E4%BD%BF%E7%94%A8%E6%AD%A5%E9%AA%A4-xposed)

## 应用内配置教程

见[上游仓库](https://github.com/eritpchy/FingerprintPay?tab=readme-ov-file#%E8%AF%A6%E7%BB%86%E6%95%99%E7%A8%8B)

## 模块配置

### 作用域
在 `/data/adb/modules/zygisk-biometric-pay/config/`下可以配置模块作用域

> 作用域就是选择对哪些应用启用指纹支付，创建特定名字**空文件**就可以了

> 小提示：创建或删除文件后，强行停止目标应用，下次启动时作用域就可以生效了

|  应用  | 创建文件名 |
| :----: | :--------: |
| 支付宝 |   alipay   |
|   QQ   |     qq     |
|  淘宝  |   taobao   |
| 云闪付 |  unionpay  |
|  微信  |   wechat   |

修改压缩包里config中的空文件来配置默认情况，安装时不选择，超时时使用的配置。该文件夹下的文件安装时**没有sha256验证**

### dex文件更新

> ⚠当应用更新时，指纹支付可能失效，因为本人没有足够时间维护dex实现，推荐使用上游仓库的dex

1. 下载对应dex,去[这儿](https://github.com/eritpchy/FingerprintPay/releases)下载模块，推荐选择zygisk模块，实际上选哪个都行
2. 解压得到dex
3. 重命名成`zygisk-biometric-pay.dex`，放在压缩文件中`dex`目录下
4. 获取dex文件sha256，填入`zygisk-biometric-pay.dex.sha256`中，注意为utf-8小写十六进制文本，不要有换行符，大小应为64Byte。此文件仅供模块安装时检验文件是否损坏
5. 安装修改后的模块

或者直接修改模块目录
1. 下载对应dex,去[这儿](https://github.com/eritpchy/FingerprintPay/releases)下载模块，推荐选择zygisk模块，实际上选哪个都行
2. 解压得到dex
3. 重命名成`zygisk-biometric-pay.dex`，放在`/data/adb/modules/zygisk-biometric-pay/dex/`目录下
4. 强行停止目标应用后生效


## 致谢
* [eritpchy/FingerprintPay](https://github.com/eritpchy/FingerprintPay)
* [aviraxp/Zygisk-KeystoreInjection](https://github.com/aviraxp/Zygisk-KeystoreInjection)
* [WechatFp](https://github.com/dss16694/WechatFp)
* [Magisk](https://github.com/topjohnwu/Magisk)
* [Zygisk Next](https://github.com/Dr-TSNG/ZygiskNext)
* [KernelSU](https://github.com/tiann/KernelSU)
* [Magisk Delta](https://huskydg.github.io/magisk-files/)
* [LSPosed](https://github.com/LSPosed/LSPosed)
* [Riru](https://github.com/RikkaApps/Riru)
* [EdXposed](https://github.com/ElderDrivers/EdXposed)

## 提示

1. 本软件的网络功能仅限检查自己软件更新功能, 如不放心, 欢迎REVIEW代码.
2. 支付宝、淘宝、微信、QQ、云闪付支持版本请参考镜像站的适配版本, 随意升级新版本可能不兼容
3. 云闪付请用play版本
4. 支付宝10.3.80.9100及以上版本请前往 设置-->支付设置 中查找入口
5. 自4.7.4版本开始, 为减少打扰, 非紧急更新推送暂缓推送
6. Magisk Delta + Zygisk Next 组合 截止2023年11月8日目前这两软件尚未互相适配, 切勿尝试!
7. Magisk 本身自带Zygisk功能, 切勿尝试 Magisk + Zygisk Next 这么无聊的组合
8. 自5.0.0版本开始, 如果您**每次**(请注意, 是**每次**!)都识别出错第一次, 属于不正常现象, 正常现象应为首次出错一次,后续正常, 您可以删除系统指纹再重新添加并重新录入支付密码尝试

