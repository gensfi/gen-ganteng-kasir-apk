package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.sin

object SoundHelper {
    /**
     * Plays a crisp, pleasant cash register chime ("tring" 1x) on clicking Konfirmasi & Bayar.
     */
    fun playCashChime(context: Context) {
        // Trigger subtle haptic confirmation
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(40)
                }
            }
        } catch (_: Exception) {}

        CoroutineScope(Dispatchers.IO).launch {
            var played = false
            // Primary: High fidelity bell chime synthesis ("tring" sound)
            try {
                val sampleRate = 44100
                val durationMs = 380
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)

                // Bell harmonics: C7 (2093 Hz) + E7 (2637 Hz) + G7 (3136 Hz) for metallic ring
                val f1 = 2093.0
                val f2 = 2637.0
                val f3 = 3136.0

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = exp(-t * 9.5) // Fast exponential decay
                    val wave = (0.50 * sin(2.0 * Math.PI * f1 * t) +
                               0.35 * sin(2.0 * Math.PI * f2 * t) +
                               0.15 * sin(2.0 * Math.PI * f3 * t)) * envelope
                    samples[i] = (wave * Short.MAX_VALUE * 0.95).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, numSamples)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 30)
                audioTrack.stop()
                audioTrack.release()
                played = true
            } catch (_: Exception) {}

            // Fallback: ToneGenerator on STREAM_MUSIC for 100% guarantee
            if (!played) {
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 260)
                    Thread.sleep(300)
                    toneGen.release()
                    played = true
                } catch (_: Exception) {}
            }

            // Final fallback: System notification chime
            if (!played) {
                try {
                    val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                    ringtone?.play()
                } catch (_: Exception) {}
            }
        }
    }
}
