package com.securitytraining.weatherapp

import android.app.Application
import com.securitytraining.weatherapp.di.appModule
import com.securitytraining.weatherapp.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WeatherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WeatherApplication)
            modules(networkModule, appModule)
        }
    }
}
