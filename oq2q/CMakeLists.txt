cmake_minimum_required(VERSION 3.5)
project(oq2q)

include_directories(
	${LIBOSCAR_INCLUDE_DIR}
)

set(OQ2Q_SOURCES_CPP
	src/main.cpp
)

SET(OQ2Q_LINK_LIBS
	liboscar
)

add_executable(${PROJECT_NAME} ${OQ2Q_SOURCES_CPP})
target_link_libraries(${PROJECT_NAME} ${OQ2Q_LINK_LIBS})
