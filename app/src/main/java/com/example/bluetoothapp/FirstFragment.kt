package com.example.gpsbluetooth

import android.Manifest
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.bluetoothapp.databinding.ActivityMainBinding
//import com.example.gpsbluetooth.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var bluetoothSocket: BluetoothSocket? = null
    private val deviceName = "HC-05" // Change this to your Bluetooth module's name
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID for SPP

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Fetching location and sending via Bluetooth", Snackbar.LENGTH_LONG).show()
            requestLocation()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestPermissions()
        connectBluetooth()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT
        )

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                    requestLocation()
                }
            }
        requestPermissionLauncher.launch(permissions)
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("GPS", "Location permission not granted")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val data = "Latitude: ${it.latitude}, Longitude: ${it.longitude}"
                Log.d("GPS", data)
                sendDataToBluetooth(data)
            } ?: Log.e("GPS", "Failed to retrieve location")
        }
    }

    private fun connectBluetooth() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.e("Bluetooth", "Device does not support Bluetooth")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        val device = pairedDevices?.find { it.name == deviceName }

        if (device != null) {
            try {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                Log.d("Bluetooth", "Connected to $deviceName")
            } catch (e: IOException) {
                Log.e("Bluetooth", "Could not connect to $deviceName", e)
            }
        } else {
            Log.e("Bluetooth", "$deviceName not found in paired devices")
        }
    }

    private fun sendDataToBluetooth(data: String) {
        bluetoothSocket?.outputStream?.let { outputStream ->
            try {
                outputStream.write(data.toByteArray())
                Log.d("Bluetooth", "Data sent: $data")
            } catch (e: IOException) {
                Log.e("Bluetooth", "Error sending data", e)
            }
        } ?: Log.e("Bluetooth", "Bluetooth socket is null")
    }
}
