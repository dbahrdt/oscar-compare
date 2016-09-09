cmake_minimum_required(VERSION 3.5)
project(foq)

include_directories(
	${LIBOSCAR_INCLUDE_DIR}
)

set(FOQ_SOURCES_CPP
	src/main.cpp
)

SET(FOQ_LINK_LIBS
	liboscar
)

add_executable(${PROJECT_NAME} ${FOQ_SOURCES_CPP})
target_link_libraries(${PROJECT_NAME} ${FOQ_LINK_LIBS})
