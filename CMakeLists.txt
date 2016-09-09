cmake_minimum_required(VERSION 3.9)
project(ocse)

# set -Wshadow-compatible-local as soon as supported
set(MY_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra -Wno-unknown-pragmas -Wstrict-overflow=3 -frounding-math")
set(MY_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -Wno-unknown-pragmas -Wstrict-overflow=3 -frounding-math")# -Wshorten-64-to-32")
set(DEBUG_FLAGS "${DEBUG_FLAGS}")#-Wconversion")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${MY_C_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${MY_CXX_FLAGS}")
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} ${DEBUG_FLAGS}")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} ${DEBUG_FLAGS}")
set(CMAKE_C_FLAGS_RELWITHDEBINFO "${CMAKE_C_FLAGS_RELWITHDEBINFO} ${DEBUG_FLAGS}")
set(CMAKE_CXX_FLAGS_RELWITHDEBINFO "${CMAKE_CXX_FLAGS_RELWITHDEBINFO} ${DEBUG_FLAGS}")

set(CMAKE_MODULE_PATH
	${CMAKE_SOURCE_DIR}/cmake
)

include(add_buildtype_ultra)
include(add_buildtype_lto)

add_subdirectory(vendor/libjoscar libjoscar)
add_subdirectory(oq2q)
add_subdirectory(foq)
add_subdirectory(oqs)
