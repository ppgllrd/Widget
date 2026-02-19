package com.example.balancewidget

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Main launcher activity.
 *
 * Shows the current balance and provides a SeekBar to adjust it directly from within the app,
 * in addition to the home-screen widget.
 *
 * Also explains that the app requires MODIFY_AUDIO_SETTINGS and, on privileged builds,
 * MODIFY_AUDIO_ROUTING to apply the system-wide balance.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var seekBar: SeekBar
    private lateinit var tvValue: TextView
    private lateinit var tvInfo: TextView
    private lateinit var btnCenter: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        seekBar  = findViewById(R.id.sb_main_balance)
        tvValue  = findViewById(R.id.tv_main_balance_value)
        tvInfo   = findViewById(R.id.tv_permission_info)
        btnCenter = findViewById(R.id.btn_main_center)

        // SeekBar 0–20, 10 = center
        val range = AudioBalanceManager.MAX_STEPS - AudioBalanceManager.MIN_STEPS
        seekBar.max = range
        val currentSteps = AudioBalanceManager.getSavedSteps(this)
        seekBar.progress = currentSteps - AudioBalanceManager.MIN_STEPS
        updateValueLabel(currentSteps)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val steps = progress + AudioBalanceManager.MIN_STEPS
                updateValueLabel(steps)
                if (fromUser) {
                    AudioBalanceManager.setBalance(this@MainActivity, steps)
                    BalanceWidget.updateAllWidgets(this@MainActivity)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        btnCenter.setOnClickListener {
            val centerProgress = AudioBalanceManager.CENTER_STEPS - AudioBalanceManager.MIN_STEPS
            seekBar.progress = centerProgress
            AudioBalanceManager.setBalance(this, AudioBalanceManager.CENTER_STEPS)
            BalanceWidget.updateAllWidgets(this)
            updateValueLabel(AudioBalanceManager.CENTER_STEPS)
        }

        checkPermissionsAndUpdateInfo()
    }

    override fun onResume() {
        super.onResume()
        // Refresh UI in case balance was changed via the widget
        val steps = AudioBalanceManager.getSavedSteps(this)
        seekBar.progress = steps - AudioBalanceManager.MIN_STEPS
        updateValueLabel(steps)
    }

    private fun updateValueLabel(steps: Int) {
        tvValue.text = AudioBalanceManager.formatLabel(steps)
    }

    private fun checkPermissionsAndUpdateInfo() {
        val hasModifyAudio = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.MODIFY_AUDIO_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

        val hasRouting = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.MODIFY_AUDIO_ROUTING
        ) == PackageManager.PERMISSION_GRANTED

        tvInfo.text = getString(
            R.string.permission_status,
            if (hasModifyAudio) getString(R.string.granted) else getString(R.string.not_granted),
            if (hasRouting) getString(R.string.granted) else getString(R.string.not_granted)
        )
        // MODIFY_AUDIO_SETTINGS is a normal permission and is auto-granted at install time;
        // no runtime request is needed or possible.
        // MODIFY_AUDIO_ROUTING is a signature/privileged permission; it cannot be granted at
        // runtime to user-installed apps — the status is informational only.
    }
}
