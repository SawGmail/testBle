package com.example.testble

import com.clj.fastble.data.BleDevice

data class ItemClass(val name: String, val mac: String, val rssi: Int, val bleDevice: BleDevice) {
}