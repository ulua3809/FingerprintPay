#include <cstdlib>
#include <unistd.h>
#include <fcntl.h>
#include <android/log.h>

#include "zygisk.hpp"
#include "log.h"
#include "fingerprint.h"

using zygisk::Api;
using zygisk::AppSpecializeArgs;
using zygisk::ServerSpecializeArgs;

class ZygiskModule : public zygisk::ModuleBase {
public:
    void onLoad(Api *api, JNIEnv *env) override {
        this->api = api;
        this->env = env;
    }

    void preAppSpecialize(AppSpecializeArgs *args) override {
        fingerprintPre(env, &args->app_data_dir, &args->nice_name);
    }

    void postAppSpecialize(const AppSpecializeArgs *args) override {
        fingerprintPost(env, MAGISK_MODULE_TYPE_ZYGISK);
    }

private:
    Api *api;
    JNIEnv *env;
};
REGISTER_ZYGISK_MODULE(ZygiskModule)