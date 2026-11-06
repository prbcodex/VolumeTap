package com.pritambarman.volumetap

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.KeyEvent
import android.widget.Toast

/**
 * Global volume-key gesture handler.
 *
 * Double Volume Down = lock now.
 * Double Volume Up = wake display (never bypasses the lock screen).
 * A single press is replayed as a media-volume change after the double-tap window.
 */
class VolumeAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val doubleTapWindowMs = 420L

    private var pendingKeyCode = KeyEvent.KEYCODE_UNKNOWN
    private var pendingDownTime = 0L
    private var pendingSinglePress: Runnable? = null

    private val consumedKeyDowns = mutableSetOf<Int>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> handleVolumeDown(keyCode, event)
            KeyEvent.ACTION_UP -> consumedKeyDowns.remove(keyCode)
            else -> false
        }
    }

    private fun handleVolumeDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount > 0) {
            if (consumedKeyDowns.contains(keyCode)) {
                if (pendingKeyCode != keyCode) adjustMediaVolume(keyCode)
                return true
            }
            return false
        }

        val now = SystemClock.uptimeMillis()
        if (pendingKeyCode == keyCode && now - pendingDownTime <= doubleTapWindowMs) {
            pendingSinglePress?.let(handler::removeCallbacks)
            pendingSinglePress = null
            pendingKeyCode = KeyEvent.KEYCODE_UNKNOWN
            consumedKeyDowns.add(keyCode)
            runGesture(keyCode)
            return true
        }

        if (pendingKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            commitPendingSinglePress()
        }

        pendingKeyCode = keyCode
        pendingDownTime = now
        consumedKeyDowns.add(keyCode)

        val singlePress = Runnable {
            if (pendingKeyCode == keyCode) {
                adjustMediaVolume(keyCode)
                pendingKeyCode = KeyEvent.KEYCODE_UNKNOWN
                pendingSinglePress = null
            }
        }
        pendingSinglePress = singlePress
        handler.postDelayed(singlePress, doubleTapWindowMs)
        return true
    }

    private fun commitPendingSinglePress() {
        val oldKey = pendingKeyCode
        pendingSinglePress?.let(handler::removeCallbacks)
        pendingSinglePress = null
        pendingKeyCode = KeyEvent.KEYCODE_UNKNOWN
        if (oldKey == KeyEvent.KEYCODE_VOLUME_UP || oldKey == KeyEvent.KEYCODE_VOLUME_DOWN) {
            adjustMediaVolume(oldKey)
        }
    }

    private fun adjustMediaVolume(keyCode: Int) {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val direction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            AudioManager.ADJUST_RAISE
        } else {
            AudioManager.ADJUST_LOWER
        }
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun runGesture(keyCode: Int) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            lockDevice()
        } else {
            wakeDisplay()
        }
    }

    private fun lockDevice() {
        val admin = ComponentName(this, LockAdminReceiver::class.java)
        val policy = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!policy.isAdminActive(admin)) {
            Toast.makeText(this, "Open VolumeTap and enable Lock permission first", Toast.LENGTH_LONG).show()
            return
        }

        try {
            policy.lockNow()
        } catch (securityError: SecurityException) {
            Toast.makeText(this, "Lock permission is not active. Re-enable it in VolumeTap.", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun wakeDisplay() {
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        try {
            val wakeLock = power.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
                "VolumeTap:WakeDisplay"
            )
            wakeLock.acquire(1_500L)
        } catch (error: Exception) {
            Toast.makeText(this, "Wake shortcut is not supported by this phone's software", Toast.LENGTH_LONG).show()
        }
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        pendingSinglePress?.let(handler::removeCallbacks)
        pendingSinglePress = null
        super.onDestroy()
    }
}
