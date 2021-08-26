#!/bin/bash
set -e
cd ${0%/*}

bash ./build.sh :module:assembleRelease ./src/gradle/qq.gradle
bash ./build.sh :module:assembleRelease ./src/gradle/alipay.gradle
bash ./build.sh :module:assembleRelease ./src/gradle/taobao.gradle
bash ./build.sh :module:assembleRelease ./src/gradle/wechat.gradle
