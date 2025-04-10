package com.example.deftcontroller.controller

import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

private const val TAG = "GameController"

/**
 * Core interface for any game controller implementation
 */
interface GameController {
    
    val isButtonAPressed: Boolean
    val isButtonBPressed: Boolean
    val isButtonXPressed: Boolean 
    val isButtonYPressed: Boolean
    val isButtonL1Pressed: Boolean
    val isButtonR1Pressed: Boolean
    val isButtonSelectPressed: Boolean
    val isButtonStartPressed: Boolean
    val isDpadUpPressed: Boolean
    val isDpadDownPressed: Boolean
    val isDpadLeftPressed: Boolean
    val isDpadRightPressed: Boolean
    val isL3Pressed: Boolean
    val isR3Pressed: Boolean
    
    
    val leftStickX: Float
    val leftStickY: Float
    val rightStickX: Float
    val rightStickY: Float
    val leftTrigger: Float
    val rightTrigger: Float
    
    
    val lastButtonName: State<String?>
    
    
    val controllerEvents: SharedFlow<ControllerEvent>
    
    
    fun processKeyEvent(event: KeyEvent): Boolean
    fun processMotionEvent(event: MotionEvent): Boolean
    
    
    fun reset()
}

/**
 * Events that can be emitted by the controller
 */
sealed class ControllerEvent {
    data class ButtonDown(val buttonCode: Int, val buttonName: String) : ControllerEvent()
    data class ButtonUp(val buttonCode: Int, val buttonName: String) : ControllerEvent()
    data class StickMoved(val stick: StickType, val x: Float, val y: Float) : ControllerEvent()
    data class TriggerMoved(val trigger: TriggerType, val value: Float) : ControllerEvent()
}

/**
 * Stick types
 */
enum class StickType {
    LEFT, RIGHT
}

/**
 * Trigger types
 */
enum class TriggerType {
    LEFT, RIGHT
}

/**
 * Default implementation of GameController interface
 */
