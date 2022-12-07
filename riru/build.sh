#!/bin/bash
set -e
cd ${0%/*}
MODULE_GRALDE_TASK="$1"
MODULE_GRALDE_FILE="$2"
PLUGIN_TYPE_NAME="$3"
MODULE_TEMPLATE="./Riru-ModuleTemplate"
VERSION=$(cat ../app/build.gradle| grep versionName | sed -E 's/.+"(.+)".*/\1/g')
APP_PRODUCT_TARGET=$(echo "$MODULE_GRALDE_FILE"|sed -E 's/.+\/(.+)\..+/\1/g')
MODULE_LIB_NAME="$(echo "$PLUGIN_TYPE_NAME" | tr '[:upper:]' '[:lower:]')-module-xfingerprint-pay-$APP_PRODUCT_TARGET"
echo VERSION: $VERSION
bash ./reset.sh
echo "updateJson=\${updateJson}" >> $MODULE_TEMPLATE/template/magisk_module/module.prop
perl -i -pe  's/(description: moduleDescription,)/$1 \nupdateJson: moduleUpdateJson,/g'  $MODULE_TEMPLATE/module/build.gradle

cp -rfv ./src/cpp/* $MODULE_TEMPLATE/module/src/main/cpp/
cp -rfv "$MODULE_GRALDE_FILE" $MODULE_TEMPLATE/module.gradle
cp -rfv "./src/gradle/fingerprint.gradle" $MODULE_TEMPLATE/
if [ -f "../local.properties" ]; then cp -rfv ../local.properties $MODULE_TEMPLATE/; fi
if [ "$PLUGIN_TYPE_NAME" == "Zygisk" ]; then
  echo "ZYGISK_MODULE_LIB_NAME=\"$MODULE_LIB_NAME\"" > $MODULE_TEMPLATE/template/magisk_module/customize.sh
  cat ./src/zygisk/customize.sh >> $MODULE_TEMPLATE/template/magisk_module/customize.sh
  rm -f $MODULE_TEMPLATE/template/magisk_module/riru.sh
else
  cat ./src/magisk/customize.sh >> $MODULE_TEMPLATE/template/magisk_module/customize.sh
fi
echo "rm -f \"/data/local/tmp/lib$MODULE_LIB_NAME.dex\" || true" >> $MODULE_TEMPLATE/template/magisk_module/uninstall.sh
cat ./src/magisk/post-fs-data.sh > $MODULE_TEMPLATE/template/magisk_module/post-fs-data.sh
chmod 0755 $MODULE_TEMPLATE/template/magisk_module/post-fs-data.sh
perl -0777 -i -pe  's/(forkAndSpecializePre[\W\w]+?{[\W\w]+?)}/$1    fingerprintPre(env, appDataDir, niceName);\n}/'  $MODULE_TEMPLATE/module/src/main/cpp/main.cpp
perl -0777 -i -pe  's/(forkAndSpecializePost[\W\w]+?{[\W\w]*?)}/$1    fingerprintPost(env, MAGISK_MODULE_TYPE_RIRU);\n    }/'  $MODULE_TEMPLATE/module/src/main/cpp/main.cpp
perl -0777 -i -pe  's/(specializeAppProcessPre[\W\w]+?{[\W\w]+?)}/$1    fingerprintPre(env, appDataDir, niceName);\n}/'  $MODULE_TEMPLATE/module/src/main/cpp/main.cpp
perl -0777 -i -pe  's/(specializeAppProcessPost[\W\w]+?{[\W\w]+?)}/$1    fingerprintPost(env, MAGISK_MODULE_TYPE_RIRU);\n}/'  $MODULE_TEMPLATE/module/src/main/cpp/main.cpp
perl -0777 -i -pe  's/^/#include "fingerprint.h"\n/'  $MODULE_TEMPLATE/module/src/main/cpp/main.cpp
perl -i -pe  's/(main\.cpp)/$1 fingerprint.cpp zygisk_main.cpp/g'  $MODULE_TEMPLATE/module/src/main/cpp/CMakeLists.txt
echo 'add_definitions(-DMODULE_NAME="${MODULE_NAME}")' >> $MODULE_TEMPLATE/module/src/main/cpp/CMakeLists.txt
echo 'target_link_libraries(${MODULE_NAME})' >> $MODULE_TEMPLATE/module/src/main/cpp/CMakeLists.txt
$MODULE_TEMPLATE/gradlew -p $MODULE_TEMPLATE clean \
  -PVERSION=$VERSION \
  -PPLUGIN_TYPE_NAME=$PLUGIN_TYPE_NAME \
  -PMODULE_LIB_NAME=$MODULE_LIB_NAME \

$MODULE_TEMPLATE/gradlew -p $MODULE_TEMPLATE $MODULE_GRALDE_TASK \
  -PVERSION=$VERSION \
  -PPLUGIN_TYPE_NAME=$PLUGIN_TYPE_NAME \
  -PMODULE_LIB_NAME=$MODULE_LIB_NAME \

if [ ! -d "./build/release" ]; then mkdir -p "./build/release"; fi
find $MODULE_TEMPLATE/out -name "*.zip" | xargs -I{} bash -c "cp -fv {} ./build/release/\$(basename {})"
ZIPNAME=$(ls $MODULE_TEMPLATE/out/ | grep -E "\.zip$" | head -n1 | sed  -E 's/-[A-Za-z]+-v/-all-v/g')
zip -j -u ./build/release/$ZIPNAME $MODULE_TEMPLATE/out/*.zip
bash ./reset.sh

