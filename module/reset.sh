#!/bin/bash
set -e
cd ${0%/*}
cd $1
git reset --hard HEAD
git clean -df .