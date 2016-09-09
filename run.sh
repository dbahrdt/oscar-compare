#!/bin/bash

#params:
#JOSCAR_LIB_PATH=<path to joscar native lib>
#REMOTE_DEBUG = "y|n"
#DEBUG="y|n"
#CGDB_DEBUG="y|n"
#TIME_BENCH="y|n"

SCRIPT=$(readlink -f $0)
SCRIPTPATH=`dirname $SCRIPT`
BASE_PATH=${SCRIPTPATH}


if [ "${NO_SETUP}" != "y" ]; then
	${BASE_PATH}/setup.sh
fi

if [ -z "${JOSCAR_LIB_PATH}" ]; then
	JOSCAR_LIB_PATH="${BASE_PATH}/build/libjoscar"
fi

pathchk "${JOSCAR_LIB_PATH}" || exit 1

if [ ! -d ${JOSCAR_LIB_PATH} ]; then
	echo "Path to libjoscar is missing"
	exit 1
fi

if [ ! -f "${JOSCAR_LIB_PATH}/libjoscar.so" ]; then
	echo "libjoscar is missing in JOSCAR_LIB_PATH=${JOSCAR_LIB_PATH}"
fi

if [ "${DEBUG}" = "y" ]; then
	DEBUG_OPTIONS="-ea"
fi

cd "${BASE_PATH}"

LD_LIBRARY_PATH="${JOSCAR_LIB_PATH}:${JOSCAR_LIB_PATH}/liboscar:${LD_LIBRARY_PATH}"
export LD_LIBRARY_PATH

if [ "${REMOTE_DEBUG}" = "y" ]; then
	java -agentlib:jdwp=transport=dt_socket,address=27000,server=y,suspend=y ${DEBUG_OPTIONS} -jar java/dist/bin/ocse-bundle.jar $@ &
	MY_PID=$!
	#echo "Listening with pid=$MY_PID"
	#echo "stop in Main.main" > .jdbrc
	#echo "run $@" >> .jdbrc
	#enter "cont" to continue, but jdb sucks anyway
	#jdb -attach 27000
	# kill $MY_PID
else
	if [ "${CGDB_DEBUG}" = "y" ] || [ "${USE_GDB}" = "y" ]; then
		cgdb --args /usr/bin/java -d64 -Xmx8g ${DEBUG_OPTIONS} -jar java/dist/bin/ocse-bundle.jar $@
	else
		if [ "${TIME_BENCH}" = "y" ]; then
			/usr/bin/time -v /usr/bin/java -d64 -Xmx8g ${DEBUG_OPTIONS} -jar java/dist/bin/ocse-bundle.jar $@;
		elif [ -n "${TIME_BENCH}" ]; then
			/usr/bin/time -v -o "${TIME_BENCH}" /usr/bin/java -d64 -Xmx8g ${DEBUG_OPTIONS} -jar java/dist/bin/ocse-bundle.jar $@;
		else
			/usr/bin/java -d64 -Xmx8g ${DEBUG_OPTIONS} -jar java/dist/bin/ocse-bundle.jar $@;
		fi
	fi
fi
