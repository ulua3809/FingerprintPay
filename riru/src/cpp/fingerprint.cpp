#include <stdbool.h>
#include <stdio.h>
#include <unistd.h>
#include <jni.h>
#include <sys/types.h>
#include <riru.h>
#include <malloc.h>
#include <cstring>

#include "fingerprint.h"
#include "log.h"

static bool sHookEnable = false;
static char *sAppDataDir = NULL;
static char *sNiceName = NULL;

static char *jstringToC(JNIEnv * env, jstring jstr){
    char *ret = NULL;
    if (jstr) {
        const char* str = env->GetStringUTFChars(jstr, NULL);
        if (str != NULL) {
            int len = strlen(str);
            ret = (char*) malloc((len + 1) * sizeof(char));
            if (ret != NULL){
                memset(ret, 0, len + 1);
                memcpy(ret, str, len);
            }
            env->ReleaseStringUTFChars(jstr, str);
        }
    }
    return ret;
}

static bool equals(const char *str1, const char *str2) {
    if (str1 == NULL && str2 == NULL) {
        return true;
    } else {
        if (str1 != NULL && str2 != NULL) {
            return strcmp(str1, str2) == 0;
        } else {
            return false;
        }
    }
}


/**  参数说明：
 *  jdexPath        dex存储路径
 *  jodexPath       优化后的dex包存放位置
 *  jclassName      需要调用jar包中的类名
 *  jmethodName     需要调用的类中的静态方法
 */
static void loadDex(JNIEnv *env, jstring jdexPath, jstring jodexPath, jstring jclassName, const char* methodName, jstring jarg1) {

    if (!jdexPath) {
        LOGD("MEM ERR");
        return;
    }

    if (!jodexPath) {
        LOGD("MEM ERR");
        return;
    }

    if (!jclassName) {
        LOGD("MEM ERR");
        return;
    }

    if (!jarg1) {
        LOGD("MEM ERR");
        return;
    }

    jclass classloaderClass = env->FindClass("java/lang/ClassLoader");
    jmethodID getsysClassloaderMethod = env->GetStaticMethodID(classloaderClass, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
    jobject loader = env->CallStaticObjectMethod(classloaderClass, getsysClassloaderMethod);
    jclass dexLoaderClass = env->FindClass("dalvik/system/DexClassLoader");
    jmethodID initDexLoaderMethod = env->GetMethodID(dexLoaderClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)V");
    jobject dexLoader = env->NewObject(dexLoaderClass,initDexLoaderMethod, jdexPath, jodexPath, NULL, loader);
    jmethodID findclassMethod = env->GetMethodID(dexLoaderClass, "findClass", "(Ljava/lang/String;)Ljava/lang/Class;");

    if (findclassMethod == NULL) {
        findclassMethod = env->GetMethodID(dexLoaderClass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    }

    jclass javaClientClass = (jclass)env->CallObjectMethod(dexLoader, findclassMethod, jclassName);
    jmethodID targetMethod = env->GetStaticMethodID(javaClientClass, methodName, "(Ljava/lang/String;)V");

    if (targetMethod == NULL) {
        LOGD("target method(%s) not found", methodName);
        return;
    }

    env->CallStaticVoidMethod(javaClientClass, targetMethod, jarg1);
}

void fingerprintPre(JNIEnv *env, jstring *appDataDir, jstring *niceName) {
    char *cAppDataDir = jstringToC(env, *appDataDir);
    if (cAppDataDir == NULL) {
        LOGD("MEM ERR");
        return;
    }
    sAppDataDir = strdup(cAppDataDir);
    free(cAppDataDir);
    if (sAppDataDir == NULL) {
        LOGD("MEM ERR");
        return;
    }
    sNiceName = jstringToC(env, *niceName);
    if (sNiceName == NULL) {
        LOGD("MEM ERR");
        return;
    }

    if (strstr(MODULE_NAME, "qq")) {
        sHookEnable = equals(sNiceName, "com.tencent.mobileqq")
            || equals(sNiceName, "com.tencent.mobileqq:tool");
    } else if (strstr(MODULE_NAME, "wechat")) {
        sHookEnable = equals(sNiceName, "com.tencent.mm");
    } else if (strstr(MODULE_NAME, "alipay")) {
        sHookEnable = equals(sNiceName, "com.eg.android.AlipayGphone");
    } else if (strstr(MODULE_NAME, "taobao")) {
        sHookEnable = equals(sNiceName, "com.taobao.taobao");
    } else {
        perror("unimplement target " MODULE_NAME);
    }
}

void fingerprintPost(JNIEnv *env) {
    if (sHookEnable) {
        char appCacheDir[PATH_MAX] = {0};
        snprintf(appCacheDir, PATH_MAX - 1, "%s/cache", sAppDataDir);

            const char *dexPath = "/data/local/tmp/lib" MODULE_NAME ".dex";
            if (access(dexPath, 0) != 0) {
                dexPath = "/system/framework/lib" MODULE_NAME ".dex";
            }
            const char *bootClassPath;
            if (strstr(MODULE_NAME, "qq")) {
                bootClassPath = "com.surcumference.fingerprint.plugin.magisk.QQPlugin";
            } else if (strstr(MODULE_NAME, "wechat")) {
                bootClassPath = "com.surcumference.fingerprint.plugin.magisk.WeChatPlugin";
            } else if (strstr(MODULE_NAME, "alipay")) {
                bootClassPath = "com.surcumference.fingerprint.plugin.magisk.AlipayPlugin";
            } else if (strstr(MODULE_NAME, "taobao")) {
                bootClassPath = "com.surcumference.fingerprint.plugin.magisk.TaobaoPlugin";
            } else {
                perror("unimplement target " MODULE_NAME);
            }
            loadDex(env,
                env->NewStringUTF(dexPath),
                env->NewStringUTF(appCacheDir),
                env->NewStringUTF(bootClassPath),
                "main",
                env->NewStringUTF(sNiceName)
        );
    }
}
