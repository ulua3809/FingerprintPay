#include <limits.h>
#include <sys/system_properties.h>
#include <unistd.h>

#include <string>
#include <vector>

#include "biometricPay.hpp"
#include "const.hpp"
#include "zygisk.hpp"

ssize_t byteread(int fd, void *buffer, size_t count) {
  ssize_t total = 0;
  char *buf = (char *)buffer;
  while (count > 0) {
    ssize_t ret = read(fd, buf, count);
    if (ret < 0) {
      if (errno == EINTR) {
        continue;
      }
      return -1;
    }
    // EOF
    if (ret == 0) {
      break;
    }
    buf += ret;
    total += ret;
    count -= ret;
  }
  return total;
}

ssize_t bytewrite(int fd, const void *buffer, size_t count) {
  ssize_t total = 0;
  const char *buf = (const char *)buffer;
  while (count > 0) {
    ssize_t ret = write(fd, buf, count);
    if (ret < 0) {
      if (errno == EINTR) {
        continue;
      }
      return -1;
    }
    buf += ret;
    total += ret;
    count -= ret;
  }
  return total;
}

bool confExists(const char *name) {
  if (name == nullptr) {
    LOGD("File name is null");
    return false;
  }
  char path[PATH_MAX] = {0};
  int format_result = snprintf(path, sizeof(path), "%s/%s", CONF_DIR, name);
  if (format_result < 0) {
    LOGD("Path formatting failed");
    return false;
  } else if (format_result >= (int)sizeof(path)) {
    LOGD("Path exceeds maximum length (%d bytes)", PATH_MAX);
    return false;
  }
  if (access(path, F_OK) == 0) {
    return true;
  }
  // LOGD("access %s failed: %s", path, strerror(errno));
  return false;
}

void serializeToFile(const companionStruct *obj, int fd) {
  // 写入 confFlags (1字节)
  bytewrite(fd, reinterpret_cast<const char *>(&obj->confFlags),
            sizeof(obj->confFlags));
  // 写入 dexVector 的大小 (4字节, uint32_t)
  uint32_t size = static_cast<uint32_t>(obj->dexVector.size());
  bytewrite(fd, reinterpret_cast<const char *>(&size), sizeof(size));
  // 写入 dexVector 数据
  if (!obj->dexVector.empty()) {
    bytewrite(fd, reinterpret_cast<const char *>(obj->dexVector.data()),
              obj->dexVector.size());
  }
  return;
}

companionStruct deserializeFromFile(int fd) {
  uint8_t confFlags;
  byteread(fd, reinterpret_cast<char *>(&confFlags), sizeof(confFlags));
  uint32_t vecSize = 0;
  byteread(fd, reinterpret_cast<char *>(&vecSize), sizeof(vecSize));
  std::vector<uint8_t> dexVec(vecSize);
  if (vecSize > 0) {
    byteread(fd, reinterpret_cast<char *>(dexVec.data()), vecSize);
  }
  return companionStruct(confFlags, std::move(dexVec));
}

uint8_t getconfigFlag() {
  uint8_t confFlag = 0;
  if (confExists("alipay")) {
    confFlag |= ConfigBits::alipay;
  }
  if (confExists("qq")) {
    confFlag |= ConfigBits::qq;
  }
  if (confExists("taobao")) {
    confFlag |= ConfigBits::taobao;
  }
  if (confExists("unionpay")) {
    confFlag |= ConfigBits::unionpay;
  }
  if (confExists("wechat")) {
    confFlag |= ConfigBits::wechat;
  }
  return confFlag;
}
std::vector<uint8_t> readFile(const char *path) {
  std::vector<uint8_t> vector;
  FILE *file = fopen(path, "rb");
  if (file) {
    fseek(file, 0, SEEK_END);
    long size = ftell(file);
    fseek(file, 0, SEEK_SET);
    vector.resize(size);
    fread(vector.data(), 1, size, file);
    fclose(file);
  } else {
    LOGD("Couldn't read %s file!", path);
  }
  return vector;
}

/**
 * @brief Compares process cmdline to target.
 *
 * @param nicename process cmdline aka &args->nice_name
 * @param target compare target
 * @return true if strings match according to rules
 * Examples:
 * - ("app", "app:")    → true
 * - ("app:le", "app:")    → false
 * - ("app:le", "app")  → true
 * - ("app", "app:le")  → false
 * - ("ap:ple", "apple")→ false
 *
 * @note Important Limitations:
 * - Requires NULL-terminated valid C-strings
 * - Undefined behavior if inputs are non-terminated buffers
 */
bool niceeq(const char *nicename, const char *target) {
  if (nicename == NULL && target == NULL) {
    return true;
  } else if (nicename == NULL || target == NULL) {
    return false;
  } else {
    const char *niceProcessName = strchr(nicename, ':');
    const char *targetProcessName = strchr(target, ':');
    // both has or dont has processName
    if ((niceProcessName == NULL) == (targetProcessName == NULL)) {
      return strcmp(nicename, target) == 0;
    } else if (niceProcessName == NULL && targetProcessName != NULL) {
      if (strlen(targetProcessName) == 1) {
        return (strlen(nicename) == targetProcessName - target) &&
               strncmp(nicename, target, targetProcessName - target) == 0;
      }
      return false;
    } else {
      return (strlen(target) == niceProcessName - nicename) &&
             strncmp(nicename, target, niceProcessName - nicename) == 0;
    }
  }
}
