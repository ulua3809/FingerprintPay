#!/bin/bash
set -e
cd ${0%/*}

bash ./build.sh :module:flashRelease ./src/gradle/qq.gradle      Zygisk
bash ./build.sh :module:flashRelease ./src/gradle/qq.gradle      Riru
bash ./build.sh :module:flashRelease ./src/gradle/alipay.gradle  Zygisk
bash ./build.sh :module:flashRelease ./src/gradle/alipay.gradle  Riru
bash ./build.sh :module:flashRelease ./src/gradle/taobao.gradle  Zygisk
bash ./build.sh :module:flashRelease ./src/gradle/taobao.gradle  Riru
bash ./build.sh :module:flashRelease ./src/gradle/wechat.gradle  Zygisk
bash ./build.sh :module:flashRelease ./src/gradle/wechat.gradle  Riru
adb shell "rm -fv /data/local/tmp/lib*.debug.dex"