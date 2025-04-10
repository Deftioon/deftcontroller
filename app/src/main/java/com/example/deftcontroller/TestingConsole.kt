package com.example.deftcontroller

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean 




private const val GAME_SIR_VENDOR_ID = 0x3537 
private const val GAME_SIR_PRODUCT_ID = 0x0108 


private const val ACTION_USB_PERMISSION = "com.example.deftcontroller.USB_PERMISSION"
private const val TAG = "HID_RAW_LOG"


private const val MAX_LOG_LINES = 100

@Composable
fun HomePageContentHidRawLog() {
    
    var rawReportLog by remember { mutableStateOf("Raw HID Reports:\n") }
    
    var statusLog by remember { mutableStateOf("Status Logs:\n") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope() 

    
    var device by remember { mutableStateOf<UsbDevice?>(null) }
    var connection by remember { mutableStateOf<UsbDeviceConnection?>(null) }
    var hidInterface by remember { mutableStateOf<UsbInterface?>(null) }
    var inputEndpoint by remember { mutableStateOf<UsbEndpoint?>(null) }
    var permissionRequested by remember { mutableStateOf(false) }
    val readingJob = remember { mutableStateOf<Job?>(null) } 

    val usbManager = remember { context.getSystemService(Context.USB_SERVICE) as UsbManager }

    
    val addStatusLog: (String) -> Unit = { log ->
        Log.d(TAG, "[Status] $log")
        
        scope.launch(Dispatchers.Main) {
            val lines = statusLog.split('\n')
            val limitedLog = lines.take(MAX_LOG_LINES).joinToString("\n")
            statusLog = "$log\n$limitedLog" 
        }
    }

    val addRawReportLog: suspend (String) -> Unit = { reportHex ->
        
        withContext(Dispatchers.Main) {
            val lines = rawReportLog.split('\n')
            val limitedLog = lines.take(MAX_LOG_LINES).joinToString("\n")
            rawReportLog = "$reportHex\n$limitedLog" 
        }
    }
    


    
    val permissionReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (ACTION_USB_PERMISSION == intent.action) {
                    synchronized(this) {
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        val deviceFromIntent: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }

                        if (deviceFromIntent != null && deviceFromIntent.vendorId == GAME_SIR_VENDOR_ID && deviceFromIntent.productId == GAME_SIR_PRODUCT_ID) {
                            device = deviceFromIntent 
                            if (granted) {
                                addStatusLog("USB permission granted for ${device?.deviceName}")
                                
                                connectToDevice(usbManager, device!!, addStatusLog) { conn, iface, ep ->
                                    connection = conn
                                    hidInterface = iface
                                    inputEndpoint = ep
                                    
                                    startReadingHidReports(conn, iface, ep, addStatusLog, addRawReportLog) {
                                        readingJob.value = it 
                                    }
                                }
                            } else {
                                addStatusLog("USB permission denied for ${device?.deviceName}")
                                device = null 
                            }
                        } else {
                            addStatusLog("Received permission result for non-matching or null device.")
                        }
                        permissionRequested = false 
                    }
                }
            }
        }
    }

    
    DisposableEffect(context) {
        val intentFilter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(permissionReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED) 
        } else {
            context.registerReceiver(permissionReceiver, intentFilter)
        }
        addStatusLog("Permission receiver registered.")

        onDispose {
            context.unregisterReceiver(permissionReceiver)
            addStatusLog("Permission receiver unregistered.")
            
            readingJob.value?.cancel() 
            connection?.close()
            hidInterface?.let { connection?.releaseInterface(it) } 
            connection = null
            hidInterface = null
            inputEndpoint = null
            device = null
            addStatusLog("Resources released on dispose.")
        }
    }
    


    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Direct HID Raw Report Logger", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    if (connection == null) { 
                        addStatusLog("Attempting to find and connect GameSir...")
                        
                        val foundDevice = findGameSirHid(usbManager, addStatusLog)
                        if (foundDevice != null) {
                            device = foundDevice 
                            requestUsbPermission(context, usbManager, foundDevice, addStatusLog)
                            permissionRequested = true
                        } else {
                            addStatusLog("GameSir device not found. Check VID/PID and connection.")
                        }
                    } else {
                        addStatusLog("Already connected or connecting.")
                    }
                },
                enabled = connection == null && !permissionRequested 
            ) {
                Text("Connect & Request Perm")
            }

            Button(
                onClick = {
                    addStatusLog("Disconnecting...")
                    readingJob.value?.cancel() 
                    readingJob.value = null
                    
                    scope.launch(Dispatchers.IO) { 
                        try {
                            hidInterface?.let { iface ->
                                connection?.releaseInterface(iface)
                                addStatusLog("Interface released.")
                            }
                            connection?.close()
                            addStatusLog("Connection closed.")
                        } catch (e: Exception) {
                            addStatusLog("Error during disconnect: ${e.message}")
                        } finally {
                            withContext(Dispatchers.Main) { 
                                connection = null
                                hidInterface = null
                                inputEndpoint = null
                                device = null 
                                permissionRequested = false
                                addStatusLog("Disconnected.")
                                
                                
                                
                            }
                        }
                    }
                },
                enabled = connection != null 
            ) {
                Text("Disconnect")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        val statusText = when {
            connection != null && inputEndpoint != null -> "Status: Connected to ${device?.deviceName ?: "Unknown"}"
            permissionRequested -> "Status: Requesting Permission..."
            device != null && connection == null -> "Status: Device Found, Ready to Connect/Request Perm" 
            else -> "Status: Disconnected / Device Not Found"
        }
        Text(statusText)
        Spacer(modifier = Modifier.height(16.dp))

        
        Row(Modifier.fillMaxSize()) {
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                    .fillMaxHeight()
            ) {
                Text("Raw Reports (Hex):", fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier
                    .fillMaxSize() 
                    .verticalScroll(rememberScrollState()) 
                ) {
                    Text(rawReportLog, fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 14.sp)
                }
            }

            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .fillMaxHeight()
            ) {
                Text("Status Log:", fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier
                    .fillMaxSize() 
                    .verticalScroll(rememberScrollState()) 
                ) {
                    Text(statusLog, fontSize = 12.sp, lineHeight = 14.sp)
                }
            }
        }
    }
}



