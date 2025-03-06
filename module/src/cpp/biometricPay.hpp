#pragma once

#include <android/log.h>

#include <cstdint>
#include <vector>
#define LOGD(...) \
  __android_log_print(ANDROID_LOG_DEBUG, "XFINGERPRINT", __VA_ARGS__)

#define PLUGINTYPENAME "Zygisk"
#define ALIPAY_PROCESS "com.eg.android.AlipayGphone:"
#define QQ0_PROCESS "com.tencent.mobileqq:"
#define QQ1_PROCESS "com.tencent.mobileqq:tool"
#define TAOBAO_PROCESS "com.taobao.taobao:"
#define UNIONPAY_PROCESS "com.unionpay:"
#define WECHAT_PROCESS "com.tencent.mm:"

#define ALIPAY_BCP "com.surcumference.fingerprint.plugin.magisk.AlipayPlugin"
#define QQ_BCP "com.surcumference.fingerprint.plugin.magisk.QQPlugin"
#define TAOBAO_BCP "com.surcumference.fingerprint.plugin.magisk.TaobaoPlugin"
#define UNIONPAY_BCP \
  "com.surcumference.fingerprint.plugin.magisk.UnionPayPlugin"
#define WECHAT_BCP "com.surcumference.fingerprint.plugin.magisk.WeChatPlugin"

#define ALIPAY_EMN "main"
#define QQ_EMN "main"
#define TAOBAO_EMN "main"
#define UNIONPAY_EMN "main"
#define WECHAT_EMN "main"

#ifndef CONFIGBITS_ENUM_H
#define CONFIGBITS_ENUM_H
enum ConfigBits : uint8_t {
  alipay = 1u << 0,
  qq = 1u << 1,
  taobao = 1u << 2,
  unionpay = 1u << 3,
  wechat = 1u << 4
};
#define ALL_SCPOE_FLAG (alipay | qq | taobao | unionpay | wechat)
#endif

#ifndef COMPANION_STRUCT_H
#define COMPANION_STRUCT_H

struct companionStruct {
  uint8_t confFlags = ALL_SCPOE_FLAG;
  std::vector<uint8_t> dexVector;
  companionStruct(uint8_t flags, std::vector<uint8_t> dexV)
      : confFlags(flags), dexVector(std::move(dexV)) {}
};
#endif
