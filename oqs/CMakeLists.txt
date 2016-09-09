cmake_minimum_required(VERSION 3.5)
project(oqs)

include_directories(
	${LIBOSCAR_INCLUDE_DIR}
)

set(OQS_SOURCES_CPP
	src/main.cpp
)

SET(OQS_LINK_LIBS
	liboscar
)

add_executable(${PROJECT_NAME} ${OQS_SOURCES_CPP})
target_link_libraries(${PROJECT_NAME} ${OQS_LINK_LIBS})
