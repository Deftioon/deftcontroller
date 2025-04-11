package com.example.deftcontroller

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.Button
import kotlinx.coroutines.delay

@Composable
fun SettingsPageContent(bluetoothAdapter: BluetoothAdapter?) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings")
        MainComponent(bluetoothAdapter)
    }
}

@Composable
fun MainComponent(bluetoothAdapter: BluetoothAdapter?) {
    val context = LocalContext.current
    var discoveredDevices by remember {
        mutableStateOf(emptyList<BluetoothDevice>())
    }
    var scanning by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        device?.let {
                            if (!discoveredDevices.contains(it)) {
                                discoveredDevices = discoveredDevices + it
                            }
                        }
                    }
                }
            }
        }
    }
    DisposableEffect(context) {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(scanning) {
        if (scanning) {
            if (bluetoothAdapter != null && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                try {
                    bluetoothAdapter.startDiscovery()
                    delay(30000)
                } catch (e: SecurityException) {
                    errorMessage = "Bluetooth scan permission required"
                } finally {
                    bluetoothAdapter.cancelDiscovery()
                    scanning = false
                }
            } else if (bluetoothAdapter == null) {
                errorMessage = "Bluetooth is not available on this device"
                scanning = false
            } else {
                errorMessage = "Bluetooth scan permission not granted"
                scanning = false
            }
        }
    }

    val (knownDevices, unknownDevices) = discoveredDevices.partition {
        !it.name.isNullOrEmpty() && it.name != "Unknown Device"
    }

    val sortedDevices = knownDevices + unknownDevices

    Column {
        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = androidx.compose.ui.graphics.Color.Red
            )
        }
        Button(onClick = {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                scanning = !scanning
            } else {
                ActivityCompat.requestPermissions(
                    context as ComponentActivity,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                    1
                )
            }
        }) {
            Text(
                if (scanning) "Stop Scanning" else "Start Scanning"
            )
        }
        DeviceList(devices = sortedDevices, onDeviceClick = { selectedDevice ->
            println("Selected device: $selectedDevice.name")
        })
    }
}