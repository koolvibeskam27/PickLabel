package com.kamal.picklabel

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ZebraPrinterStatusManager {

    suspend fun checkStatus(socket: BluetoothSocket): String =
        withContext(Dispatchers.IO) {

            val statusCommand = "! U1 getvar \"device.status\"\r\n"
            val out = socket.outputStream
            val input = socket.inputStream

            out.write(statusCommand.toByteArray())
            out.flush()

            val buffer = ByteArray(1024)
            val len = input.read(buffer)

            val response = String(buffer, 0, len)

            return@withContext when {
                "head_open" in response -> "Head open"
                "paper_out" in response -> "Out of media"
                "paused" in response -> "Printer paused"
                "ready" in response -> "Ready"
                else -> "Unknown status: $response"
            }
        }
}
