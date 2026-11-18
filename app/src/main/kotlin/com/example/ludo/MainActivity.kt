package com.example.ludo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ludo.data.AppContainer
import com.example.ludo.ui.LudoApp

class MainActivity : ComponentActivity() {
    private val container by lazy { AppContainer(applicationContext) }
    private val ads by lazy { Ads(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ads.start()
        setContent { LudoApp(viewModel { AppViewModel(container) }, ads) }
    }
}
