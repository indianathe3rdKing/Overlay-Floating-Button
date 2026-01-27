package com.example.overlayfloatingbutton.gesture



import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.util.Log

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation

@Singleton
internal class GestureExecutor @Inject constructor(){

    private var resultCallback: GestureResultCallback?= null
    private var currentContinuation: Continuation<Boolean>?= null

    private var completeGesture:Long = 0L
    private var cancelledGestures:Long = 0L
    private var errorGestures:Long=0L




}