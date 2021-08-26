#!/bin/bash
set -e
cd ${0%/*}
MODULE_GRALDE_TASK="$1"
MODULE_GRALDE_FILE="$2"
MODULE_TEMPLATE="./Riru-ModuleTemplate"
VERSION=$(cat ../app/build.gradle| grep versionName | sed -E 's/.+"(.+)".*/\1/g')
echo VERSION: $VERSION
bash ./reset.sh
cp -rfv ./src/cpp/* $MODULE_TEMPLATE/module/src/main/cpp/
cp -rfv "$MODULE_GRALDE_FILE" $MODULE_TEMPLATE/module.gradle
cp -rfv "./src/gradle/fingerprint.gradle" $MODULE_TEMPLATE/
if [ -f "../local.properties" ]; then cp -rfv ../local.properties $MODULE_TEMPLATE/; fi
cat ./src/magisk/customize.sh >> $MODULE_TEMPLATE/template/magisk_module/customize.sh
perl -0777 -i -pe  's/(forkAndSpecializePre[\W\w]+?{[\W\w]+?)}/$1    fingerprintPre(env, appDataDir, niceName);\n}/'  $MODULE_TEMPLATE/module/src/main/cpp/main.cpp
perl -0777 -i -pe  's/(forkAndSpecializePost[\W\w]+?{[\W\w]*?)}/$1    fingerprintPost(env);\n    }/'  $MODULE_TEMPLATE/module/src/main/cpp/main.cpp
perl -0777 -i -pe  's/(specializeAppProcessPre[\W\w]+?{[\W\w]+?)}/$1    fingerprintPre(env, appDataDir, niceName);\n}/'  $MODULE_TEMPLATE/module/src/main/cpp/main.cpp
perl -0777 -i -pe  's/(specializeAppProcessPost[\W\w]+?{[\W\w]+?)}/$1    fingerprintPost(env);\n}/'  $MODULE_TEMPLATE/module/src/main/cpp/main.cpp
perl -0777 -i -pe  's/^/#include "fingerprint.h"\n/'  $MODULE_TEMPLATE/module/src/main/cpp/main.cpp
perl -i -pe  's/(main\.cpp)/$1 fingerprint.cpp/g'  $MODULE_TEMPLATE/module/src/main/cpp/CMakeLists.txt
echo 'add_definitions(-DMODULE_NAME="${MODULE_NAME}")' >> $MODULE_TEMPLATE/module/src/main/cpp/CMakeLists.txt
$MODULE_TEMPLATE/gradlew -p $MODULE_TEMPLATE clean $MODULE_GRALDE_TASK -PVERSION=$VERSION
if [ ! -d "./build/release" ]; then mkdir -p "./build/release"; fi
find $MODULE_TEMPLATE/out -name "*.zip" | xargs -I{} bash -c "cp -fv {} ./build/release/magisk-\$(basename {})"
bash ./reset.sh

