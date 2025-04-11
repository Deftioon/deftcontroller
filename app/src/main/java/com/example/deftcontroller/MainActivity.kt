package com.example.deftcontroller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.deftcontroller.ui.theme.DeftcontrollerTheme
import kotlinx.coroutines.launch
private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    
    private lateinit var gamepadManager: GamepadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        
        gamepadManager = GamepadManager(this)
        
        enableEdgeToEdge()
        setContent {
            DeftcontrollerTheme {
                Surface(modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background) {
                    MainContent(gamepadManager)
                }
            }
        }
        
        
        window.decorView.setOnKeyListener { _, keyCode, event ->
            Log.d(TAG, "Window key event: keyCode=$keyCode")
            gamepadManager.handleKeyEvent(event)
            false 
        }
        
        
        gamepadManager.logAllInputDevices()
        Log.d(TAG, "Activity created, GamepadManager initialized")
    }
    
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Log.d(TAG, "Activity onKeyDown: $keyCode")
        return if (gamepadManager.handleKeyEvent(event)) {
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }
    
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        Log.d(TAG, "Activity onKeyUp: $keyCode")
        return if (gamepadManager.handleKeyEvent(event)) {
            true
        } else {
            super.onKeyUp(keyCode, event)
        }
    }
    
    
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        Log.d(TAG, "Activity onGenericMotionEvent")
        return if (gamepadManager.handleMotionEvent(event)) {
            true
        } else {
            super.onGenericMotionEvent(event)
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(gamepadManager: GamepadManager? = null) {
    var selectedPage by remember {
        mutableIntStateOf(0)
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    
    val context = LocalContext.current


    gamepadManager ?: GamepadManager(context)

    val navItems = listOf(
        NavigationItem("Home", Icons.Filled.Home, 0),
        NavigationItem("Settings", Icons.Filled.Settings, 1)
    )

    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = remember { bluetoothManager?.adapter }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(200.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                navItems.forEach {
                    item -> NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedPage == item.route,
                        onClick = {
                            selectedPage = item.route
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(),
                        title = { Text("Controller") },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            ) {
                innerPadding -> Surface (
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedPage) {
                        0 -> HomePageContent() 
                        1 -> SettingsPageContent(bluetoothAdapter = bluetoothAdapter)
                        else -> Text("Unknown Page")
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    DeftcontrollerTheme {
        MainContent()
    }
}