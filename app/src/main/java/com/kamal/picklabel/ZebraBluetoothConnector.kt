package com.kamal.picklabel

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID


class ZebraBluetoothConnector {

    private val sppUUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    suspend fun connect(device: BluetoothDevice): BluetoothSocket? =
        withContext(Dispatchers.IO) {

            BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()

            // Try insecure RFCOMM first (Zebra preferred)
            val insecure = try {
                device.createInsecureRfcommSocketToServiceRecord(sppUUID)
            } catch (_: Exception) { null }

            insecure?.let {
                try {
                    it.connect()
                    delay(120) // Zebra wake-up delay
                    return@withContext it
                } catch (_: Exception) {}
            }

            // Fallback: secure RFCOMM
            val secure = try {
                device.createRfcommSocketToServiceRecord(sppUUID)
            } catch (_: Exception) { null }

            secure?.let {
                try {
                    it.connect()
                    delay(120)
                    return@withContext it
                } catch (_: Exception) {}
            }

            // Final fallback: channel 1 reflection (QLn older models)
            val fallback = try {
                val clazz = device.javaClass
                val method = clazz.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                method.invoke(device, 1) as BluetoothSocket
            } catch (_: Exception) { null }

            fallback?.let {
                try {
                    it.connect()
                    delay(120)
                    return@withContext it
                } catch (_: Exception) {}
            }

            return@withContext null
        }
}