class DefaultGameController : GameController {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    
    private val buttonStates = mutableMapOf<Int, Boolean>()
    
    
    private val _controllerEvents = MutableSharedFlow<ControllerEvent>(extraBufferCapacity = 64)
    override val controllerEvents = _controllerEvents.asSharedFlow()
    
    
    private val _lastButtonName = mutableStateOf<String?>(null)
    override val lastButtonName: State<String?> = _lastButtonName
    
    
    private val _leftStickState = mutableStateOf(Pair(0f, 0f))
    private val _rightStickState = mutableStateOf(Pair(0f, 0f))
    private val _leftTriggerState = mutableStateOf(0f)
    private val _rightTriggerState = mutableStateOf(0f)
    
    
    override val isButtonAPressed get() = buttonStates[KeyEvent.KEYCODE_BUTTON_A] ?: false
    override val isButtonBPressed get() = buttonStates[KeyEvent.KEYCODE_BUTTON_B] ?: false
    override val isButtonXPressed get() = buttonStates[KeyEvent.KEYCODE_BUTTON_X] ?: false
    override val isButtonYPressed get() = buttonStates[KeyEvent.KEYCODE_BUTTON_Y] ?: false
    override val isButtonL1Pressed get() = buttonStates[KeyEvent.KEYCODE_BUTTON_L1] ?: false
    override val isButtonR1Pressed get() = buttonStates[KeyEvent.KEYCODE_BUTTON_R1] ?: false
    override val isButtonSelectPressed get() = buttonStates[KeyEvent.KEYCODE_BUTTON_SELECT] ?: false
    override val isButtonStartPressed get() = buttonStates[KeyEvent.KEYCODE_BUTTON_START] ?: false
    override val isDpadUpPressed get() = buttonStates[KeyEvent.KEYCODE_DPAD_UP] ?: false
    override val isDpadDownPressed get() = buttonStates[KeyEvent.KEYCODE_DPAD_DOWN] ?: false
    override val isDpadLeftPressed get() = buttonStates[KeyEvent.KEYCODE_DPAD_LEFT] ?: false
    override val isDpadRightPressed get() = buttonStates[KeyEvent.KEYCODE_DPAD_RIGHT] ?: false
    override val isL3Pressed get() = buttonStates[KeyEvent.KEYCODE_BUTTON_THUMBL] ?: false
    override val isR3Pressed get() = buttonStates[KeyEvent.KEYCODE_BUTTON_THUMBR] ?: false
    
    
    override val leftStickX get() = _leftStickState.value.first
    override val leftStickY get() = _leftStickState.value.second
    override val rightStickX get() = _rightStickState.value.first
    override val rightStickY get() = _rightStickState.value.second
    override val leftTrigger get() = _leftTriggerState.value
    override val rightTrigger get() = _rightTriggerState.value
    
    
    private val buttonNames = mapOf(
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

    /**
     * Process a key event from the controller
     */
    override fun processKeyEvent(event: KeyEvent): Boolean {
        
        
        
        
        
        
        val buttonName = buttonNames[event.keyCode] ?: "Key ${event.keyCode}"
        
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                buttonStates[event.keyCode] = true
                _lastButtonName.value = buttonName
                
                
                scope.launch {
                    _controllerEvents.emit(ControllerEvent.ButtonDown(event.keyCode, buttonName))
                }
                
                Log.d(TAG, "Button pressed: $buttonName")
                return true
            }
            KeyEvent.ACTION_UP -> {
                buttonStates[event.keyCode] = false
                
                
                scope.launch {
                    _controllerEvents.emit(ControllerEvent.ButtonUp(event.keyCode, buttonName))
                }
                
                Log.d(TAG, "Button released: $buttonName")
                return true
            }
        }
        return false
    }

    /**
     * Process a motion event from the controller
     */
    override fun processMotionEvent(event: MotionEvent): Boolean {
        
        val lx = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_X), STICK_DEADZONE)
        val ly = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Y), STICK_DEADZONE)
        if (_leftStickState.value.first != lx || _leftStickState.value.second != ly) {
            _leftStickState.value = Pair(lx, ly)
            
            
            scope.launch {
                _controllerEvents.emit(ControllerEvent.StickMoved(StickType.LEFT, lx, ly))
            }
        }

        
        val rx: Float
        val ry: Float
        
        
        val z = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Z), STICK_DEADZONE)
        val rz = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_RZ), STICK_DEADZONE)
        
        if (z != 0f || rz != 0f) {
            rx = z
            ry = rz
        } else {
            
            rx = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_RX), STICK_DEADZONE)
            ry = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_RY), STICK_DEADZONE)
        }
        
        if (_rightStickState.value.first != rx || _rightStickState.value.second != ry) {
            _rightStickState.value = Pair(rx, ry)
            
            
            scope.launch {
                _controllerEvents.emit(ControllerEvent.StickMoved(StickType.RIGHT, rx, ry))
            }
        }
        
        
        
        
        var lt = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_BRAKE), TRIGGER_DEADZONE)
        if (lt == 0f) {
            
            lt = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), TRIGGER_DEADZONE)
        }
        
        if (_leftTriggerState.value != lt) {
            _leftTriggerState.value = lt
            
            
            scope.launch {
                _controllerEvents.emit(ControllerEvent.TriggerMoved(TriggerType.LEFT, lt))
            }
        }
        
        
        var rt = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_GAS), TRIGGER_DEADZONE)
        if (rt == 0f) {
            
            rt = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), TRIGGER_DEADZONE)
        }
        
        if (_rightTriggerState.value != rt) {
            _rightTriggerState.value = rt
            
            
            scope.launch {
                _controllerEvents.emit(ControllerEvent.TriggerMoved(TriggerType.RIGHT, rt))
            }
        }

        return true
    }
    
    /**
     * Reset all controller state
     */
    override fun reset() {
        buttonStates.clear()
        _leftStickState.value = Pair(0f, 0f)
        _rightStickState.value = Pair(0f, 0f)
        _leftTriggerState.value = 0f
        _rightTriggerState.value = 0f
        _lastButtonName.value = null
    }
    
    /**
     * Apply deadzone to analog inputs to prevent drift
     */
    private fun applyDeadzone(value: Float, deadzone: Float): Float {
        return if (kotlin.math.abs(value) < deadzone) {
            0f
        } else {
            
            val sign = if (value > 0) 1 else -1
            val normalizedValue = (kotlin.math.abs(value) - deadzone) / (1 - deadzone) 
            sign * normalizedValue
        }
    }
    
    companion object {
        
        const val STICK_DEADZONE = 0.1f   
        const val TRIGGER_DEADZONE = 0.05f 
    }
}
