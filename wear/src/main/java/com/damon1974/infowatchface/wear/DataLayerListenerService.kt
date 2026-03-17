package com.damon1974.infowatchface.wear

import android.content.ComponentName
import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class DataLayerListenerService : WearableListenerService() {

    override fun onCreate() {
        super.onCreate()
        Log.d("DataLayer", "DataLayerListenerService created")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d("DataLayer", "onDataChanged: ${dataEvents.count} events")
        dataEvents.forEach { event ->
            val path = event.dataItem.uri.path ?: return@forEach
            Log.d("DataLayer", "path: $path")
            if (path == "/usage") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val session = dataMap.getInt("session_pct", 0)
                val weekly = dataMap.getInt("weekly_pct", 0)
                val mcOnline = dataMap.getInt("mc_online", -1)
                val mcMax = dataMap.getInt("mc_max", -1)
                val rustOnline = dataMap.getInt("rust_online", -1)
                val rustMax = dataMap.getInt("rust_max", -1)
                Log.d("DataLayer", "DataChanged: session=$session weekly=$weekly mc=$mcOnline/$mcMax rust=$rustOnline/$rustMax")
                updateComplications(session, weekly, mcOnline, mcMax, rustOnline, rustMax)
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        Log.d("DataLayer", "onMessageReceived: path=${event.path}")
        if (event.path == "/usage") {
            val parts = String(event.data).split(",")
            if (parts.size >= 2) {
                val session = parts[0].toIntOrNull() ?: 0
                val weekly = parts[1].toIntOrNull() ?: 0
                val mcOnline = if (parts.size > 2) parts[2].toIntOrNull() ?: -1 else -1
                val mcMax = if (parts.size > 3) parts[3].toIntOrNull() ?: -1 else -1
                val rustOnline = if (parts.size > 4) parts[4].toIntOrNull() ?: -1 else -1
                val rustMax = if (parts.size > 5) parts[5].toIntOrNull() ?: -1 else -1
                Log.d("DataLayer", "MessageReceived: session=$session weekly=$weekly mc=$mcOnline/$mcMax rust=$rustOnline/$rustMax")
                updateComplications(session, weekly, mcOnline, mcMax, rustOnline, rustMax)
            }
        }
    }

    private fun updateComplications(session: Int, weekly: Int, mcOnline: Int = -1, mcMax: Int = -1, rustOnline: Int = -1, rustMax: Int = -1) {
        UsageCache.save(applicationContext, session, weekly)
        if (mcOnline >= 0) UsageCache.saveMc(applicationContext, mcOnline, mcMax)
        if (rustOnline >= 0) UsageCache.saveRust(applicationContext, rustOnline, rustMax)
        ComplicationDataSourceUpdateRequester.create(
            applicationContext,
            ComponentName(applicationContext, SessionComplicationService::class.java)
        ).requestUpdateAll()
        ComplicationDataSourceUpdateRequester.create(
            applicationContext,
            ComponentName(applicationContext, WeeklyComplicationService::class.java)
        ).requestUpdateAll()
        ComplicationDataSourceUpdateRequester.create(
                applicationContext,
                ComponentName(applicationContext, RustComplicationService::class.java)
            ).requestUpdateAll()
            ComplicationDataSourceUpdateRequester.create(
                applicationContext,
                ComponentName(applicationContext, McComplicationService::class.java)
            ).requestUpdateAll()
            Log.d("DataLayer", "Complications update requested")
    }
}
