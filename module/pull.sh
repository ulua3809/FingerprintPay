#!/bin/bash
set -e
cd ${0%/*}
bash ./reset.sh
git pull
