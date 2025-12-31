package com.auralink.services

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

class AudioRouter(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var originalRingtoneVolume: Int = -1

    fun enableBluetoothSCO() {
        if (!isBluetoothHeadsetConnected()) return

        audioManager.mode = AudioManager.MODE_IN_CALL
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true
    }

    fun disableBluetoothSCO() {
        audioManager.isBluetoothScoOn = false
        audioManager.stopBluetoothSco()
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    /**
     * Lowers ringtone volume to minimum (1) to prevent interference with announcement
     */
    fun muteRingtone() {
        originalRingtoneVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 1, 0)
    }

    /**
     * Restores ringtone volume to its original level
     */
    fun restoreRingtone() {
        if (originalRingtoneVolume != -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, originalRingtoneVolume, 0)
            originalRingtoneVolume = -1
        }
    }

    fun isBluetoothHeadsetConnected(): Boolean {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_BLE_HEADSET) {
                return true
            }
        }
        return false
    }
}
