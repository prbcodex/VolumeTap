package com.pritambarman.volumetap

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var lockStatus: TextView
    private lateinit var serviceStatus: TextView
    private lateinit var readyStatus: TextView

    private val adminComponent by lazy { ComponentName(this, LockAdminReceiver::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        if (::lockStatus.isInitialized) refreshStatus()
    }

    private fun buildScreen() {
        val page = ScrollView(this).apply {
            setBackgroundColor(0xFFF4F6FB.toInt())
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(26), dp(22), dp(28))
        }

        val brand = TextView(this).apply {
            text = "VolumeTap"
            textSize = 30f
            setTextColor(0xFF17223B.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        content.addView(brand, matchWrap())

        val intro = TextView(this).apply {
            text = "Give your Galaxy M32 a power-button alternative."
            textSize = 15f
            setTextColor(0xFF65708A.toInt())
            setPadding(0, dp(6), 0, dp(18))
        }
        content.addView(intro, matchWrap())

        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(15), dp(17), dp(15))
            background = rounded(0xFFFFFFFF.toInt(), dp(18))
            elevation = dp(2).toFloat()
        }
        readyStatus = TextView(this).apply {
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF17223B.toInt())
        }
        lockStatus = statusLine()
        serviceStatus = statusLine()
        statusCard.addView(readyStatus, matchWrap())
        statusCard.addView(lockStatus, matchWrap().apply { topMargin = dp(12) })
        statusCard.addView(serviceStatus, matchWrap().apply { topMargin = dp(5) })
        content.addView(statusCard, matchWrap().apply { bottomMargin = dp(20) })

        content.addView(sectionTitle("Your shortcuts"), matchWrap())
        content.addView(infoCard("DOUBLE-TAP VOLUME DOWN", "Lock the phone immediately."), matchWrap().apply { bottomMargin = dp(9) })
        content.addView(infoCard("DOUBLE-TAP VOLUME UP", "Wake the display and show the lock screen."), matchWrap().apply { bottomMargin = dp(9) })
        content.addView(infoCard("SINGLE PRESS", "Still changes media volume. There is a short delay while VolumeTap checks for a second tap."), matchWrap().apply { bottomMargin = dp(20) })

        content.addView(sectionTitle("One-time setup"), matchWrap())
        content.addView(actionButton("1. Enable lock permission", 0xFF3157D5.toInt()) { requestDeviceAdmin() }, matchWrap().apply { bottomMargin = dp(10) })
        content.addView(actionButton("2. Enable VolumeTap button service", 0xFF3157D5.toInt()) { openAccessibilitySettings() }, matchWrap().apply { bottomMargin = dp(10) })
        content.addView(actionButton("Test screen lock", 0xFF17223B.toInt()) { testLock() }, matchWrap().apply { bottomMargin = dp(10) })
        content.addView(actionButton("Battery / background settings", 0xFF5B6478.toInt()) { openAppSettings() }, matchWrap().apply { bottomMargin = dp(18) })

        val note = TextView(this).apply {
            text = "Important: Wake means turning the display on, not bypassing your PIN, pattern or fingerprint. If Samsung does not deliver volume-key events while the display is fully asleep, the wake shortcut may not work on that firmware."
            textSize = 13f
            setTextColor(0xFF65708A.toInt())
            setPadding(dp(2), 0, dp(2), 0)
        }
        content.addView(note, matchWrap())

        page.addView(content)
        setContentView(page)
        refreshStatus()
    }

    private fun statusLine(): TextView = TextView(this).apply {
        textSize = 14f
        setTextColor(0xFF65708A.toInt())
    }

    private fun sectionTitle(value: String): TextView = TextView(this).apply {
        text = value
        textSize = 18f
        setTextColor(0xFF17223B.toInt())
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(2), 0, dp(10))
    }

    private fun infoCard(heading: String, detail: String): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(13))
            background = rounded(0xFFFFFFFF.toInt(), dp(14))
        }
        box.addView(TextView(this).apply {
            text = heading
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF3157D5.toInt())
        }, matchWrap())
        box.addView(TextView(this).apply {
            text = detail
            textSize = 14f
            setTextColor(0xFF39445A.toInt())
            setPadding(0, dp(5), 0, 0)
        }, matchWrap())
        return box
    }

    private fun actionButton(label: String, color: Int, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 14f
        isAllCaps = false
        setTextColor(0xFFFFFFFF.toInt())
        background = rounded(color, dp(13))
        setPadding(dp(10), dp(7), dp(10), dp(7))
        setOnClickListener { action() }
    }

    private fun refreshStatus() {
        val policy = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminOn = policy.isAdminActive(adminComponent)
        val serviceOn = isAccessibilityServiceEnabled()

        lockStatus.text = "Lock permission: ${if (adminOn) "Enabled" else "Not enabled"}"
        lockStatus.setTextColor(if (adminOn) 0xFF16834A.toInt() else 0xFFB05B18.toInt())
        serviceStatus.text = "Button service: ${if (serviceOn) "Enabled" else "Not enabled"}"
        serviceStatus.setTextColor(if (serviceOn) 0xFF16834A.toInt() else 0xFFB05B18.toInt())
        readyStatus.text = if (adminOn && serviceOn) "You're all set" else "Finish the setup below"
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val expected = ComponentName(this, VolumeAccessibilityService::class.java).flattenToString()
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    private fun requestDeviceAdmin() {
        val policy = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (policy.isAdminActive(adminComponent)) {
            Toast.makeText(this, "Lock permission is already enabled", Toast.LENGTH_SHORT).show()
            return
        }
        val request = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "VolumeTap needs permission only to lock your screen after a double-press of Volume Down."
            )
        }
        startActivity(request)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(this, "Choose VolumeTap button gestures and turn it on", Toast.LENGTH_LONG).show()
    }

    private fun testLock() {
        val policy = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!policy.isAdminActive(adminComponent)) {
            Toast.makeText(this, "Enable lock permission first", Toast.LENGTH_LONG).show()
            requestDeviceAdmin()
            return
        }
        try {
            policy.lockNow()
        } catch (error: SecurityException) {
            Toast.makeText(this, "Lock permission was not accepted. Enable it again.", Toast.LENGTH_LONG).show()
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun rounded(color: Int, radiusPx: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusPx.toFloat()
        }

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
