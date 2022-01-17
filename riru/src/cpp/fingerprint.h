#ifndef _FINGERPRINT_H_
#define _FINGERPRINT_H_

#include <jni.h>

#define MAGISK_MODULE_TYPE_RIRU     "Riru"
#define MAGISK_MODULE_TYPE_ZYGISK   "Zygisk"

void fingerprintPre(JNIEnv *env, jstring *appDataDir, jstring *niceName);
void fingerprintPost(JNIEnv *env, const char *pluginTypeName);

#endif // _FINGERPRINT_H_