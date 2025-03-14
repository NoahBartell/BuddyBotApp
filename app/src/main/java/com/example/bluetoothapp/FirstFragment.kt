package com.example.bluetoothapp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.*
import kotlin.concurrent.thread

class FirstFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var bluetoothSocket: BluetoothSocket? = null
    private val deviceName = "DSD TECH HC-05" // Bluetooth module name
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID
    private val handler = Handler(Looper.getMainLooper())
    private var isSendingLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("APP", "Created")
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        // Request Bluetooth permissions
        checkBluetoothPermissions()
    }

    fun checkBluetoothPermissions() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter?.let {
            if (!it.isEnabled) {
                Log.e("Bluetooth", "Bluetooth is disabled")
            } else {
                connectBluetooth()
            }
        } ?: Log.e("Bluetooth", "Device does not support Bluetooth")
    }

    @SuppressLint("MissingPermission")
    private fun connectBluetooth() {
        Log.d("Bluetooth", "Connecting to $deviceName")
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val device = pairedDevices?.find { it.name == deviceName }

        device?.let {
            thread {
                try {
                    bluetoothSocket = it.createRfcommSocketToServiceRecord(uuid)
                    bluetoothSocket?.connect()
                    Log.d("Bluetooth", "Connected to $deviceName")
                    startSendingLocation()
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Could not connect to $deviceName", e)
                }
            }
        } ?: Log.e("Bluetooth", "$deviceName not found in paired devices")
    }

    private fun startSendingLocation() {
        if (!isSendingLocation) {
            isSendingLocation = true
            val runnable = object : Runnable {
                override fun run() {
                    requestLocation()
                    handler.postDelayed(this, 5000) // Repeat every 5 seconds
                }
            }
            handler.post(runnable)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val data = "Latitude: ${it.latitude}, Longitude: ${it.longitude}"
                Log.d("GPS", data)
                sendDataToBluetooth(data)
            } ?: Log.e("GPS", "Failed to retrieve location")
        }
    }

    private fun sendDataToBluetooth(data: String) {
        bluetoothSocket?.let { socket ->
            try {
                val outputStream = socket.outputStream
                outputStream.write(data.toByteArray())
                Log.d("Bluetooth", "Data sent: $data")
            } catch (e: IOException) {
                Log.e("Bluetooth", "Error sending data", e)
            }
        } ?: Log.e("Bluetooth", "Bluetooth socket is null or not connected")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopSendingLocation()
        bluetoothSocket?.close()
    }

    private fun stopSendingLocation() {
        if (isSendingLocation) {
            isSendingLocation = false
            handler.removeCallbacksAndMessages(null) // Stop location updates
        }
    }
}
