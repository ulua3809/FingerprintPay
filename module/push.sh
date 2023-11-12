#!/bin/bash
set -e
cd ${0%/*}
VERSION=$(cat ../app/build.gradle| grep versionName | sed -E 's/.+"(.+)".*/\1/g')
echo VERSION: $VERSION
bash ./build.sh ./src/gradle/qq.gradle
./Riru-moduleTemplate/gradlew -p ./Riru-moduleTemplate clean :module:pushRelease -PVERSION=$VERSION

bash ./build.sh ./src/gradle/alipay.gradle
./Riru-moduleTemplate/gradlew -p ./Riru-moduleTemplate clean :module:pushRelease -PVERSION=$VERSION

bash ./build.sh ./src/gradle/taobao.gradle
./Riru-moduleTemplate/gradlew -p ./Riru-moduleTemplate clean :module:pushRelease -PVERSION=$VERSION

bash ./build.sh ./src/gradle/wechat.gradle
./Riru-moduleTemplate/gradlew -p ./Riru-moduleTemplate clean :module:pushRelease -PVERSION=$VERSION