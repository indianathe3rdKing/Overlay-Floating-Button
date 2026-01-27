package com.example.overlayfloatingbutton.gesture

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import com.example.overlayfloatingbutton.extensions.nextIntInOffset
import com.example.overlayfloatingbutton.extensions.safeLineTo
import com.example.overlayfloatingbutton.extensions.safeMoveTo


import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextInt

internal const val RANDOMIZATION_POSITION_MAX_OFFSET_PX = 5
internal const val RANDOMIZATION_DURATION_MAX_OFFSET_MS = 5L

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