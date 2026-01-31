package com.catdiego.turbocore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Button(onClick = { executar("settings put secure display_density_forced 90") }) {
                Text("MODO CAPA (DPI 90)")
            }
        }
    }

    private fun executar(cmd: String) {
        try {
            Runtime.getRuntime().exec(cmd)
            Runtime.getRuntime().exec("settings put global animator_duration_scale 1.0")
        } catch (e: IOException) { e.printStackTrace() }
    }
}
