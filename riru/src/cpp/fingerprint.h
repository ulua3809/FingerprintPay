#ifndef _FINGERPRINT_H_
#define _FINGERPRINT_H_

#include <jni.h>

void fingerprintPre(JNIEnv *env, jstring *appDataDir, jstring *niceName);
void fingerprintPost(JNIEnv *env);

#endif // _FINGERPRINT_H_