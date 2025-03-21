package com.example.bluetoothapp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*

class FirstFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var bluetoothSocket: BluetoothSocket? = null
    private val deviceName = "DSD TECH HC-05" // Bluetooth module name
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID
    private var isSendingLocation = false
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
            coroutineScope.launch {
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
            coroutineScope.launch {
                while (isSendingLocation) {
                    Log.d("GPS", "Requesting location")
                    requestLocation()
                    delay(5000) // Repeat every 5 seconds
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestLocation() {
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 5000
        ).apply {
            setMinUpdateIntervalMillis(2000)
        }.build()

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                locationResult ?: return
                val location = locationResult.lastLocation
                if (location != null) {
                    val data = "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                    Log.d("GPS", data)
                    coroutineScope.launch {
                        sendDataToBluetooth(data)
                    }
                    fusedLocationClient.removeLocationUpdates(this) // Stop location updates after receiving the first result
                }
            }
        }

        withContext(Dispatchers.Main) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private suspend fun sendDataToBluetooth(data: String) {
        bluetoothSocket?.let { socket ->
            try {
                val outputStream = socket.outputStream
                withContext(Dispatchers.IO) {
                    outputStream.write(data.toByteArray())
                }
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
        coroutineScope.cancel()
    }

    private fun stopSendingLocation() {
        if (isSendingLocation) {
            isSendingLocation = false
        }
    }
}