fun findGameSirHid(usbManager: UsbManager, addStatusLog: (String) -> Unit): UsbDevice? {
    usbManager.deviceList.values.forEach { device ->
        
        if (device.vendorId == GAME_SIR_VENDOR_ID && device.productId == GAME_SIR_PRODUCT_ID) {
            addStatusLog("Found matching device by VID/PID: ${device.deviceName}")
            
            for (i in 0 until device.interfaceCount) {
                if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_HID) {
                    addStatusLog("Device has HID interface. Selecting.")
                    return device
                }
            }
            addStatusLog("WARN: Device matched VID/PID but no standard HID interface found (might use vendor-specific class).")
            
            return device
        }
    }
    addStatusLog("No matching GameSir device found.")
    return null
}

fun requestUsbPermission(context: Context, usbManager: UsbManager, device: UsbDevice, addStatusLog: (String) -> Unit) {
    if (usbManager.hasPermission(device)) {
        addStatusLog("Permission already granted for ${device.deviceName}. Use 'Connect' button if needed.")
        
        
        
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION).putExtra(UsbManager.EXTRA_DEVICE, device), 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, permissionIntent) 
        addStatusLog("Triggering permission flow even though already granted.")

    } else {
        addStatusLog("Requesting USB permission for ${device.deviceName}...")
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION).putExtra(UsbManager.EXTRA_DEVICE, device),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE 
        )
        usbManager.requestPermission(device, permissionIntent)
    }
}


