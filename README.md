# 电机控制系统（LNController）

一个 Android 端电机控制应用，通过 UART 串口与下位机通信，实现电机的正转 / 反转 / 停止、三档切换与转速百分比控制。

## 技术栈

- Kotlin + Java（Android）
- JNI + C（NDK / CMake）实现串口底层读写
- `termios` 配置串口参数（波特率、数据位、校验位、停止位）
- 自定义文本命令协议与串口数据收发

## 功能

- 电机正转 / 反转 / 停止
- 三档切换（一档 / 二档 / 三档）
- 转速百分比输入（0–100，含输入校验）
- 「开始 / 结束」开关统一控制所有控制按钮使能，防止误触
- 串口文本与十六进制数据收发，支持字节 ↔ 十六进制转换
- 串口设备扫描（解析 `/proc/tty/drivers` 与 `/dev`）

## 目录结构（核心）

```text
app/src/main/
├── cpp/
│   ├── CMakeLists.txt      # NDK 构建配置
│   ├── SerialPort.c        # JNI：termios 打开/配置串口
│   └── SerialPort.h
├── java/com/example/myapplication/
│   ├── MainActivity.kt     # 电机控制 UI 与命令下发
│   ├── SerialPort.java     # JNI 串口封装（Builder 模式）
│   ├── SerialPortFinder.java
│   └── SerialPortManager.java  # 单例、读写线程、数据转换
└── res/
    └── layout/activity_main.xml
```

## 运行

1. 使用 Android Studio 打开工程。
2. 在 `local.properties` 配置你的 Android SDK 路径（该文件不纳入版本控制）。
3. 默认串口为 `/dev/ttyS6`，波特率 9600，8N1，可在 `MainActivity.kt` 的 `connectUART()` 中修改。

`minSdk` 26，`targetSdk` 34。

## 归属说明

- `SerialPort.java`、`SerialPortFinder.java` 与 `SerialPort.c` 的串口底层基于开源库 [android-serialport-api](https://github.com/cepr/android-serialport-api)（Cedric Priscal，Apache License 2.0），用于 JNI 串口读写。
- 电机控制逻辑、命令协议、`SerialPortManager` 封装与 UI 交互为个人实现。

## License

本项目中的第三方串口底层代码遵循 Apache License 2.0；其余部分如需再许可，请自行补充。
