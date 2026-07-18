package com.example.audio

import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin

object WaveformSynthesizer {
    fun generateHighResTrack(
        file: File,
        durationSeconds: Int = 45,
        sampleRate: Int = 48000,
        bitsPerSample: Int = 16,
        type: String = "ambient"
    ) {
        val numChannels = 2 // Stereo
        val bytesPerSample = bitsPerSample / 8
        val blockAlign = numChannels * bytesPerSample
        val byteRate = sampleRate * blockAlign
        val totalAudioLen = durationSeconds.toLong() * byteRate
        val totalDataLen = totalAudioLen + 36

        FileOutputStream(file).use { fos ->
            val dos = DataOutputStream(fos)
            
            // RIFF header
            dos.writeBytes("RIFF")
            dos.writeLeInt(totalDataLen.toInt())
            dos.writeBytes("WAVE")
            
            // fmt subchunk
            dos.writeBytes("fmt ")
            dos.writeLeInt(16) // Subchunk1Size for PCM
            dos.writeLeShort(1) // AudioFormat (1 = PCM)
            dos.writeLeShort(numChannels)
            dos.writeLeInt(sampleRate)
            dos.writeLeInt(byteRate)
            dos.writeLeShort(blockAlign)
            dos.writeLeShort(bitsPerSample)
            
            // data subchunk
            dos.writeBytes("data")
            dos.writeLeInt(totalAudioLen.toInt())
            
            // Write PCM audio samples
            val totalSamples = durationSeconds * sampleRate
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                
                var leftVal: Double
                var rightVal: Double
                
                when (type) {
                    "ambient" -> {
                        // Ambient Flow: Eb minor drone + sweeping frequencies
                        val slowLfo = sin(2.0 * PI * 0.05 * t)
                        leftVal = sin(2.0 * PI * 155.56 * t) * 0.35 + 
                                  sin(2.0 * PI * 293.66 * t) * (0.12 + 0.04 * slowLfo) +
                                  sin(2.0 * PI * 77.78 * t) * 0.18
                        
                        rightVal = sin(2.0 * PI * 155.56 * t) * 0.35 + 
                                   sin(2.0 * PI * 233.08 * t) * (0.12 - 0.04 * slowLfo) +
                                   sin(2.0 * PI * 311.13 * t) * 0.08
                    }
                    "neon" -> {
                        // Neon Pulse: Upbeat pluck at 120BPM (2Hz tempo)
                        val beat = (t * 2.0) % 1.0
                        val pluckEnvelope = kotlin.math.exp(-5.5 * beat)
                        
                        val bar = (t / 2.0).toInt()
                        val freq = when (bar % 4) {
                            0 -> 110.0 // A
                            1 -> 130.81 // C
                            2 -> 98.0 // G
                            else -> 146.83 // D
                        }
                        
                        leftVal = sin(2.0 * PI * freq * t) * pluckEnvelope * 0.4
                        rightVal = sin(2.0 * PI * freq * 1.5 * t) * pluckEnvelope * 0.25
                    }
                    else -> {
                        // Zen Echoes: Bell chimes strike every 3 seconds with stereo delay
                        val phrase = (t / 3.0).toInt()
                        val trigger = t % 3.0
                        val bellEnvelope = kotlin.math.exp(-2.5 * trigger)
                        
                        val freq = when (phrase % 3) {
                            0 -> 523.25 // C5
                            1 -> 587.33 // D5
                            else -> 659.25 // E5
                        }
                        
                        val modulation = sin(2.0 * PI * (freq * 2.2) * t) * 0.4 * bellEnvelope
                        val signal = sin(2.0 * PI * freq * t + modulation) * bellEnvelope * 0.35
                        
                        val delayTrigger = (t - 0.35) % 3.0
                        val delayEnvelope = if (trigger > 0.35) kotlin.math.exp(-2.5 * delayTrigger) * 0.45 else 0.0
                        val delaySignal = sin(2.0 * PI * freq * (t - 0.35) + modulation) * delayEnvelope * 0.35
                        
                        leftVal = signal
                        rightVal = signal * 0.4 + delaySignal * 0.6
                    }
                }
                
                // Safe bounds
                leftVal = leftVal.coerceIn(-1.0, 1.0)
                rightVal = rightVal.coerceIn(-1.0, 1.0)
                
                if (bitsPerSample == 16) {
                    val lSample = (leftVal * 32767.0).toInt()
                    val rSample = (rightVal * 32767.0).toInt()
                    dos.writeLeShort(lSample)
                    dos.writeLeShort(rSample)
                } else {
                    // Write 24-bit
                    val lSample = (leftVal * 8388607.0).toInt()
                    val rSample = (rightVal * 8388607.0).toInt()
                    dos.writeLeInt24(lSample)
                    dos.writeLeInt24(rSample)
                }
            }
        }
    }
    
    private fun DataOutputStream.writeLeInt(v: Int) {
        writeByte(v and 0xFF)
        writeByte((v ushr 8) and 0xFF)
        writeByte((v ushr 16) and 0xFF)
        writeByte((v ushr 24) and 0xFF)
    }
    
    private fun DataOutputStream.writeLeShort(v: Int) {
        writeByte(v and 0xFF)
        writeByte((v ushr 8) and 0xFF)
    }
    
    private fun DataOutputStream.writeLeInt24(v: Int) {
        writeByte(v and 0xFF)
        writeByte((v ushr 8) and 0xFF)
        writeByte((v ushr 16) and 0xFF)
    }
}
