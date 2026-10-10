package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class SoundAndHapticHelper(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        } catch (e: Exception) {
            Log.e("SoundHelper", "Could not initialize ToneGenerator", e)
        }
    }

    /**
     * Classic supermarket checkout scanner beep!
     */
    fun playCheckoutBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
        } catch (e: Exception) {
            Log.e("SoundHelper", "Failed to play beep", e)
        }
    }

    /**
     * Cheerful double chime when a list is 100% completed or when a family member updates the list
     */
    fun playSuccessChime() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        } catch (e: Exception) {
            Log.e("SoundHelper", "Failed to play chime", e)
        }
    }

    /**
     * Subtle family activity notification sound
     */
    fun playFamilyActivitySound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (e: Exception) {
            Log.e("SoundHelper", "Failed to play sound", e)
        }
    }

    /**
     * Haptic feedback for item check (quick crisp click)
     */
    fun vibrateItemChecked() {
        try {
            val vibrator = getVibrator()
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(35)
                }
            }
        } catch (e: Exception) {
            Log.e("SoundHelper", "Vibrate failed", e)
        }
    }

    /**
     * Celebratory vibration pattern when 100% is reached
     */
    fun vibrateCelebration() {
        try {
            val vibrator = getVibrator()
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 80, 50, 100, 50, 150)
                    val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 80, 50, 100, 50, 150), -1)
                }
            }
        } catch (e: Exception) {
            Log.e("SoundHelper", "Celebration vibrate failed", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
