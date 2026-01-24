package com.example.overlayfloatingbutton.lifecycle_owner

import android.os.Bundle
import androidx.lifecycle.*
import androidx.savedstate.*

internal class MyLifecycleOwner: SavedStateRegistryOwner{
    private var mLifecycleRegistry: LifecycleRegistry= LifecycleRegistry(this)
    private var mSaveStateRegistryController: SavedStateRegistryController=
        SavedStateRegistryController.create(this)

    val isInitialised: Boolean
        get()=true

    override val savedStateRegistry: SavedStateRegistry
        get() = mSaveStateRegistryController.savedStateRegistry
    override val lifecycle: Lifecycle
        get() = mLifecycleRegistry

    fun setCurrentState(state: Lifecycle.State){
        mLifecycleRegistry.currentState=state
    }
    fun handleLifecycleEvent(event: Lifecycle.Event){
        mLifecycleRegistry.handleLifecycleEvent(event)
    }
    fun performRestore(savedState: Bundle?){
        mSaveStateRegistryController.performRestore(savedState)
    }
    fun performSave(outBundle: Bundle){
        mSaveStateRegistryController.performRestore(outBundle)
    }
}