fun connectToDevice(
    usbManager: UsbManager,
    device: UsbDevice,
    addStatusLog: (String) -> Unit,
    
    onConnected: (connection: UsbDeviceConnection, iface: UsbInterface, endpoint: UsbEndpoint) -> Unit
) {
    var connection: UsbDeviceConnection? = null
    var hidInterface: UsbInterface? = null
    var inputEndpoint: UsbEndpoint? = null
    var claimed = false 

    try {
        addStatusLog("Attempting to open device ${device.deviceName}...")
        connection = usbManager.openDevice(device)
        if (connection == null) {
            addStatusLog("Failed to open device connection (null).")
            return
        }
        addStatusLog("Device opened.")

        
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_HID) {
                addStatusLog("Found HID interface: ID ${iface.id}")
                hidInterface = iface
                break
            }
        }

        if (hidInterface == null) {
            
            
            addStatusLog("No standard HID interface found. Searching for potential vendor-specific...")
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC) {
                    
                    addStatusLog("Found Vendor-Specific interface: ID ${iface.id}. Attempting to use.")
                    hidInterface = iface
                    break;
                }
            }
            if (hidInterface == null) {
                addStatusLog("No suitable HID or Vendor-Specific interface found.")
                connection.close()
                return
            }
        }

        
        addStatusLog("Claiming interface ${hidInterface.id}...")
        
        claimed = connection.claimInterface(hidInterface, true) 
        if (!claimed) {
            addStatusLog("Failed to claim interface ${hidInterface.id}. Is another app using it (e.g., GameSir app)?")
            connection.close()
            return
        }
        addStatusLog("Interface ${hidInterface.id} claimed successfully.")

        
        for (i in 0 until hidInterface.endpointCount) {
            val endpoint = hidInterface.getEndpoint(i)
            
            if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT &&
                endpoint.direction == UsbConstants.USB_DIR_IN) {
                addStatusLog("Found Input Endpoint: Addr=${endpoint.address}, MaxPacketSize=${endpoint.maxPacketSize}")
                inputEndpoint = endpoint
                break
            }
        }

        if (inputEndpoint == null) {
            addStatusLog("No suitable input endpoint (Interrupt IN) found on interface ${hidInterface.id}.")
            if (claimed) connection.releaseInterface(hidInterface)
            connection.close()
            return
        }

        
        addStatusLog("Successfully connected and found endpoint. Ready to read.")
        onConnected(connection, hidInterface, inputEndpoint)

    } catch (e: Exception) {
        addStatusLog("Error during connection setup: ${e.message}")
        
        if (claimed && hidInterface != null) {
            try { connection?.releaseInterface(hidInterface) } catch (re: Exception) { }
        }
        try { connection?.close() } catch (ce: Exception) { }
    }
}


fun startReadingHidReports(
    connection: UsbDeviceConnection,
    usbInterface: UsbInterface, 
    endpoint: UsbEndpoint,
    addStatusLog: (String) -> Unit,
    
    onRawReport: suspend (String) -> Unit,
    
    setReadingJob: (Job?) -> Unit
) {
    addStatusLog("Starting HID report reading coroutine...")
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()) 
    val currentJob = scope.launch {
        val buffer = ByteArray(endpoint.maxPacketSize)
        var consecutiveErrors = 0
        val maxConsecutiveErrors = 10 

        try {
            while (isActive) { 
                
                
                val bytesRead = connection.bulkTransfer(endpoint, buffer, buffer.size, 500) 

                if (bytesRead > 0) {
                    consecutiveErrors = 0 
                    val report = buffer.copyOf(bytesRead)
                    val reportHex = report.joinToString(" ") { "%02X".format(it) } 
                    val logLine = "[${bytesRead}b] $reportHex"

                    
                    onRawReport(logLine)

                } else if (bytesRead == 0) {
                    
                    
                    
                    consecutiveErrors = 0 
                } else {
                    
                    addStatusLog("Error reading from HID endpoint: $bytesRead")
                    consecutiveErrors++
                    if (consecutiveErrors >= maxConsecutiveErrors) {
                        addStatusLog("Too many consecutive read errors ($consecutiveErrors). Stopping reader.")
                        break 
                    }
                    delay(100) 
                }
                
                
            }
        } catch (e: CancellationException) {
            addStatusLog("HID reading coroutine cancelled.")
            
        } catch (e: Exception) {
            
            addStatusLog("Exception in HID reading loop: ${e.javaClass.simpleName} - ${e.message}")
        } finally {
            addStatusLog("HID reading loop finished.")
            
            
            
            withContext(Dispatchers.Main) { 
                setReadingJob(null)
            }
        }
    }
    
    setReadingJob(currentJob)
}