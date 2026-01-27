package com.example.overlayfloatingbutton.gesture



import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.content.ContentValues.TAG
import android.gesture.Gesture
import android.util.Log

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

@Singleton
internal class GestureExecutor @Inject constructor(){

    private var resultCallback: GestureResultCallback?= null
    private var currentContinuation: Continuation<Boolean>?= null

    private var completeGesture:Long = 0L
    private var cancelledGestures:Long = 0L
    private var errorGestures:Long=0L

    fun clear(){
        completeGesture=0L
        cancelledGestures=0L
        errorGestures=0L

        resultCallback=null
        currentContinuation=null
    }

    suspend fun dispatchGesture(service: AccessibilityService,gesture: GestureDescription):Boolean{
        if (currentContinuation!=null){
            Log.w(TAG,"Previous gesture result is not availaable yet, clearing listener to avoid stale events")

        resultCallback= null
        currentContinuation=null}

        resultCallback= resultCallback ?: newGestureResultCallback()
        return suspendCoroutine { continuation ->
            currentContinuation= continuation

            try {
                service.dispatchGesture(gesture,resultCallback,null)
            }catch (rEx: RuntimeException){
                Log.w(TAG,"System is not responsive, the user might be spamming gesture too quick",rEx)
                errorGestures++
                resumeExecution(gestureError =true)
            }
        }
    }


}