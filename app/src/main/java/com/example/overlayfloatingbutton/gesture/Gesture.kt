package com.example.overlayfloatingbutton.gesture

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextInt

fun Path.moveTo(position: Point,random: Random){
    if(random== null) safeMoveTo(postion.x, position.y)
    else safeMoveTo(
        random.nextIntInOffset(position.x, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
        random.nextIntInOffset(position.y, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
    )
}
