package com.damon1974.infowatchface.phone

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class SessionComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.RANGED_VALUE) return null
        return RangedValueComplicationData.Builder(
            value = 65f, min = 0f, max = 100f,
            contentDescription = PlainComplicationText.Builder("Session usage").build()
        ).setText(PlainComplicationText.Builder("S: 65%").build()).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.RANGED_VALUE) return null
        val ctx = applicationContext
        val pct = UsageCache.sessionPct(ctx)
        return RangedValueComplicationData.Builder(
            value = pct.toFloat(), min = 0f, max = 100f,
            contentDescription = PlainComplicationText.Builder("Session usage").build()
        ).setText(PlainComplicationText.Builder("S: $pct%").build()).build()
    }
}
