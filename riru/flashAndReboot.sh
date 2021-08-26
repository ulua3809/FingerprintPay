#!/bin/bash
set -e
cd ${0%/*}
bash ./flash.sh
adb reboot