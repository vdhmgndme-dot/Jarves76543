package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build

data class TelemetryState(
    val batteryPct: Int = 100,
    val isCharging: Boolean = false,
    val networkStatus: String = "ONLINE",
    val isConnected: Boolean = true,
    val ramAvailableMb: Long = 0,
    val totalRamMb: Long = 0,
    val osVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"
)

object SystemTelemetry {

    fun getTelemetry(context: Context): TelemetryState {
        // Battery
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, batteryFilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        // Network
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = connMgr?.activeNetwork
        val caps = connMgr?.getNetworkCapabilities(activeNet)
        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val netStatus = when {
            !isConnected -> "OFFLINE"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WIFI (HIGH SPD)"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "CELLULAR (LTE/5G)"
            else -> "CONNECTED"
        }

        // Memory
        val actMgr = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actMgr?.getMemoryInfo(memInfo)
        val ramAvailable = memInfo.availMem / (1024 * 1024)
        val totalRam = memInfo.totalMem / (1024 * 1024)

        return TelemetryState(
            batteryPct = batteryPct,
            isCharging = isCharging,
            networkStatus = netStatus,
            isConnected = isConnected,
            ramAvailableMb = ramAvailable,
            totalRamMb = totalRam
        )
    }
}
