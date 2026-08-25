package com.kamal.picklabel

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothPrinterManager(private val context: Context) {

    private val sppUUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")   // Classic SPP UUID

    // -------------------------------------------------------------------------
    // PERMISSIONS (Android 12 uses CONNECT/SCAN, Android 8/9 uses LOCATION)
    // -------------------------------------------------------------------------
    fun ensurePermissions(onGranted: () -> Unit) {
        val needLegacyLocation =
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED

        val needBtConnect =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED

        val needBtScan =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED

        // Permissions handled in MainActivity
        onGranted()
    }

    // -------------------------------------------------------------------------
    // PRINT FUNCTION (CN80‑safe, Razr+‑safe)
    // -------------------------------------------------------------------------
    suspend fun print(
        mac: String,
        zpl: String,
        callback: (Boolean, String) -> Unit
    ) {
        withContext(Dispatchers.IO) {

            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                callback(false, "Bluetooth not supported")
                return@withContext
            }

            if (!adapter.isEnabled) {
                adapter.enable()
                Thread.sleep(800)
            }

            // 8/9 requires cancelDiscovery() or SPP fails
            try { adapter.cancelDiscovery() } catch (_: Exception) {}

            val device: BluetoothDevice = try {
                adapter.getRemoteDevice(mac)
            } catch (e: Exception) {
                callback(false, "Invalid MAC address")
                return@withContext
            }

            // Try normal SPP first
            var socket: BluetoothSocket? = try {
                device.createRfcommSocketToServiceRecord(sppUUID)
            } catch (e: Exception) {
                null
            }

            // 8/9 fallback: channel 1 hack
            if (socket == null) {
                try {
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    socket = method.invoke(device, 1) as BluetoothSocket
                } catch (e: Exception) {
                    callback(false, "Unable to open SPP socket")
                    return@withContext
                }
            }

            // Connect
            try {
                socket!!.connect()
            } catch (e: IOException) {
                callback(false, "Connection failed: ${e.message}")
                try { socket!!.close() } catch (_: Exception) {}
                return@withContext
            }

            // -----------------------------------------------------------------
            // SEND ZPL (8/9‑SAFE CLOSE SEQUENCE)
            // -----------------------------------------------------------------
            try {
                val output = socket!!.outputStream
                output.write(zpl.toByteArray())

                // 8/9 needs a delay before closing
                Thread.sleep(500)

                // 8/9-safe flush
                try { output.flush() } catch (_: Exception) {}

                // 8/9-safe output close
                try { output.close() } catch (_: Exception) {}

                // 8/9-safe socket close
                try { socket!!.close() } catch (_: Exception) {}

                callback(true, "Printed Successfully")

            } catch (e: Exception) {
                callback(false, "Print failed: ${e.message}")
                try { socket!!.close() } catch (_: Exception) {}
            }
        }
    }
}
