#!/bin/bash
set -e
cd ${0%/*}
cd ../3rdparty/Riru-ModuleTemplate
git reset --hard HEAD
git clean -df .