package com.example.bluetoothapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.bluetoothapp.databinding.FragmentFirstBinding
import com.google.android.gms.location.*

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize location request
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build() // Update every 5 seconds

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location: Location? = locationResult.lastLocation
                location?.let {
                    val latitude = it.latitude
                    val longitude = it.longitude
                    binding.textviewFirst.text = "Latitude: $latitude, Longitude: $longitude"
                } ?: run {
                    binding.textviewFirst.text = "Failed to get location"
                }
            }
        }

        // Request location updates
        requestLocationUpdates()

        // Update Bluetooth device name
        binding.textView3.text = getConnectedDeviceName()
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun getConnectedDeviceName(): String {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return "Bluetooth Permission Not Granted"
        }
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                val connectionState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET)
                if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                    return device.name
                }
            }
        }
        return "No Connected Bluetooth Device"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fusedLocationClient.removeLocationUpdates(locationCallback) // Stop updates to prevent memory leaks
        _binding = null
    }
}
