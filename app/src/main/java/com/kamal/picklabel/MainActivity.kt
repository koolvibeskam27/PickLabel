package com.kamal.picklabel

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.ImageView

class MainActivity : AppCompatActivity() {

    private lateinit var txtPrinter: TextView
    private lateinit var printerManager: BluetoothPrinterManager
    private lateinit var imgPreview: ImageView

    private fun showPrintCompleteDialog() {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Print Complete")
            .setMessage("Your label has been printed successfully.")
            .setPositiveButton("OK") { d, _ ->
                d.dismiss()
            }
            .create()

        dialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        printerManager = BluetoothPrinterManager(this)

        txtPrinter = findViewById(R.id.txtPrinter)
        imgPreview = findViewById(R.id.imgPreview)

        val etPrinter = findViewById<EditText>(R.id.etPrinter)
        val etName = findViewById<EditText>(R.id.etName)
        val etWO = findViewById<EditText>(R.id.etWO)
        val etItem = findViewById<EditText>(R.id.etItem)
        val etQty = findViewById<EditText>(R.id.etQty)
        val etLot = findViewById<EditText>(R.id.etLot)
        val etLoc = findViewById<EditText>(R.id.etLoc)

        loadPrinter()

        // ⭐ CN80 needs this popup for Bluetooth scanning
        requestLegacyBluetoothPermissions()

        // Save printer MAC manually
        findViewById<Button>(R.id.btnSetPrinter).setOnClickListener {
            val mac = etPrinter.text.toString().trim()

            if (mac.isBlank()) {
                Toast.makeText(this, "Enter Printer MAC", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            savePrinter(mac)
            etPrinter.text.clear()
        }

        // Save printer MAC via barcode scan (IME action)
        etPrinter.setOnEditorActionListener { _, _, _ ->
            val mac = etPrinter.text.toString().trim()

            if (mac.isNotBlank()) {
                savePrinter(mac)
                etPrinter.text.clear()
                etWO.requestFocus()
            }

            true
        }
        //CLEAR BUTTON
        findViewById<Button>(R.id.btnClear).setOnClickListener {

            etWO.text.clear()
            etItem.text.clear()
            etQty.text.clear()
            etLot.text.clear()
            etLoc.text.clear()

            // Keep Name and Printer MAC exactly as they are

            Toast.makeText(
                this,
                "Fields Cleared",
                Toast.LENGTH_SHORT
            ).show()

            etWO.requestFocus()
        }


        // PRINT BUTTON
        findViewById<Button>(R.id.btnPrint).setOnClickListener {

            val name = etName.text.toString().trim()
            val wo = etWO.text.toString().trim()
            val item = etItem.text.toString().trim().uppercase()
            val qty = etQty.text.toString().trim()
            val lot = etLot.text.toString().trim().uppercase()
            val loc = etLoc.text.toString().trim().uppercase()

            // Build ZPL + Preview
            val zpl = buildZPL(name, wo, item, qty, lot, loc)
            val previewRenderer = ZebraZplPreviewRenderer()
            val previewBitmap = previewRenderer.renderPreview(zpl)
            imgPreview.setImageBitmap(previewBitmap)

            // Validation
            if (name.isBlank()) {
                Toast.makeText(this, "Enter Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!wo.matches(Regex("\\d{6}"))) {
                Toast.makeText(this, "Work Order must be exactly 6 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (item.isBlank()) {
                Toast.makeText(this, "Enter Item Number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val qtyInt = qty.toIntOrNull()
            if (qtyInt == null || qtyInt <= 0) {
                Toast.makeText(this, "Enter a valid quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("PREFS", MODE_PRIVATE)
            val printerMac = prefs.getString("PRINTER_MAC", null)

            if (printerMac == null) {
                Toast.makeText(this, "Scan Printer Barcode First", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Request Bluetooth permissions first then PRINT
            printerManager.ensurePermissions {
                lifecycleScope.launch {
                    printerManager.print(printerMac, zpl) { success, message ->
                        this@MainActivity.runOnUiThread {
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()

                            if (success && !isFinishing && !isDestroyed) {
                                showPrintCompleteDialog()
                            }
                        }
                    }
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadPrinter()
    }

    private fun savePrinter(mac: String) {
        val prefs = getSharedPreferences("PREFS", MODE_PRIVATE)
        prefs.edit().putString("PRINTER_MAC", mac).apply()

        txtPrinter.text = "Current Printer: $mac"
        Toast.makeText(this, "Printer Saved", Toast.LENGTH_SHORT).show()
    }

    private fun loadPrinter() {
        val prefs = getSharedPreferences("PREFS", MODE_PRIVATE)
        txtPrinter.text = "Current Printer: " +
                prefs.getString("PRINTER_MAC", "None Selected")
    }

    // CN80 LOCATION permissions for Bluetooth scanning
    private fun requestLegacyBluetoothPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )

        ActivityCompat.requestPermissions(this, permissions, 2001)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 2001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth permissions required for CN80", Toast.LENGTH_LONG).show()
            }
        }
    }
}
