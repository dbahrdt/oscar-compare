#!/bin/bash

SCRIPT=$(readlink -f $0)
SCRIPTPATH=`dirname $SCRIPT`
BASE_PATH=${SCRIPTPATH}

INITIAL_DIR=$(pwd)

cd "${BASE_PATH}/vendor/libjoscar/java" && ant || exit 1
#if [ -e "${BASE_PATH}/java/vendor/mg4j-src" ]; then
#	cd "${BASE_PATH}/java/vendor/mg4j-src" && ant || exit 1
#fi

if [ "${CLEAN}" = "y" ]; then
	cd "${BASE_PATH}/java" && ant clean || exit 1
	cd "${BASE_PATH}/vendor/libjoscar/java" && ant clean || exit 1
	cd "${BASE_PATH}"
fi

cd "${BASE_PATH}/vendor/libjoscar/java" && ant || exit 1
cd "${BASE_PATH}/java" && ant || exit 1

cd "${INITIAL_DIR}"
