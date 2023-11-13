package com.example.kotin_stressapp.presentation

import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.kotin_stressapp.R
import com.example.kotin_stressapp.presentation.theme.KotinstressappTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {

/*
Stopped = 0
Running = 1
Paused = 2
*/

//    var appState: AppState = AppState.STOPPED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp("Android")
        }
    }

    @Composable
    fun WearApp(
        greetingName: String
    ) {
        KotinstressappTheme {
            /* If you have enough items in your list, use [ScalingLazyColumn] which is an optimized
             * version of LazyColumn for wear devices with some added features. For more information,
             * see d.android.com/wear/compose.
             */
            var appState = remember { mutableStateOf(AppState.STOPPED) }
            //var appState: AppState by rememberMutableStateOf(AppState.STOPPED)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .fillMaxSize()
                    .padding(8.dp)
                    .background(MaterialTheme.colors.background),

                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Greeting(greetingName = greetingName)

                if (appState.value == AppState.STOPPED){ //while app is not running
                    Button(
                        onClick = { //running the app
                            Log.d("Button - Start", "button clicked")
                            appState.value = AppState.RUNNING
                            appRunning(appState)
                        }
                    ) {
                        Text("Start")
                    }
                }
                else{ //while app is running OR paused
                    Log.d("Button - Stop/Pause", "button should have changed")
                    Button(
                        onClick = { //stopping the app
                            appState.value = AppState.STOPPED
                            appStopping(appState)

                        }
                    ) {
                        Text("Stop")
                    }

                    //Resume + Pause buttons
                    Button(
                        onClick = {
                            if (appState.value == AppState.PAUSE){ //app is paused
                                //unpause it
                                appState.value = AppState.RUNNING
                            }
                            else{ //app is running
                                //pause it
                                appState.value = AppState.PAUSE
                                appPausing(appState)
                            }
                        }
                    ) {
                        if (appState.value == AppState.PAUSE){ //app is paused
                            //prepare for resume
                            Text("Resume")
                        }
                        else{ //app is running
                            //prepare for pause
                            Text("Pause")
                        }

                    }
                }
            }
        }
    }

    fun appRunning(state: MutableState<AppState>) {
        //start button was pressed, what will you do?
        while (state.value == AppState.RUNNING) {
            break
        }
    }

    fun appStopping(state: MutableState<AppState>){
        //stop button was pressed, what will you do?
        while (state.value == AppState.STOPPED) {
            break
        }
    }

    fun appPausing(state: MutableState<AppState>){
        //pause button was pressed, what will you do?
        while (state.value == AppState.PAUSE) {
            break
        }
    }

    fun appResuming(state: MutableState<AppState>){
        //resume button was pressed, what will you do?

    }

    @Composable
    fun Greeting(greetingName: String) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            text = stringResource(R.string.hello_world, greetingName)
        )
    }
}

