#!/bin/bash
set -e
cd ${0%/*}

bash ./build.sh :module:flashRelease ./src/gradle/qq.gradle
bash ./build.sh :module:flashRelease ./src/gradle/alipay.gradle
bash ./build.sh :module:flashRelease ./src/gradle/taobao.gradle
bash ./build.sh :module:flashRelease ./src/gradle/wechat.gradle
adb shell "rm -fv /data/local/tmp/libriru*.dex"