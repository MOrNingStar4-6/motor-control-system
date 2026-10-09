    #ifndef SERIAL_PORT_H
    #define SERIAL_PORT_H

    #include <jni.h>

    #ifdef __cplusplus
    extern "C" {
    #endif

    /*
     * Class:     com_example_godemo_SerialPort
     * Method:    open
     * Signature: (Ljava/lang/String;IIIII)Ljava/io/FileDescriptor;
     */
    JNIEXPORT jobject JNICALL Java_com_example_myapplication_SerialPort_open(
            JNIEnv *env,
            jobject thiz,
            jstring path,
            jint baudrate,
            jint dataBits,
            jint parity,
            jint stopBits,
            jint flags
    );

    /*
     * Class:     com_example_godemo_SerialPort
     * Method:    close
     * Signature: ()V
     */
    JNIEXPORT void JNICALL Java_com_example_myapplication_SerialPort_close(
            JNIEnv *env,
    jobject thiz
    );

    #ifdef __cplusplus
    }
    #endif

    #endif // SERIAL_PORT_H