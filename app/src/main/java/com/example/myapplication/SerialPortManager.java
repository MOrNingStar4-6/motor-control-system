package com.example.myapplication;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;


public class SerialPortManager {
    private static final String TAG = "SerialPortManager";
    private static SerialPortManager instance;
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;

    private volatile boolean mIsOpen = false;
    public String mPort;
    public int mBaudrate;

    private OnDataReceivedListener mDataReceivedListener;

    public SerialPortManager() {

    }
    public synchronized boolean ensureConnected(String port, int baudrate, int dataBits, int parity, int stopBits) {
        if (isOpen() && this.mPort.equals(port) && this.mBaudrate == baudrate) {
            return true;
        }

        // 关闭现有连接
        close();

        // 重新连接
        this.mPort = port;
        this.mBaudrate = baudrate;
        return open(dataBits, parity, stopBits);
    }

    // 添加简单的连接方法
    public synchronized boolean ensureConnected() {
        return ensureConnected("/dev/ttyS6", 9600, 8, 0, 1);
    }
    public static  SerialPortManager getInstance()
    {
        if (instance == null)
        {
            instance =new SerialPortManager();

        }
        return  instance;
    }
    public interface OnDataReceivedListener {
        void onDataReceived(byte[] buffer, int size);

        void onError(String error);
    }


    public SerialPortManager(String port, int baudrate) {
        this.mPort = port;
        this.mBaudrate = baudrate;
    }


    public SerialPortManager(String devicePath, int baudRate, int dataBits, int parity, int stopBits) {
        this.mPort = devicePath;
        this.mBaudrate = baudRate;
        try {
            mSerialPort = new SerialPort(new File(devicePath), baudRate, dataBits, parity, stopBits);
            mInputStream = mSerialPort.getInputStream();
            mOutputStream = mSerialPort.getOutputStream();
            mIsOpen = true;
            startReadThread();
            Log.d(TAG, "串口打开成功(构造): " + devicePath + " baud=" + baudRate);
        } catch (Exception e) {
            Log.e(TAG, "串口打开失败(构造): " + e.getMessage(), e);
            mIsOpen = false;
            safeCloseInternal();
        }
    }
    public synchronized boolean validateConnection() {
        if (!mIsOpen || mOutputStream == null) {
            return false;
        }

        try {
            mOutputStream.flush();
            return true;
        } catch (IOException e) {
            Log.w(TAG, "Connection validation failed: " + e.getMessage());
            safeCloseInternal();
            return false;
        }
    }

    public synchronized boolean open() {
        return open(8, 0, 1); // 默认 8N1
    }


    public synchronized boolean open(int dataBits, int parity, int stopBits) {
        if (mIsOpen) {
            Log.w(TAG, "open: already open");
            return true;
        }
        try {
            mSerialPort = new SerialPort(new File(mPort), mBaudrate, dataBits, parity, stopBits);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            mIsOpen = true;
            startReadThread();
            Log.i(TAG, "Serial port opened: " + mPort + " baud=" + mBaudrate +
                    " dataBits=" + dataBits + " parity=" + parity + " stopBits=" + stopBits);
            return true;
        } catch (IOException | SecurityException e) {
            Log.e(TAG, "Failed to open serial port: " + e.getMessage(), e);
            safeCloseInternal();
            return false;
        }
    }

    private void startReadThread() {
        if (mReadThread != null && mReadThread.isAlive()) {
            return;
        }
        mReadThread = new ReadThread();
        mReadThread.start();
    }
    public synchronized boolean sendText(String text) {
        return sendText(text, "UTF-8");
    }

