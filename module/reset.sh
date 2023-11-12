#!/bin/bash
set -e
cd ${0%/*}
git submodule update -f --remote
git submodule foreach git clean -df .