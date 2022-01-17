#!/bin/bash
set -e
cd ${0%/*}

bash ./build.sh :module:assembleRelease ./src/gradle/qq.gradle      Zygisk
bash ./build.sh :module:assembleRelease ./src/gradle/qq.gradle      Riru
bash ./build.sh :module:assembleRelease ./src/gradle/alipay.gradle  Zygisk
bash ./build.sh :module:assembleRelease ./src/gradle/alipay.gradle  Riru
bash ./build.sh :module:assembleRelease ./src/gradle/taobao.gradle  Zygisk
bash ./build.sh :module:assembleRelease ./src/gradle/taobao.gradle  Riru
bash ./build.sh :module:assembleRelease ./src/gradle/wechat.gradle  Zygisk
bash ./build.sh :module:assembleRelease ./src/gradle/wechat.gradle  Riru
