package com.example.deftcontroller.controller

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.mutableStateListOf

private const val TAG = "GamepadManager"

/**
 * Manages gamepad connections and delegates input events to the GameController
 */
class GamepadManager(
    private val context: Context,
    private val controller: GameController = DefaultGameController()
) {
    
    val connectedGamepads = mutableStateListOf<InputDevice>()
    
    
    val gameController: GameController = controller
    
    
    private var inputManager: InputManager? = null
    
    init {
        refreshConnectedGamepads()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        }
    }

    /**
     * Refresh the list of connected gamepads
     */
    fun refreshConnectedGamepads() {
        connectedGamepads.clear()
        val devices = InputDevice.getDeviceIds()
        for (id in devices) {
            val device = InputDevice.getDevice(id)
            
            
            if (device != null) {
                if (device.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                    device.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
                    connectedGamepads.add(device)
                }
            }
        }
    }

    /**
     * Process a key event from a gamepad
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        
        Log.d(TAG, "handleKeyEvent: keyCode=${event.keyCode}, action=${event.action}, " +
              "source=${event.source}, device=${event.device?.name ?: "Unknown"}")
        
        
        return controller.processKeyEvent(event)
    }

    /**
     * Process a motion event from a gamepad
     */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        
        Log.d(TAG, "handleMotionEvent: action=${event.action}, source=${event.source}, " +
              "device=${event.device?.name ?: "Unknown"}")
        
        
        logAxisValues(event)
        
        
        return controller.processMotionEvent(event)
    }

    /**
     * For debugging - log all non-zero axis values from a motion event
     */
    private fun logAxisValues(event: MotionEvent) {
        val axisList = listOf(
            Pair("X", MotionEvent.AXIS_X),
            Pair("Y", MotionEvent.AXIS_Y),
            Pair("Z", MotionEvent.AXIS_Z),
            Pair("RZ", MotionEvent.AXIS_RZ),
            Pair("RX", MotionEvent.AXIS_RX),
            Pair("RY", MotionEvent.AXIS_RY),
            Pair("HAT_X", MotionEvent.AXIS_HAT_X),
            Pair("HAT_Y", MotionEvent.AXIS_HAT_Y),
            Pair("LTRIGGER", MotionEvent.AXIS_LTRIGGER),
            Pair("RTRIGGER", MotionEvent.AXIS_RTRIGGER),
            Pair("GAS", MotionEvent.AXIS_GAS),
            Pair("BRAKE", MotionEvent.AXIS_BRAKE)
        )
        
        val axisValues = axisList.filter { 
            event.getAxisValue(it.second) != 0f 
        }.joinToString(", ") { 
            "${it.first}=${event.getAxisValue(it.second)}" 
        }
        
        if (axisValues.isNotEmpty()) {
            Log.d(TAG, "Active axes: $axisValues")
        }
    }

    /**
     * For debugging - get information about available input devices
     */
    fun logAllInputDevices() {
        val devices = InputDevice.getDeviceIds()
        Log.d(TAG, "==== AVAILABLE INPUT DEVICES ====")
        for (id in devices) {
            val device = InputDevice.getDevice(id)
            if (device != null) {
                Log.d(TAG, "Device ID: ${device.id}, Name: ${device.name}")
                Log.d(TAG, "  Sources: ${sourceToString(device.sources)}")
                Log.d(TAG, "  Vendor ID: ${device.vendorId}, Product ID: ${device.productId}")
                
                val controllerNumber = device.controllerNumber
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Log.d(TAG, "  Controller #: $controllerNumber, Descriptor: ${device.descriptor}")
                } else {
                    Log.d(TAG, "  Controller #: $controllerNumber")
                }
            }
        }
        Log.d(TAG, "================================")
    }
    
    private fun sourceToString(source: Int): String {
        val sources = mutableListOf<String>()
        if (source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) {
            sources.add("GAMEPAD")
        }
        if (source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
            sources.add("JOYSTICK")
        }
        if (source and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD) {
            sources.add("KEYBOARD")
        }
        if (source and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE) {
            sources.add("MOUSE")
        }
        if (source and InputDevice.SOURCE_TOUCHSCREEN == InputDevice.SOURCE_TOUCHSCREEN) {
            sources.add("TOUCHSCREEN")
        }
        
        return if (sources.isEmpty()) "UNKNOWN ($source)" else sources.joinToString(", ")
    }
}
