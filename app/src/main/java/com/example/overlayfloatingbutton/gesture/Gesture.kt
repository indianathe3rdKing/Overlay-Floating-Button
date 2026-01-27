package com.example.overlayfloatingbutton.gesture

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import com.example.overlayfloatingbutton.extensions.nextIntInOffset
import com.example.overlayfloatingbutton.extensions.nextLongInOffset
import com.example.overlayfloatingbutton.extensions.safeLineTo
import com.example.overlayfloatingbutton.extensions.safeMoveTo


import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextInt

internal const val RANDOMIZATION_POSITION_MAX_OFFSET_PX = 5
internal const val RANDOMIZATION_DURATION_MAX_OFFSET_MS = 5L
internal const val MINIMUM_STROKE_DURATION_MS = 1L
internal const val MAXIMUM_STROKE_DURATION_MS = 40L
fun Path.moveTo(position: Point,random: Random?){

    if(random== null) safeMoveTo(position.x, position.y)
    else safeMoveTo(
        random.nextIntInOffset(position.x, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
        random.nextIntInOffset(position.y, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
    )
}

fun Path.line(from: Point?,to: Point?,random: Random?){
    if(from==null || to == null) return

    moveTo(from,random)
    lineTo(to,random)
}

private fun Path.lineTo(position: Point,random: Random?){
    if (random == null ) safeLineTo(position.x,position.y)
    else safeLineTo(
        random.nextIntInOffset(position.x,RANDOMIZATION_POSITION_MAX_OFFSET_PX),
        random.nextIntInOffset(position.y,RANDOMIZATION_POSITION_MAX_OFFSET_PX)
    )
}

fun GestureDescription.Builder.buildSingletonStroke(
    path: Path,
    durationMs: Long,
    startTime: Long=0,
    random:Random?,
): GestureDescription{
    val actualDurationMs =random?.nextLongInOffset(durationMs,RANDOMIZATION_DURATION_MAX_OFFSET_MS)
        ?:durationMs

    try {
        addStroke(
            GestureDescription.StrokeDescription(
                path,
                startTime.toNormalizedStrokeStartTime(),
                actualDurationMs.toNormalizedStrokeDurationsMs()
            )
        )
    }catch (ex: IllegalStateException){
        throw IllegalStateException("Invalid gesture: Duration= $durationMs",ex)
    }catch (ex: IllegalArgumentException){
        throw IllegalStateException("Invalid gesture: Duration = $durationMs",ex)
    }

    return build()
}

private fun Long.toNormalizedStrokeStartTime():Long =
    max(0,this)

private fun Long.toNormalizedStrokeDurationsMs(): Long=
    max(MINIMUM_STROKE_DURATION_MS,min(MAXIMUM_STROKE_DURATION_MS,this))