cmake_minimum_required(VERSION 3.22.1)

# 设置项目名称
project("serial_port")


# 创建共享库
add_library(
        serial_port
        SHARED
        SerialPort.c
)

# 查找日志库
find_library(
        log-lib
        log
)


target_link_libraries(
        serial_port
        ${log-lib}
)

# 设置包含目录
include_directories(src/main/cpp)
