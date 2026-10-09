package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.SerialPortManager.OnDataReceivedListener

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var serialPortManager: SerialPortManager? = null
    private var buttonStart: Button? = null
    private var buttonOver: Button? = null
    private var buttonOneGear: Button? = null
    private var buttonTwoGear: Button? = null
    private var buttonThreeGear: Button? = null
    private var buttonForward: Button? = null
    private var buttonStop: Button? = null
    private var buttonBackward: Button? = null
    private var buttonSendSpeed: Button? = null
    private var textGear: TextView? = null
    private var textSpeedStatus: TextView? = null
    private var editSpeedPercent: EditText? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag", "ServiceCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            initUIComponents()
            setupButtonListeners()
            connectUART()
            closeAllButtons()
            Log.d(TAG, "onCreate: Activity initialization completed")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Initialization error", e)
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: 清理资源")
        try {
            serialPortManager?.close()
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy: 关闭串口失败", e)
        }
        Log.d(TAG, "onDestroy: 资源清理完成")
    }

    private fun initUIComponents() {
        Log.d(TAG, "initUIComponents: Initializing UI components")
        buttonStart = findViewById(R.id.buttonStart)
        buttonOver = findViewById(R.id.buttonOver)
        buttonOneGear = findViewById(R.id.buttonOneGear)
        buttonTwoGear = findViewById(R.id.buttonTwoGear)
        buttonThreeGear = findViewById(R.id.buttonThreeGear)
        buttonForward = findViewById(R.id.buttonForward)
        buttonBackward = findViewById(R.id.buttonBackward)
        buttonStop = findViewById(R.id.buttonStop)
        buttonSendSpeed = findViewById(R.id.buttonSendSpeed)
        textGear = findViewById(R.id.textGear)
        textSpeedStatus = findViewById(R.id.textSpeedStatus)
        editSpeedPercent = findViewById(R.id.editSpeedPercent)
    }

    private fun setupButtonListeners() {
        buttonStart?.setOnClickListener { openAllButtons() }
        buttonOver?.setOnClickListener { closeAllButtons() }
        buttonOneGear?.setOnClickListener {
            serialPortManager?.sendText("ONE_GEAR")
            textGear?.text = "当前档位：一档"
        }
        buttonTwoGear?.setOnClickListener {
            serialPortManager?.sendText("TWO_GEAR")
            textGear?.text = "当前档位：二档"
        }
        buttonThreeGear?.setOnClickListener {
            serialPortManager?.sendText("THREE_GEAR")
            textGear?.text = "当前档位：三档"
        }
        buttonForward?.setOnClickListener { serialPortManager?.sendText("FORWARD") }
        buttonBackward?.setOnClickListener { serialPortManager?.sendText("BACKWARD") }
        buttonStop?.setOnClickListener { serialPortManager?.sendText("STOP") }
        buttonSendSpeed?.setOnClickListener { sendSpeedPercent() }
    }

    private fun sendSpeedPercent() {
        val inputText = editSpeedPercent?.text?.toString()?.trim().orEmpty()
        if (inputText.isEmpty()) {
            Toast.makeText(this, "请输入转速百分比", Toast.LENGTH_SHORT).show()
            return
        }

        val speedPercent = inputText.toIntOrNull()
        if (speedPercent == null || speedPercent !in 0..100) {
            Toast.makeText(this, "转速百分比请输入 0 到 100 之间的整数", Toast.LENGTH_SHORT).show()
            return
        }

        val sendCommand = when {
            speedPercent in 0..9 -> "Speed1$speedPercent"
            speedPercent in 10..99 -> "Speed2$speedPercent"
            speedPercent == 100 -> "Speed3100"
            else -> "Speed$speedPercent"
        }

        val sendResult = serialPortManager?.sendText(sendCommand) == true
        if (sendResult) {
            textSpeedStatus?.text = "当前转速：${speedPercent}%"
            Toast.makeText(this, "已发送转速 ${speedPercent}% (命令: $sendCommand)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "转速发送失败，请检查串口连接", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAllButtons() {
        Log.d(TAG, "openAllButtons: Enabling all function buttons")
        buttonOneGear?.isEnabled = true
        buttonTwoGear?.isEnabled = true
        buttonThreeGear?.isEnabled = true
        buttonForward?.isEnabled = true
        buttonBackward?.isEnabled = true
        buttonStop?.isEnabled = true
        buttonSendSpeed?.isEnabled = true

        buttonOneGear?.alpha = 1.0f
        buttonTwoGear?.alpha = 1.0f
        buttonThreeGear?.alpha = 1.0f
        buttonForward?.alpha = 1.0f
        buttonBackward?.alpha = 1.0f
        buttonStop?.alpha = 1.0f
        buttonSendSpeed?.alpha = 1.0f

        buttonStart?.isEnabled = false
        buttonOver?.isEnabled = true
    }

    private fun closeAllButtons() {
        Log.d(TAG, "closeAllButtons: Disabling all function buttons")
        buttonOneGear?.isEnabled = false
        buttonTwoGear?.isEnabled = false
        buttonThreeGear?.isEnabled = false
        buttonForward?.isEnabled = false
        buttonBackward?.isEnabled = false
        buttonStop?.isEnabled = false
        buttonSendSpeed?.isEnabled = false

        buttonOneGear?.alpha = 0.5f
        buttonTwoGear?.alpha = 0.5f
        buttonThreeGear?.alpha = 0.5f
        buttonForward?.alpha = 0.5f
        buttonBackward?.alpha = 0.5f
        buttonStop?.alpha = 0.5f
        buttonSendSpeed?.alpha = 0.5f

        buttonStart?.isEnabled = true
        buttonOver?.isEnabled = false
    }

    private fun connectUART() {
        try {
            serialPortManager = SerialPortManager.getInstance()
            setSerialPortListener()
            val result = serialPortManager?.ensureConnected("/dev/ttyS6", 9600, 8, 0, 1)

            if (result == true) {
                Toast.makeText(this, "串口连接成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "串口打开失败，请检查权限", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "串口打开失败")
            }
        } catch (e: Exception) {
            Log.e(TAG, "连接串口异常", e)
            Toast.makeText(this, "连接异常: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setSerialPortListener() {
        val manager = serialPortManager
        if (manager != null) {
            val listener = object : OnDataReceivedListener {
                override fun onDataReceived(data: ByteArray?, size: Int) {
                    Log.d(TAG, "=== MainActivity 串口监听器被调用 ===")
                    Log.d(TAG, "数据大小: $size")
                    if (data != null) {
                        Log.d(TAG, "原始数据: ${data.joinToString(" ") { "%02X".format(it) }}")
                    }
                    handleReceivedData(data, size)
                }

                override fun onError(error: String?) {
                    Log.e(TAG, "MainActivity 串口错误: $error")
                    handler.post {
                        Toast.makeText(this@MainActivity, "串口错误: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            manager.setOnDataReceivedListener(listener)
            Log.d(TAG, "串口监听器已设置")
        } else {
            Log.e(TAG, "setSerialPortListener: serialPortManager is null")
        }
    }

    private fun handleReceivedData(data: ByteArray?, size: Int) {
        if (data == null || size <= 0) {
            return
        }
        try {
            val receivedText = String(data, 0, size).trim { it <= ' ' }
            Log.d(TAG, "收到串口数据: $receivedText (长度: $size)")
            handler.post {
                processReceivedCommand(receivedText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理接收数据异常: ${e.message}")
        }
    }

    private fun processReceivedCommand(command: String?) {
        Log.d(TAG, "处理命令: $command")
        if (command == null || command.trim().isEmpty()) {
            Log.w(TAG, "接收到空命令")
            return
        }
        val cleanCommand = command.trim()
        Log.d(TAG, "清洗后的命令: $cleanCommand")
    }
}
