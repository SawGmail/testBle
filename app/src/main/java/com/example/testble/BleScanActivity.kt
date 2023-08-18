package com.example.testble

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.clj.fastble.scan.BleScanRuleConfig
import java.util.*


class BleScanActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    private val itemList = mutableListOf<ItemClass>()

    private val targetBleName = "111"
    private val REQUEST_PERMISSIONS_CODE = 1
    private val targetBleAdvUUID: String = "6e400001-b5a3-f393-e0a9-e50e24dc4179"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_scan)

        recyclerView = findViewById<RecyclerView>(R.id.rvList)
        recyclerView.layoutManager = LinearLayoutManager(this)



        itemAdapter = ItemAdapter(itemList, object : ItemAdapter.ItemClickListener {
            override fun onItemClick(position: Int) {
                Log.d("BleScanActivity", "onItemClick: $position")
                connectTargetBleDevice(itemList[position].bleDevice)
            }
        })
        recyclerView.adapter = itemAdapter


        // Check if permissions are granted
        if (!arePermissionsGranted()) {
            // Request permissions
            requestPermissions()
        } else {
            // Permissions already granted
            // Start your BLE scanning and connecting logic here
            initBle()
            startScan()
        }
    }


    private fun connectTargetBleDevice(bleDevice: BleDevice) {
        BleManager.getInstance().cancelScan()
        BleManager.getInstance().connect(bleDevice, object : BleGattCallback() {
            override fun onStartConnect() {
                Log.d("BleScanActivity", "onStartConnect: $bleDevice")
                Toast.makeText(this@BleScanActivity, "开始连接", Toast.LENGTH_SHORT).show()
            }

            override fun onConnectFail(bleDevice: BleDevice?, exception: BleException?) {
                Toast.makeText(this@BleScanActivity, "连接失败，请重试", Toast.LENGTH_SHORT).show()
                Log.d("BleScanActivity", "onConnectFail: $bleDevice")
            }

            override fun onConnectSuccess(bleDevice: BleDevice?, gatt: BluetoothGatt?, status: Int) {
                Log.d("BleScanActivity", "onConnectSuccess: ${bleDevice?.name}")
                Log.d("BleScanActivity", "onConnectSuccess: ${bleDevice?.mac}")
                if (bleDevice != null) {
                    Toast.makeText(this@BleScanActivity, "连接成功", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@BleScanActivity, BleDeviceActivity::class.java).apply {
                        putExtra("bleDeviceMac", bleDevice.mac)
                        putExtra("bleDeviceName", bleDevice.name)
                    })
                }
            }

            override fun onDisConnected(isActiveDisConnected: Boolean, device: BleDevice?, gatt: BluetoothGatt?, status: Int) {
                Log.d("BleScanActivity", "onDisConnected: $bleDevice")
            }
        })
    }


    private fun initBle() {
        itemList.clear()
        BleManager.getInstance().init(application)
        BleManager.getInstance()
            .enableLog(true)
            .setReConnectCount(1, 5000)
            .setSplitWriteNum(20)
            .setConnectOverTime(10000)
            .setOperateTimeout(5000);
    }

    private fun startScan() {
        val scanRuleConfig = BleScanRuleConfig.Builder()
//            .setDeviceName(true, targetBleName)
            .setServiceUuids(arrayOf(UUID.fromString(targetBleAdvUUID)))
            .setAutoConnect(false)
            .setScanTimeOut(10000)
            .build()
        BleManager.getInstance().initScanRule(scanRuleConfig)

        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
            }
            override fun onScanning(bleDevice: BleDevice) {
                val name = bleDevice.name ?: "No name"
                itemList.add(ItemClass(name, bleDevice.mac, bleDevice.rssi, bleDevice))
                itemAdapter.notifyItemInserted(itemList.size - 1)
                bleDevice.scanRecord
                Log.d("BleScanActivity", "onScanning: $bleDevice")
                Log.d("BleScanActivity", "onScanning scanRecord: ${String(bleDevice.scanRecord)}")
            }
            override fun onScanFinished(scanResultList: List<BleDevice>) {
                Log.d("BleScanActivity", "onScanFinished: $scanResultList")
            }
        })
    }


    private fun arePermissionsGranted(): Boolean {
        val bluetoothPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH
        )
        val bluetoothAdminPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        val locationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        return bluetoothPermission == PackageManager.PERMISSION_GRANTED &&
                bluetoothAdminPermission == PackageManager.PERMISSION_GRANTED &&
                locationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_PERMISSIONS_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            // Check if all permissions are granted
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted
                // Start your BLE scanning and connecting logic here
            } else {
                // Permissions not granted
                // Handle the scenario where the user denied the permissions
            }
        }
    }
}