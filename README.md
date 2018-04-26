

![1](https://github.com/eritpchy/Xposed-Fingerprint-pay/raw/master/app/src/main/res/mipmap-xhdpi/ic_launcher.png)
# Xposed Fingerprint pay
让微信、支付宝、淘宝和腾讯QQ在支持指纹识别的手机上使用指纹支付, 即使他们都不打算支持!

已适配版本:

| 微信                                       |                   支付宝                    |                                       淘宝 |                                     腾讯QQ |
| ---------------------------------------- | :--------------------------------------: | ---------------------------------------: | ---------------------------------------: |
| 6.5.8                                    | [10.1.0.090418-114](https://github.com/eritpchy/Xposed-Fingerprint-pay/releases/download/2.0.0/Alipay-10.1.0.090418-114.apk) | [6.11.0-161](https://github.com/eritpchy/Xposed-Fingerprint-pay/releases/download/2.1.0/Taobao-6.11.0-161.apk) | [QQ-7.2.5-744](https://github.com/eritpchy/Xposed-Fingerprint-pay/releases/download/3.0.0/QQ-7.2.5-744.apk) |
| [6.5.10-1080](https://github.com/eritpchy/Xposed-Fingerprint-pay/releases/download/1.3/weixin6510.apk) |                   或最新版                   |                                     或最新版 |                                     或最新版 |
| 6.5.13-1081                              |                                          |                                          |                                          |
| [6.5.13-1100](https://github.com/eritpchy/Xposed-Fingerprint-pay/releases/download/1.4.1/WeChat-6.5.13-1100.apk) |                                          |                                          |                                          |
| [6.5.16-1101](https://github.com/eritpchy/Xposed-Fingerprint-pay/releases/download/2.4.0/WeChat-6.5.16-1101.apk) |                                          |                                          |                                          |
| [6.5.16-1120](https://github.com/eritpchy/Xposed-Fingerprint-pay/releases/download/2.3.0/WeChat-6.5.16-1120.apk) |                                          |                                          |                                          |
| 或最新版                                     |                                          |                                          |                                          |

以下软件经过测试, 可以正常调用支付宝、微信:\
猫眼\
美团外卖\
京东\
去哪儿\
饿了么\
网易严选\
一淘


感谢原作者 dss16694/WechatFp , 这么给力的项目

## 注意: 这是Xposed插件
无法安装 Xposed 可以下载分身大师X版([点击下载](http://msoftdl.360.cn/mobilesafe/shouji360/360safe/300030/Magic.apk)), 在里面使用Xposed插件

最低配置要求:
1. 有指纹硬件
2. Android 6.0+(大部分机型)
3. Android 5.1+(部分魅族机型)
4. Android 4.4+(部分三星机型)


#### 使用步骤:
1. 下载并安装插件: [WeChatFp-3.2.0-release.apk](https://github.com/eritpchy/Xposed-Fingerprint-pay/releases/download/3.2.0/WeChatFp-3.2.0-release.apk)
2. 下载并安装微信: [WeChat-6.5.16-1120.apk](https://github.com/eritpchy/Xposed-Fingerprint-pay/releases/download/2.3.0/WeChat-6.5.16-1120.apk)
3. 下载并安装支付宝: [Alipay-10.1.0.090418-114.apk](https://github.com/eritpchy/Xposed-Fingerprint-pay/releases/download/2.0.0/Alipay-10.1.0.090418-114.apk)
4. 下载并安装淘宝: [Taobao-6.11.0-161.apk](https://github.com/eritpchy/Xposed-Fingerprint-pay/releases/download/2.1.0/Taobao-6.11.0-161.apk)
5. 下载并安装QQ: [QQ-7.2.5-744.apk](https://github.com/eritpchy/Xposed-Fingerprint-pay/releases/download/3.0.0/QQ-7.2.5-744.apk)
6. 启用插件, 输入密码
7. 关闭手机
8. 打开手机，Enjoy.

#### 详细教程:
1. [支付宝](https://github.com/eritpchy/Xposed-Fingerprint-pay/tree/master/doc/Alipay)
2. [淘宝](https://github.com/eritpchy/Xposed-Fingerprint-pay/tree/master/doc/Taobao)
3. [微信](https://github.com/eritpchy/Xposed-Fingerprint-pay/tree/master/doc/WeChat)
4. [QQ](https://github.com/eritpchy/Xposed-Fingerprint-pay/tree/master/doc/QQ)

百度云下载地址:
链接: https://pan.baidu.com/s/1qZdJvHE 密码: wss3

#### 常见问题:
1. 部分三星设备(S8, S7e等) 在2.6版本以上 Xposed 需要安装88.2版本, 避免开机卡住
2. 因Xposed 造成的开机卡住, 可按电源键禁用Xposed (多次振动后重启手机)
3. 可以解锁手机但提示系统指纹未启用\
      3.1 QQ请确认版本在7.2.5以上\
      3.2 说明您的手机系统版本过低不支持, 请升级至安卓6.0以上
4. 插件已安装, 但在微信或支付宝中看不见菜单?\
      4.1 请逐个检查支付宝、淘宝、微信的菜单项， 是否有任何一个已激活\
      4.3 请同时安装其它插件, 比如微x 确保Xposed是正常的工作的\
      4.2 尝试, 取消勾选插件, 再次勾选插件, 关机, 再开机
5. 魅蓝系列5.1系统不支持, 请升级系统至安卓6.0以上, 请不要再问了! 想支持请提供手机!
6. 魅蓝note3可能支持, 请自行测试


#### 友情提示: 
1. 以上提供的安装包均为 支付宝/微信/淘宝官方提供的安装包, 没有任何添加修改, 提供安装包只为方便找到指定适配过的安装包.
2. 本软件的网络功能仅限检查自己软件更新功能, 如不放心, 欢迎REVIEW代码.
3. 支付宝、淘宝和QQ均可沿用市场中的最新版.
4. 自2.6.0版本开始微信也可沿用市场最新版.

![qq](https://github.com/eritpchy/Xposed-Fingerprint-pay/raw/master/doc/qqGroup.png)

#### QQ交流群: [665167891](http://shang.qq.com/wpa/qunwpa?idkey=91c2cd8f14532413701607c364f03f43afa1539a24b96b8907c92f3c018894e5)
