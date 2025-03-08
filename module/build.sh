#!/bin/bash
set -e
cd ${0%/*}

MODULE_author="ulua3809 \&\& Jason Eric"
MODULE_description="让支付宝、QQ、淘宝、云闪付、微信支持生物支付 biometric pay for Alipay,QQ,Taobao,UnionPay,WeChat."
# ADD your updateJson here
MODULE_updateJson=""
MODULE_Id="zygisk-biometric-pay"
MODULE_Name="zygisk-生物识别支付"
# get version info from app
MODULE_verName=$(cat ../app/build.gradle| grep versionName | sed -E 's/.+"(.+)".*/\1/g')
MODULE_verCode=$(cat ../app/build.gradle| grep versionCode | sed -E 's/.+versionCode +([0-9]+).*/\1/g')
MODULE_commithash=$(git -C ../ rev-parse --verify --short HEAD)

# all apps shared same dex,do not add suffiix
INJECT_DEX_NAME="zygisk-biometric-pay"
GRALDE_TASK_ZYGISK_MODULE=":module:zipRelease"
PATH_ZYGISK_MODULE_TEMPLATE="../3rdparty/zygisk-module-template"
PATH_TEMP_MODULE_TEMPLATE='./moduleBuildTmp'
PATH_APP_PROJ='..'
File_BuildDex='../app/build/intermediates/dex/release/mergeDexRelease/classes.dex'

echo "[Info] setting up ZYGISK MODULE TEMPLATE"
# reset PATH_ZYGISK_MODULE_TEMPLATE prebuild
bash ./reset.sh "$PATH_ZYGISK_MODULE_TEMPLATE"
# cleaning PATH_TEMP_MODULE_TEMPLATE prebuild
rm -rfv "$PATH_TEMP_MODULE_TEMPLATE"
cp -rfv ./template "$PATH_TEMP_MODULE_TEMPLATE"
echo "=================================="
echo "MODULE_VERSION_NAME: $MODULE_verName"
echo "MODULE_VERSION_CODE: $MODULE_verCode"
echo "MODULE_CommitHash: $MODULE_commithash"
echo "=================================="
echo "[Info] writing module infos"
# copy readme
cp -fv "../README.md" "$PATH_ZYGISK_MODULE_TEMPLATE/"
# write author
sed -i "s/#replace-me-with-author/${MODULE_author}/g" "$PATH_TEMP_MODULE_TEMPLATE/module.prop"
# write updateJson
sed -i "s@#replace-me-with-updateJson@${MODULE_updateJson}@g" "$PATH_TEMP_MODULE_TEMPLATE/module.prop"
# write description
sed -i "s/#replace-me-with-desc/${MODULE_description}/g" "$PATH_TEMP_MODULE_TEMPLATE/module.prop"
# write MODULE_Id
sed -i -r "s/val moduleId by extra\(\".*\"\)/val moduleId by extra\(\"${MODULE_Id}\"\)/g" "$PATH_ZYGISK_MODULE_TEMPLATE/build.gradle.kts"
# write MODULE_Name
sed -i -r "s/val moduleName by extra\(\".*\"\)/val moduleName by extra\(\"${MODULE_Name}\"\)/g" "$PATH_ZYGISK_MODULE_TEMPLATE/build.gradle.kts"
# write MODULE_verName
sed -i -r "s/val verName by extra\(\".*\"\)/val verName by extra\(\"${MODULE_verName}\"\)/g" "$PATH_ZYGISK_MODULE_TEMPLATE/build.gradle.kts"
# write MODULE_commithash
sed -i -r "s/val commitHash by extra\(.*\)/val commitHash by extra\(\"${MODULE_commithash}\"\)/g" "$PATH_ZYGISK_MODULE_TEMPLATE/build.gradle.kts"
# write MODULE_verCode
sed -i -r "s/val verCode by extra\(.*\)/val verCode by extra\(${MODULE_verCode}\)/g" "$PATH_ZYGISK_MODULE_TEMPLATE/build.gradle.kts"
# use moduleid to name output zip
# because github release file name cant use chinese
sed -i "s/\$moduleName-\$verName/\$moduleId-\$verName/g" "$PATH_ZYGISK_MODULE_TEMPLATE/module/build.gradle.kts"
# disable eof fix
sed -i "s@filter<FixCrLfFilter>@//filter<FixCrLfFilter>@g" "$PATH_ZYGISK_MODULE_TEMPLATE/module/build.gradle.kts"

# we dont want build x86 abi
# no one would use fingerprintpay on x86 device.would you?^_^
sed -i -r "s/val abiList by extra\(.*\)/val abiList by extra\(listOf\(\"arm64-v8a\", \"armeabi-v7a\"\)\)/g" "$PATH_ZYGISK_MODULE_TEMPLATE/build.gradle.kts"
# write INJECT_DEX_NAME
sed -i "s/#replace-me-with-INJECT_DEX_NAME/${INJECT_DEX_NAME}/g" "$PATH_TEMP_MODULE_TEMPLATE/customize.sh"
# merge the template
echo "$(cat "$PATH_ZYGISK_MODULE_TEMPLATE/module/template/customize.sh" "$PATH_TEMP_MODULE_TEMPLATE/customize.sh")" > "$PATH_TEMP_MODULE_TEMPLATE/customize.sh"
# remove some gitkeep
rm -fv "$PATH_TEMP_MODULE_TEMPLATE/dex/.gitkeep"
rm -fv "$PATH_TEMP_MODULE_TEMPLATE/config/.gitkeep"

if [ ! -f $File_BuildDex ];then
echo '[Warning] dex file not exist, satrt building dex'
# building dex here
"$PATH_APP_PROJ/gradlew" -p "$PATH_APP_PROJ" clean :app:mergeDexRelease
echo '[Info] dex file build success'
fi

getsha256(){
  s256=$(sha256sum $1 | tr -d '\n')
  echo -n ${s256% *}
}

echo "[Info] dex file found -> $File_BuildDex"
dexsha256="$(getsha256 "$File_BuildDex")"
echo "[Info] sha256 : ${dexsha256}"

cp -fv "$File_BuildDex" "${PATH_TEMP_MODULE_TEMPLATE}/dex/${INJECT_DEX_NAME}.dex"
cp -rfv "${PATH_TEMP_MODULE_TEMPLATE}"/. "$PATH_ZYGISK_MODULE_TEMPLATE/module/template/"

# remove module example
rm -fv "$PATH_ZYGISK_MODULE_TEMPLATE/module/src/main/cpp/example.cpp"
echo '[Info] copy module source code to template'
cp -rfv ./src/cpp/. "$PATH_ZYGISK_MODULE_TEMPLATE/module/src/main/cpp/"
sed -i 's/example\.cpp/biometricPay.cpp dexutil.cpp/g' "$PATH_ZYGISK_MODULE_TEMPLATE/module/src/main/cpp/CMakeLists.txt"

sed -i "s/#{{inject_dex_name}}/${INJECT_DEX_NAME}/g" "$PATH_ZYGISK_MODULE_TEMPLATE/module/src/main/cpp/const.hpp"
sed -i "s/#{{module-id}}/${MODULE_Id}/g" "$PATH_ZYGISK_MODULE_TEMPLATE/module/src/main/cpp/const.hpp"

echo '[Info] building zygisk module'
$PATH_ZYGISK_MODULE_TEMPLATE/gradlew -p "$PATH_ZYGISK_MODULE_TEMPLATE" $GRALDE_TASK_ZYGISK_MODULE

# after build
if [ ! -d "./build/release" ]; then mkdir -p "./build/release"; fi
echo -n ${dexsha256} > "./build/release/${INJECT_DEX_NAME}.dex.sha256"
cp -fv "$PATH_TEMP_MODULE_TEMPLATE/dex/${INJECT_DEX_NAME}.dex" "./build/release/"
cp -fv "$PATH_ZYGISK_MODULE_TEMPLATE"/module/release/*.zip "./build/release/"

echo '[Info] zygisk module build success'
