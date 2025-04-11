package com.example.deftcontroller

import android.content.Context
import android.hardware.input.InputManager
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

private const val TAG = "GamepadManager"

/**
 * Manages gamepad connections and input events
 */
class GamepadManager(context: Context) {
    
    companion object {
        val BUTTON_NAMES = mapOf(
            KeyEvent.KEYCODE_BUTTON_A to "A",
            KeyEvent.KEYCODE_BUTTON_B to "B",
            KeyEvent.KEYCODE_BUTTON_X to "X", 
            KeyEvent.KEYCODE_BUTTON_Y to "Y",
            KeyEvent.KEYCODE_BUTTON_L1 to "L1",
            KeyEvent.KEYCODE_BUTTON_R1 to "R1",
            KeyEvent.KEYCODE_BUTTON_THUMBL to "L3",
            KeyEvent.KEYCODE_BUTTON_THUMBR to "R3",
            KeyEvent.KEYCODE_BUTTON_START to "Start",
            KeyEvent.KEYCODE_BUTTON_SELECT to "Select",
            KeyEvent.KEYCODE_DPAD_UP to "D-Up",
            KeyEvent.KEYCODE_DPAD_DOWN to "D-Down",
            KeyEvent.KEYCODE_DPAD_LEFT to "D-Left",
            KeyEvent.KEYCODE_DPAD_RIGHT to "D-Right"
        )
    }

    
    private val connectedGamepads = mutableStateListOf<InputDevice>()
    private val lastButtonPress = mutableStateOf<String?>(null)
    private val leftStick = mutableStateOf(Pair(0f, 0f))
    private val rightStick = mutableStateOf(Pair(0f, 0f))
    private val leftTrigger = mutableFloatStateOf(0f)
    private val rightTrigger = mutableFloatStateOf(0f)

    
    private var inputManager: InputManager? = null
    
    init {
        refreshConnectedGamepads()

        inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
    }

    /**
     * Refresh the list of connected gamepads
     */
    private fun refreshConnectedGamepads() {
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
        
        
        
        
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                val buttonName = BUTTON_NAMES[event.keyCode] ?: "Key ${event.keyCode}"
                lastButtonPress.value = buttonName
                Log.d(TAG, "Button pressed: $buttonName")
                return true
            }
            KeyEvent.ACTION_UP -> {
                
                val buttonName = BUTTON_NAMES[event.keyCode] ?: "Key ${event.keyCode}"
                Log.d(TAG, "Button released: $buttonName")
                return true
            }
        }
        return false
    }

    /**
     * Process a motion event from a gamepad
     */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        
        Log.d(TAG, "handleMotionEvent: action=${event.action}, source=${event.source}, " +
              "device=${event.device?.name ?: "Unknown"}")
        
        
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
            Pair("BRAKE", MotionEvent.AXIS_BRAKE),
            Pair("GENERIC_1", MotionEvent.AXIS_GENERIC_1),
            Pair("GENERIC_2", MotionEvent.AXIS_GENERIC_2)
        )
        
        val axisValues = axisList.filter { 
            event.getAxisValue(it.second) != 0f 
        }.joinToString(", ") { 
            "${it.first}=${event.getAxisValue(it.second)}" 
        }
        
        if (axisValues.isNotEmpty()) {
            Log.d(TAG, "Active axes: $axisValues")
        }

        
        val lx = event.getAxisValue(MotionEvent.AXIS_X)
        val ly = event.getAxisValue(MotionEvent.AXIS_Y)
        if (lx != 0f || ly != 0f) {
            leftStick.value = Pair(lx, ly)
        }

        
        val rx = event.getAxisValue(MotionEvent.AXIS_Z)
        val ry = event.getAxisValue(MotionEvent.AXIS_RZ)
        
        if (rx != 0f || ry != 0f) {
            rightStick.value = Pair(rx, ry)
        } else {
            
            val altRx = event.getAxisValue(MotionEvent.AXIS_RX)
            val altRy = event.getAxisValue(MotionEvent.AXIS_RY)
            if (altRx != 0f || altRy != 0f) {
                rightStick.value = Pair(altRx, altRy)
            }
        }

        
        val lt = event.getAxisValue(MotionEvent.AXIS_BRAKE)
        if (lt != 0f) {
            leftTrigger.floatValue = lt
        } else {
            
            val altLt = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
            if (altLt != 0f) {
                leftTrigger.floatValue = altLt
            }
        }

        val rt = event.getAxisValue(MotionEvent.AXIS_GAS)
        if (rt != 0f) {
            rightTrigger.floatValue = rt
        } else {
            
            val altRt = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
            if (altRt != 0f) {
                rightTrigger.floatValue = altRt
            }
        }

        return true  
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
                Log.d(TAG, "  Controller #: $controllerNumber, Descriptor: ${device.descriptor}")
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