    /**
     * 发送字符串数据（使用指定编码）
     * @param text 要发送的字符串
     * @param charsetName 字符编码名称，如 "UTF-8", "GBK", "GB2312" 等
     * @return 发送是否成功
     */
    public synchronized boolean sendText(String text, String charsetName) {
        if (!mIsOpen || mOutputStream == null) {
            Log.e(TAG, "Send failed: Serial port not opened.");

            return false;
        }
        if (text == null) {
            Log.e(TAG, "Send failed: Text is null");
            return false;
        }
        try {
            byte[] data = text.getBytes(charsetName);
            mOutputStream.write(data);
            mOutputStream.flush();
            Log.d(TAG, "发送字符串: \"" + text + "\" (" + charsetName + ")");
            Log.d(TAG, "发送数据(十六进制): " + bytesToHex(data, 0, data.length));
            return true;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "发送字符串失败: 不支持的编码格式 - " + charsetName, e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "发送字符串失败: " + e.getMessage(), e);
            safeCloseInternal();
            return false;
        }
    }

    public synchronized boolean sendData(byte[] data) {
        return sendData(data, 0, data.length);
    }


    public synchronized boolean sendData(byte[] data, int offset, int length) {
        
        if (!mIsOpen || mOutputStream == null) {
            Log.e(TAG, "Send failed: Serial port not opened.");
            return false;
        }
        if (data == null || offset < 0 || length <= 0 || offset + length > data.length) {
            Log.e(TAG, "Send failed: Invalid data parameters");
            return false;
        }
        try {
            mOutputStream.write(data, offset, length);
            mOutputStream.flush();
            Log.d(TAG, "发送数据: " + bytesToHex(data, offset, length));
            return true;
        } catch (IOException e) {
            Log.e(TAG, "发送数据失败: " + e.getMessage(), e);
            safeCloseInternal();
            return false;
        }
    }


    public synchronized boolean sendHexData(String hexString) {
        if (!mIsOpen || mOutputStream == null) {
            Log.e(TAG, "Send failed: Serial port not opened.");
            return false;
        }
        try {
            byte[] data = hexStringToBytes(hexString);
            if (data == null) {
                Log.e(TAG, "Send failed: Invalid hex string");
                return false;
            }
            mOutputStream.write(data);
            mOutputStream.flush();
            Log.d(TAG, "发送十六进制数据: " + hexString);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "发送数据失败: " + e.getMessage(), e);
            safeCloseInternal();
            return false;
        }
    }

    public boolean isOpen() {
        return mIsOpen;
    }

    public String getPort() {
        return mPort;
    }

    public int getBaudrate() {
        return mBaudrate;
    }

    public void setOnDataReceivedListener(OnDataReceivedListener listener) {
        this.mDataReceivedListener = listener;
    }


    public synchronized void close() {
        if (mReadThread != null) {
            mReadThread.interrupt();
            try {
                mReadThread.join(100); // 等待读线程结束
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            mReadThread = null;
        }
        safeCloseInternal();
        Log.i(TAG, "Serial port closed.");
    }


    private void safeCloseInternal() {
        try {
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
        } catch (IOException e) {
            Log.w(TAG, "Error closing input stream: " + e.getMessage());
        }

        try {
            if (mOutputStream != null) {
                mOutputStream.close();
                mOutputStream = null;
            }
        } catch (IOException e) {
            Log.w(TAG, "Error closing output stream: " + e.getMessage());
        }

        try {
            if (mSerialPort != null) {
                mSerialPort.close();
                mSerialPort = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error closing serial port: " + e.getMessage());
        }

        mIsOpen = false;
    }

    /**
     * 读线程
     */
    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            byte[] buffer = new byte[1024];
            while (!isInterrupted() && mIsOpen) {
                try {
                    if (mInputStream == null) {
                        Log.w(TAG, "ReadThread: InputStream is null");
                        break;
                    }
                    int size = mInputStream.read(buffer);
                    if (size > 0) {
                        byte[] received = new byte[size];
                        System.arraycopy(buffer, 0, received, 0, size);
                        Log.d(TAG, "接收数据: " + bytesToHex(received, 0, size));
                        if (mDataReceivedListener != null) {
                            mDataReceivedListener.onDataReceived(received, size);
                        }
                    }
                } catch (IOException e) {
                    if (!isInterrupted()) {
                        Log.e(TAG, "ReadThread IOException: " + e.getMessage());
                    }
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "ReadThread Unexpected: " + e.getMessage());
                    break;
                }
            }
            Log.d(TAG, "ReadThread: exited");
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes, int offset, int length) {
        if (bytes == null || offset < 0 || length <= 0 || offset + length > bytes.length) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }

    /**
     * 十六进制字符串转字节数组
     */
    private byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.trim().isEmpty()) {
            return null;
        }

        String hex = hexString.replaceAll("\\s+", "").toUpperCase();
        if (hex.length() % 2 != 0) {
            return null;
        }

        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            try {
                data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                        + Character.digit(hex.charAt(i + 1), 16));
            } catch (Exception e) {
                return null;
            }
        }
        return data;
    }


    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }
}
