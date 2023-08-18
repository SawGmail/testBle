package com.example.testble

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleMtuChangedCallback
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import org.w3c.dom.Text


class BleDeviceActivity : AppCompatActivity() {
    private lateinit var btnSend: Button
    private lateinit var etSendData: EditText
    private lateinit var tvReceiveData: TextView
    lateinit var bleDeviceMac: String
    lateinit var bleDeviceName: String
    lateinit var bleDevice: BleDevice

    private val UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dc4179"
    private val UUID_CHAR_WRITE = "6e400002-b5a3-f393-e0a9-e50e24dc4179"
    private val UUID_CHAR_NOTIFY = "6e400003-b5a3-f393-e0a9-e50e24dc4179"

    val sendData = "68AAAAAAAAAAAA6811584A21212134343337EE35343337EE33333333EE33343333EE33353333EE33363333EE33373333EE33333433EE33343433EE33353433EE33363433EE33373433EE33333533EE33343533EE33353533EE33363533EE333735338816"

    private val mtu = 256

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_device)

        btnSend = findViewById<Button>(R.id.btnSend)
        etSendData = findViewById(R.id.etSendData)
        tvReceiveData = findViewById(R.id.tvData)

        btnSend.setOnClickListener {
            btnClick()
        }

        val ret = getConnectedDevice()
        if (ret) {
            registerNotify()
            requestMtu()
            setDefaultDataInEditText()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BleManager.getInstance().disconnect(bleDevice)
    }


    private fun setDefaultDataInEditText()
    {
        etSendData.setText(sendData)
    }

    private fun getConnectedDevice(): Boolean {
        bleDeviceMac = intent.getStringExtra("bleDeviceMac") ?: ""
        bleDeviceName = intent.getStringExtra("bleDeviceName") ?: ""
        BleManager.getInstance().allConnectedDevice.forEach {
            if (it.mac == bleDeviceMac && it.name == bleDeviceName) {
                bleDevice = it
                return true
            }
        }
        return false
    }


    private fun notifyCallback() = object : BleNotifyCallback() {
        override fun onNotifySuccess() {
            Toast.makeText(this@BleDeviceActivity, "onNotifySuccess", Toast.LENGTH_SHORT).show()
        }

        override fun onNotifyFailure(exception: BleException?) {
            Toast.makeText(this@BleDeviceActivity, "onNotifyFailure", Toast.LENGTH_SHORT).show()
        }

        override fun onCharacteristicChanged(data: ByteArray?) {
            var dataString: String = data?.joinToString("") { "%02X".format(it) } ?: ""
            Log.d("BleDeviceActivity", "onCharacteristicChanged, data: ${dataString}")
            var oldText = tvReceiveData.text
            oldText = oldText.toString() + "" + dataString
            tvReceiveData.text = oldText
        }
    }

    private fun requestMtu() {

        BleManager.getInstance().setMtu(bleDevice, mtu, object : BleMtuChangedCallback() {
            override fun onSetMTUFailure(exception: BleException) {}
            override fun onMtuChanged(mtu: Int) {
                Toast.makeText(this@BleDeviceActivity, "onMtuChanged, mtu: $mtu", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun registerNotify() {
        BleManager.getInstance().notify(
            bleDevice,
            UUID_SERVICE,
            UUID_CHAR_NOTIFY,
            false,
            notifyCallback(),
        )
    }

    private fun btnClick() {
        val data = etSendData.text.toString()
        val byteArray: ByteArray = data.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        Log.d("BleDeviceActivity", "send data, byte array: ${byteArray.toString()}")
        BleManager.getInstance().write(
            bleDevice,
            UUID_SERVICE,
            UUID_CHAR_WRITE,
            byteArray,
            object : com.clj.fastble.callback.BleWriteCallback() {
                override fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray?) {
                    Log.d("BleDeviceActivity", "onWriteSuccess, current: $current, total: $total, justWrite: $justWrite")
                }

                override fun onWriteFailure(exception: BleException?) {
                    Toast.makeText(this@BleDeviceActivity, "onWriteFailure", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )
    }
}