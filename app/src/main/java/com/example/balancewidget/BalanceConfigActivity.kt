package com.example.balancewidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Optional configuration activity for the balance widget.
 *
 * Launched when:
 *  - The user adds the widget to the launcher (first-time configuration).
 *  - The user taps the balance value label on the widget.
 *
 * Provides a SeekBar for fine-grained balance control with real-time preview.
 */
class BalanceConfigActivity : AppCompatActivity() {

    companion object {
        fun createIntent(context: Context, appWidgetId: Int): Intent =
            Intent(context, BalanceConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                // Ensure unique task history per widget instance
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
    }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private lateinit var seekBar: SeekBar
    private lateinit var tvValue: TextView
    private lateinit var btnApply: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the user cancels, the widget should not be created (relevant for first-time config)
        setResult(RESULT_CANCELED)

        setContentView(R.layout.activity_balance_config)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        seekBar   = findViewById(R.id.sb_balance)
        tvValue   = findViewById(R.id.tv_current_value)
        btnApply  = findViewById(R.id.btn_apply)
        btnCancel = findViewById(R.id.btn_cancel)

        // SeekBar 0–20, where 10 = center
        val range = AudioBalanceManager.MAX_STEPS - AudioBalanceManager.MIN_STEPS  // 20
        seekBar.max = range
        val currentSteps = AudioBalanceManager.getSavedSteps(this)
        seekBar.progress = currentSteps - AudioBalanceManager.MIN_STEPS  // shift to 0-based
        updateValueLabel(currentSteps)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val steps = progress + AudioBalanceManager.MIN_STEPS
                updateValueLabel(steps)
                if (fromUser) {
                    // Live preview while dragging
                    AudioBalanceManager.setBalance(this@BalanceConfigActivity, steps)
                    BalanceWidget.updateAllWidgets(this@BalanceConfigActivity)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        btnApply.setOnClickListener {
            val steps = seekBar.progress + AudioBalanceManager.MIN_STEPS
            AudioBalanceManager.setBalance(this, steps)
            BalanceWidget.updateAllWidgets(this)
            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }

        btnCancel.setOnClickListener {
            // Restore the original balance before preview changes
            AudioBalanceManager.setBalance(this, currentSteps)
            BalanceWidget.updateAllWidgets(this)
            finish()
        }
    }

    private fun updateValueLabel(steps: Int) {
        tvValue.text = AudioBalanceManager.formatLabel(steps)
    }
}
