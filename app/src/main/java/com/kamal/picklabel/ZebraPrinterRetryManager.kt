package com.kamal.picklabel

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException


class ZebraPrinterRetryManager {

    suspend fun sendWithRetry(
        socket: BluetoothSocket,
        data: ByteArray,
        maxRetries: Int = 3
    ): Boolean = withContext(Dispatchers.IO) {

        var attempt = 0

        while (attempt < maxRetries) {
            try {
                socket.outputStream.write(data)
                socket.outputStream.flush()
                return@withContext true
            } catch (e: IOException) {
                attempt++
                delay(300L * attempt) // exponential backoff
            }
        }

        return@withContext false
    }
}
