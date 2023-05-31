package com.techno.monocle.ui.chat

import com.techno.monocle.R

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_ENABLE_BLUETOOTH = 1
        private const val REQUEST_BLUETOOTH_PERMISSION = 2
        private val MY_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Example UUID
        private const val TARGET_DEVICE_ADDRESS =
            "00:00:00:00:00:00" // Replace with actual MAC address
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        // Check Bluetooth permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                ),
                REQUEST_BLUETOOTH_PERMISSION
            )
        } else {
            setupBluetooth()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                setupBluetooth()
            } else {
                Toast.makeText(this, "Bluetooth is required for this app.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        closeBluetoothSocket()
    }

    private fun setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Bluetooth is not supported on the device
            Toast.makeText(this, "Bluetooth is not supported on this device.", Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
            return
        }

        val targetDevice: BluetoothDevice? =
            bluetoothAdapter!!.getRemoteDevice(TARGET_DEVICE_ADDRESS)
        if (targetDevice == null) {
            // Device not found
            Toast.makeText(this, "Target device not found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            bluetoothSocket = targetDevice.createRfcommSocketToServiceRecord(MY_UUID)
            bluetoothSocket?.connect()

            // Sending and receiving data
            sendData("Hello from Android!")
            val receivedData = receiveData()
            Toast.makeText(this, "Received data: $receivedData", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to connect: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun sendData(message: String) {
        val outputStream: OutputStream? = bluetoothSocket?.outputStream
        outputStream?.write(message.toByteArray())
    }

    private fun receiveData(): String? {
        val inputStream: InputStream? = bluetoothSocket
            ?.inputStream
        val buffer = ByteArray(1024)
        val bytesRead: Int = inputStream?.read(buffer) ?: -1
        if (bytesRead != -1) {
            return String(buffer, 0, bytesRead)
        }
        return null
    }

    private fun closeBluetoothSocket() {
        try {
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                setupBluetooth()
            } else {
                Toast.makeText(
                    this,
                    "Bluetooth permission is required for this app.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}