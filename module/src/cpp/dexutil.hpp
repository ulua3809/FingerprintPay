#ifndef DEXUTIL_HPP
#define DEXUTIL_HPP

#include <jni.h>
#include <sys/types.h>
#include <unistd.h>

#include <string>
#include <vector>

#include "biometricPay.hpp"
#include "const.hpp"
#include "zygisk.hpp"

// I/O utility functions
ssize_t byteread(int fd, void* buffer, size_t count);
ssize_t bytewrite(int fd, const void* buffer, size_t count);

// Serialization functions
void serializeToFile(const companionStruct* obj, int fd);
companionStruct deserializeFromFile(int fd);

// Configuration function
uint8_t getconfigFlag();

std::vector<uint8_t> readFile(const char* path);
bool niceeq(const char* nicename, const char* target);
bool confExists(const char *name);
/**
 * @brief Dynamically injects DEX and executes entry method
 *
 * @tparam entryMethodArgs Variadic template for method arguments
 * @param env JNI environment pointer
 * @param dexVector DEX file contents
 * @param entryClass Fully qualified class name
 * @param entryMethodName Static method name to invoke
 * @param entryMethodsignature JNI method signature
 * @param args Arguments to pass to the method
 */
template <typename... entryMethodArgs>
void injectDex(JNIEnv* env, std::vector<uint8_t> dexVector,
               std::string entryClass, std::string entryMethodName,
               std::string entryMethodsignature, entryMethodArgs... args) {
  LOGD("get system classloader");
  auto clClass = env->FindClass("java/lang/ClassLoader");
  auto getSystemClassLoader = env->GetStaticMethodID(
      clClass, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
  auto systemClassLoader =
      env->CallStaticObjectMethod(clClass, getSystemClassLoader);

  LOGD("create class loader");
  auto dexClClass = env->FindClass("dalvik/system/InMemoryDexClassLoader");
  auto dexClInit = env->GetMethodID(
      dexClClass, "<init>", "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V");
  auto buffer = env->NewDirectByteBuffer(dexVector.data(), dexVector.size());
  auto dexCl = env->NewObject(dexClClass, dexClInit, buffer, systemClassLoader);

  LOGD("load class");
  auto loadClass = env->GetMethodID(clClass, "loadClass",
                                    "(Ljava/lang/String;)Ljava/lang/Class;");
  auto entryClassName = env->NewStringUTF(entryClass.c_str());
  auto entryClassObj =
      (jclass)env->CallObjectMethod(dexCl, loadClass, entryClassName);

  LOGD("call entry class");
  auto entryMethodId = env->GetStaticMethodID(
      entryClassObj, entryMethodName.c_str(), entryMethodsignature.c_str());
  env->CallStaticVoidMethod(entryClassObj, entryMethodId, args...);
  return;
}

#endif  // DEXUTIL_HPP
