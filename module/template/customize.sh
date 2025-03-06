if [ $API -lt 26 ];then
  ui_print "*********************************************************"
  ui_print "! This module requires at least Android 8.0 !"
  ui_print "! Please update your system or use legacy module"
  abort    "*********************************************************"
fi

ui_print "- Extracting dex"
extract "$ZIPFILE" "dex/#replace-me-with-INJECT_DEX_NAME.dex" "$MODPATH"

checkKey(){
  timeoutsec=$1
  START_TIME=$(date +%s)
  while true ; do
    NOW_TIME=$(date +%s)
    ke0=$(timeout 1 getevent -lc 1 | grep KEY_VOLUME)
    # /dev/input/event0: EV_KEY       KEY_VOLUMEUP         DOWN            
    # /dev/input/event0: EV_KEY       KEY_VOLUMEUP         UP              
    ke1=$(echo "$ke0" | sed -r "s/.*(KEY_VOLUME)/\1/g")
    # KEY_VOLUMEUP         DOWN            
    # KEY_VOLUMEUP         UP              
    ke2=$(echo "$ke1" | sed -r "s/.* UP.*//g")
    # KEY_VOLUMEUP         DOWN            
    kevent=$(echo "$ke2" | sed -r "s/ +DOWN.*//g")
    # KEY_VOLUMEUP
    if [ $(( NOW_TIME - START_TIME )) -gt $timeoutsec ]; then
      echo "timeout"
      break
    elif $(echo "$kevent" | grep -q "KEY_VOLUMEUP" ); then
      echo "volUp"
      break
    elif $(echo "$kevent" | grep -q "KEY_VOLUMEDOWN" ); then
      echo "volDown"
      break
    fi
  done
}

ch_template(){
  # $1 appName $2 configName $3 chooseTimeout
  ui_print "- 为${1}-${2}启用指纹支付"
  ui_print "- [ 音量(+): 确定 ]"
  ui_print "- [ 音量(-): 取消 ]"
  result=$(checkKey $3)
  if [ $result = 'volUp' ]; then
    touch $MODPATH/config/${2}
    ui_print "- 已经为${1}-${2}启用指纹支付"
  elif  [ $result = 'volDown' ]; then
    rm -fv $MODPATH/config/${2}
    ui_print "- 不启用${1}-${2}指纹支付"
  else
    ui_print "- 选择超时，不启用${1}-${2}指纹支付"
    rm -fv $MODPATH/config/${2}
  fi
  ui_print "--------------------------------------------------------"
}
ui_print "- Setting default scope"
mkdir $MODPATH/config
unzip -o "$ZIPFILE" "config/*" -x "*.sha256" -d "$MODPATH"

ui_print "--------------------------------------------------------"
ui_print "- 开始自定义模块作用域，20秒内无按键输入则采用默认作用域(全部启用)"
ui_print "- 安装后可通过添加或删除模块目录下/config/中空文件来修改模块作用域"
ui_print "--------------------------------------------------------"
customize=1
# alipay
ui_print "- 为支付宝-alipay启用指纹支付"
ui_print "- [ 音量(+): 确定 ]"
ui_print "- [ 音量(-): 取消 ]"
result=$(checkKey 20)
if [ $result = 'volUp' ]; then
touch $MODPATH/config/alipay
ui_print "- 已经为支付宝-alipay启用指纹支付"
elif  [ $result = 'volDown' ]; then
rm -fv $MODPATH/config/alipay
ui_print "- 不启用支付宝-alipay指纹支付"
else
ui_print "- 选择超时，采用默认配置"
customize=0
fi
ui_print "--------------------------------------------------------"
if [ $customize -eq 1 ]; then
  # qq
  ch_template "QQ" "qq" 10
  # taobao
  ch_template "淘宝" "taobao" 10
  # unionpay
  ch_template "云闪付" "unionpay" 10
  # wechat
  ch_template "微信" "wechat" 10
fi
set_perm_recursive "$MODPATH" 0 0 0755 0644
