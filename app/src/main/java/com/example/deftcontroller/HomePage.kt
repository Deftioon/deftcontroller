package com.example.deftcontroller

import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.zIndex
import com.example.deftcontroller.controller.GamepadManager
import com.example.deftcontroller.ui.GamepadVisualizer
import kotlinx.coroutines.delay

@Composable
fun HomePageContent() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    
    val gamepadManager = remember { GamepadManager(context) }
    
    
    val connectedGamepads = gamepadManager.connectedGamepads
    
    
    var debugLog by remember { mutableStateOf("") }
    
    
    LaunchedEffect(Unit) {
        gamepadManager.refreshConnectedGamepads()
        gamepadManager.logAllInputDevices()
        
        
        debugLog = "Refreshed connected devices: ${connectedGamepads.size} found"
    }
    
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        
        GamepadInputCapture(
            gamepadManager = gamepadManager,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f) 
        )
        
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                "Gamepad Tester", 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            
            Text("Connected Controllers: ${connectedGamepads.size}", 
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp))
            
            for (device in connectedGamepads) {
                Text("• ${device.name} (ID: ${device.id})")
            }
            
            
            Button(
                onClick = { 
                    gamepadManager.refreshConnectedGamepads() 
                    gamepadManager.logAllInputDevices()
                    debugLog = "Refreshed connected devices: ${connectedGamepads.size} found"
                },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("Refresh Controllers")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            
            GamepadVisualizer(
                gameController = gamepadManager.gameController,
                modifier = Modifier.fillMaxWidth()
            )
            
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun GamepadInputCapture(gamepadManager: GamepadManager, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    
    AndroidView(
        factory = { context ->
            object : View(context) {
                init {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()
                    Log.d("GamepadCapture", "View created and focus requested")
                }
                
                override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
                    Log.d("GamepadCapture", "onKeyDown received: $keyCode")
                    return gamepadManager.handleKeyEvent(event) || super.onKeyDown(keyCode, event)
                }
                
                override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
                    Log.d("GamepadCapture", "onKeyUp received: $keyCode")
                    return gamepadManager.handleKeyEvent(event) || super.onKeyUp(keyCode, event)
                }
                
                override fun onGenericMotionEvent(event: MotionEvent): Boolean {
                    Log.d("GamepadCapture", "onGenericMotionEvent received")
                    return gamepadManager.handleMotionEvent(event) || super.onGenericMotionEvent(event)
                }
                
                override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
                    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
                    Log.d("GamepadCapture", "Focus changed: $gainFocus")
                    hasFocus = gainFocus
                }
            }
        },
        modifier = modifier
            .focusRequester(focusRequester)
    )
    
    LaunchedEffect(Unit) {
        while (true) {
            focusRequester.requestFocus()
            delay(1000)
        }
    }
}