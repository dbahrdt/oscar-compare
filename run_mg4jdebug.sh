#!/bin/bash

#params:
#REMOTE_DEBUG = y|n
#JOSCAR_LIB_PATH=<path to joscar native lib>


BASE_PATH=$(pwd)
DEFAULT_OPTIONS=""

if [ ! -d "${JOSCAR_LIB_PATH}" ]; then
	JOSCAR_LIB_PATH="build"
fi

cd "${BASE_PATH}/vendor/libjoscar/java" && ant || exit
if [ -e "${BASE_PATH}/java/vendor/mg4j-src" ]; then
	cd "${BASE_PATH}/java/vendor/mg4j-src" && ant || exit
fi
cd "${BASE_PATH}/java" && ant mg4jdebug || exit
cd "${BASE_PATH}"



LD_LIBRARY_PATH="$(pwd)/${JOSCAR_LIB_PATH}/libjoscar:$(pwd)/${JOSCAR_LIB_PATH}/libjoscar/liboscar"
export LD_LIBRARY_PATH

#java go fuck yourself! jdb seems to be a piece of crap.
#Its not even possible to correct the entered command (except with backspace!)

if [ "${REMOTE_DEBUG}" = "y" ]; then
	java -agentlib:jdwp=transport=dt_socket,address=27000,server=y,suspend=y -ea -jar java/dist/bin/mg4jdebug.jar $@ &
	MY_PID=$!
	#echo "Listening with pid=$MY_PID"
	#echo "stop in Main.main" > .jdbrc
	#echo "run $@" >> .jdbrc
	#enter "cont" to continue, but jdb sucks anyway
	#jdb -attach 27000
	# kill $MY_PID
else
	java -d64 -Xmx8g -ea -jar java/dist/bin/mg4jdebug.jar $@
fi