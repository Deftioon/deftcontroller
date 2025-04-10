package com.example.deftcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.deftcontroller.controller.ControllerEvent
import com.example.deftcontroller.controller.GameController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A composable that visualizes gamepad input state
 */
@Composable
fun GamepadVisualizer(
    gameController: GameController,
    modifier: Modifier = Modifier
) {
    // Track events for debug display
    val eventLog = remember { MutableStateFlow<String>("No events yet") }
    
    // Collect controller events
    val events by gameController.controllerEvents.collectAsState(initial = null)
    
    // Update event log when events are received
    events?.let { event ->
        when (event) {
            is ControllerEvent.ButtonDown -> {
                eventLog.update { "Button Down: ${event.buttonName}" }
            }
            is ControllerEvent.ButtonUp -> {
                eventLog.update { "Button Up: ${event.buttonName}" }
            }
            is ControllerEvent.StickMoved -> {
                val stickName = if (event.stick.name == "LEFT") "Left Stick" else "Right Stick"
                eventLog.update { "$stickName: X=${String.format("%.2f", event.x)}, Y=${String.format("%.2f", event.y)}" }
            }
            is ControllerEvent.TriggerMoved -> {
                val triggerName = if (event.trigger.name == "LEFT") "Left Trigger" else "Right Trigger"
                eventLog.update { "$triggerName: ${String.format("%.2f", event.value)}" }
            }
        }
    }
    
    // Get latest values from controller
    val lastButton by gameController.lastButtonName // This should now work correctly with State<String?>
    val eventLogValue by eventLog.collectAsState()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Last pressed button
            Text(
                "Last Button: ${lastButton ?: "None"}",
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Event log display
            Text("Last Event: $eventLogValue")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stick visualization and values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Left stick
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Left Stick")
                    StickVisualizer(
                        x = gameController.leftStickX,
                        y = gameController.leftStickY,
                        modifier = Modifier.size(100.dp)
                    )
                    Text("X: ${String.format("%.2f", gameController.leftStickX)}")
                    Text("Y: ${String.format("%.2f", gameController.leftStickY)}")
                }
                
                // Right stick
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Right Stick")
                    StickVisualizer(
                        x = gameController.rightStickX,
                        y = gameController.rightStickY,
                        modifier = Modifier.size(100.dp)
                    )
                    Text("X: ${String.format("%.2f", gameController.rightStickX)}")
                    Text("Y: ${String.format("%.2f", gameController.rightStickY)}")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Left trigger
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Left Trigger")
                    TriggerVisualizer(
                        value = gameController.leftTrigger,
                        modifier = Modifier
                            .height(100.dp)
                            .width(30.dp)
                    )
                    Text("${String.format("%.2f", gameController.leftTrigger)}")
                }
                
                // Right trigger
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Right Trigger")
                    TriggerVisualizer(
                        value = gameController.rightTrigger,
                        modifier = Modifier
                            .height(100.dp)
                            .width(30.dp)
                    )
                    Text("${String.format("%.2f", gameController.rightTrigger)}")
                }
            }
            
            // Button state display
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column {
                    ButtonStateIndicator(name = "A", isPressed = gameController.isButtonAPressed)
                    ButtonStateIndicator(name = "X", isPressed = gameController.isButtonXPressed)
                    ButtonStateIndicator(name = "L1", isPressed = gameController.isButtonL1Pressed)
                    ButtonStateIndicator(name = "L3", isPressed = gameController.isL3Pressed)
                }
                
                Column {
                    ButtonStateIndicator(name = "B", isPressed = gameController.isButtonBPressed)
                    ButtonStateIndicator(name = "Y", isPressed = gameController.isButtonYPressed)
                    ButtonStateIndicator(name = "R1", isPressed = gameController.isButtonR1Pressed)
                    ButtonStateIndicator(name = "R3", isPressed = gameController.isR3Pressed)
                }
                
                Column {
                    ButtonStateIndicator(name = "Select", isPressed = gameController.isButtonSelectPressed)
                    ButtonStateIndicator(name = "Start", isPressed = gameController.isButtonStartPressed)
                    ButtonStateIndicator(name = "D-Up", isPressed = gameController.isDpadUpPressed)
                    ButtonStateIndicator(name = "D-Down", isPressed = gameController.isDpadDownPressed)
                }
                
                Column {
                    Spacer(Modifier.height(24.dp))
                    Spacer(Modifier.height(24.dp))
                    ButtonStateIndicator(name = "D-Left", isPressed = gameController.isDpadLeftPressed)
                    ButtonStateIndicator(name = "D-Right", isPressed = gameController.isDpadRightPressed)
                }
            }
        }
    }
}

@Composable
fun StickVisualizer(x: Float, y: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, Color.Gray)
            .background(Color.LightGray.copy(alpha = 0.3f))
    ) {
        val stickSize = 20.dp
        
        Box(
            modifier = Modifier
                .size(stickSize)
                .offset(
                    x = (x * 40).dp, // Scale x by a fixed amount for visibility 
                    y = (y * 40).dp  // Scale y by a fixed amount for visibility
                )
                .background(Color.Red, CircleShape)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun TriggerVisualizer(value: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, Color.Gray)
            .background(Color.LightGray.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(value.coerceIn(0f, 1f)) // Ensure value is between 0 and 1
                .background(Color.Red)
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ButtonStateIndicator(name: String, isPressed: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(name, Modifier.width(40.dp))
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(if (isPressed) Color.Green else Color.Gray, CircleShape)
        )
    }
}
