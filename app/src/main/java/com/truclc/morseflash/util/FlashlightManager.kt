package com.truclc.morseflash.util

import android.app.Activity
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Window
import android.view.WindowManager

object FlashlightManager {

    fun turnOnFlashlight(activity: Activity) {
        // Adjust brightness
        adjustScreenBrightness(activity, 1.0f)

        // Vibrate to notify the user
        vibrate(activity)
    }

    fun turnOffFlashlight(activity: Activity) {

        // Adjust brightness to auto mode
        adjustScreenBrightness(activity, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
    }

    private fun adjustScreenBrightness(activity: Activity, brightness: Float) {
        val window = activity.window
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness
        window.attributes = layoutParams
    }

    private fun vibrate(activity: Activity) {
        val vibrator = activity.getSystemService(Activity.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }
}