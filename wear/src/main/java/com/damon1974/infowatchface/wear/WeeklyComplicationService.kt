package com.damon1974.infowatchface.wear

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class WeeklyComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.RANGED_VALUE) return null
        return RangedValueComplicationData.Builder(
            value = 41f, min = 0f, max = 100f,
            contentDescription = PlainComplicationText.Builder("Weekly usage").build()
        ).setText(PlainComplicationText.Builder("W 41%").build()).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.RANGED_VALUE) return null
        val pct = UsageCache.weeklyPct(applicationContext)
        android.util.Log.d("WeeklyComplication", "onComplicationRequest called, pct=$pct")
        return RangedValueComplicationData.Builder(
            value = pct.toFloat(), min = 0f, max = 100f,
            contentDescription = PlainComplicationText.Builder("Weekly usage").build()
        ).setText(PlainComplicationText.Builder("W $pct%").build()).build()
    }
}
