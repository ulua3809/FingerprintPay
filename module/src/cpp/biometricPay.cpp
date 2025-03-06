#include "biometricPay.hpp"

#include <fcntl.h>
#include <unistd.h>

#include <cstdlib>

#include "const.hpp"
#include "dexutil.hpp"
#include "zygisk.hpp"

using zygisk::Api;
using zygisk::AppSpecializeArgs;
using zygisk::ServerSpecializeArgs;

class biometricPay : public zygisk::ModuleBase {
 public:
  void onLoad(Api *api, JNIEnv *env) override {
    this->api = api;
    this->env = env;
  }

  void preAppSpecialize(AppSpecializeArgs *args) override {
    api->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
    const char *process = env->GetStringUTFChars(args->nice_name, nullptr);
    processname = new char[strlen(process) + 1];
    strcpy(processname, process);
    env->ReleaseStringUTFChars(args->nice_name, process);
    // assume all scope
    configFlag = ALL_SCPOE_FLAG;
    if (!shouldInject()) {
      needInject = false;
      return;
    }
    int fd = api->connectCompanion();
    companionStruct cs = deserializeFromFile(fd);
    configFlag = cs.confFlags;
    dexVector = cs.dexVector;
    if (shouldInject()) {
      needInject = true;
      return;
    }
    needInject = false;
    delete processname;
  }

  void postAppSpecialize(const AppSpecializeArgs *args) override {
    if (!needInject) {
      return;
    }

    injectDex(env, dexVector, processEntryClass, processEntryMethodName,
              "(Ljava/lang/String;Ljava/lang/String;)V",
              env->NewStringUTF(processname),
              env->NewStringUTF(PLUGINTYPENAME));
    delete processEntryClass;
    delete processEntryMethodName;
  }

  void preServerSpecialize(ServerSpecializeArgs *args) override {
    api->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
  }

 private:
  Api *api;
  JNIEnv *env;

  std::vector<uint8_t> dexVector;
  uint8_t configFlag;

  bool needInject;

  char *processname;
  char *processEntryClass;
  char *processEntryMethodName;

  bool shouldInject() {
    if (niceeq(processname, ALIPAY_PROCESS) &&
        (configFlag & ConfigBits::alipay)) {
      processEntryMethodName = new char[strlen(ALIPAY_EMN) + 1];
      strcpy(processEntryMethodName, ALIPAY_EMN);

      processEntryClass = new char[strlen(ALIPAY_BCP) + 1];
      strcpy(processEntryClass, ALIPAY_BCP);

      return true;
    } else if ((niceeq(processname, QQ0_PROCESS) ||
                niceeq(processname, QQ1_PROCESS)) &&
               (configFlag & ConfigBits::qq)) {
      processEntryMethodName = new char[strlen(QQ_EMN) + 1];
      strcpy(processEntryMethodName, QQ_EMN);

      processEntryClass = new char[strlen(QQ_BCP) + 1];
      strcpy(processEntryClass, QQ_BCP);

      return true;
    } else if (niceeq(processname, TAOBAO_PROCESS) &&
               (configFlag & ConfigBits::taobao)) {
      processEntryMethodName = new char[strlen(TAOBAO_EMN) + 1];
      strcpy(processEntryMethodName, TAOBAO_EMN);

      processEntryClass = new char[strlen(TAOBAO_BCP) + 1];
      strcpy(processEntryClass, TAOBAO_BCP);
      return true;
    } else if (niceeq(processname, UNIONPAY_PROCESS) &&
               (configFlag & ConfigBits::unionpay)) {
      processEntryMethodName = new char[strlen(UNIONPAY_EMN) + 1];
      strcpy(processEntryMethodName, UNIONPAY_EMN);

      processEntryClass = new char[strlen(UNIONPAY_BCP) + 1];
      strcpy(processEntryClass, UNIONPAY_BCP);
      return true;
    } else if (niceeq(processname, WECHAT_PROCESS) &&
               (configFlag & ConfigBits::wechat)) {
      processEntryMethodName = new char[strlen(WECHAT_EMN) + 1];
      strcpy(processEntryMethodName, WECHAT_EMN);

      processEntryClass = new char[strlen(WECHAT_BCP) + 1];
      strcpy(processEntryClass, WECHAT_BCP);
      return true;
    } else {
      return false;
    }
  }
};

static void companion_handler(int i) {
  companionStruct csw = companionStruct(getconfigFlag(), readFile(DEX_PATH));
  serializeToFile(&csw, i);
}

REGISTER_ZYGISK_MODULE(biometricPay)
REGISTER_ZYGISK_COMPANION(companion_handler)
