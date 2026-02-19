package com.example.balancewidget

import android.content.Context
import android.media.AudioManager
import android.util.Log

/**
 * Manages the left/right audio balance for the system.
 *
 * Balance is represented as a float in [-1.0, +1.0]:
 *   -1.0  = full left
 *    0.0  = center (default)
 *   +1.0  = full right
 *
 * Internally stored as an integer in [-10, +10] (tenths) for easy widget step handling.
 *
 * The system method AudioManager.setMasterBalance() requires android.permission.MODIFY_AUDIO_ROUTING
 * (a privileged/signature permission). On devices where this permission is granted (system apps,
 * some custom ROMs, or via adb grant) the balance will be applied system-wide. On regular user
 * installs the call will fail gracefully and a message is logged.
 */
object AudioBalanceManager {

    private const val TAG = "AudioBalanceManager"
    const val PREFS_NAME = "balance_widget_prefs"
    const val PREF_BALANCE = "balance_steps"

    /** Steps range: -10 (full left) … 0 (center) … +10 (full right). */
    const val MIN_STEPS = -10
    const val MAX_STEPS = 10
    const val CENTER_STEPS = 0

    fun stepsToFloat(steps: Int): Float = steps / 10f

    /** Read the persisted balance (in steps) from SharedPreferences. */
    fun getSavedSteps(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_BALANCE, CENTER_STEPS)
    }

    /** Persist and apply the given balance steps. */
    fun setBalance(context: Context, steps: Int) {
        val clamped = steps.coerceIn(MIN_STEPS, MAX_STEPS)
        // Persist
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(PREF_BALANCE, clamped)
            .apply()
        // Apply
        applyBalance(context, stepsToFloat(clamped))
    }

    /**
     * Apply [balance] (-1.0 … +1.0) to the audio system.
     *
     * Primary path : AudioManager.setMasterBalance() — API 29+, requires MODIFY_AUDIO_ROUTING.
     * Fallback path: reflection on older/restricted builds.
     */
    private fun applyBalance(context: Context, balance: Float) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Primary: public API (Android 10 / API 29+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                audioManager.setMasterBalance(balance)
                Log.d(TAG, "setMasterBalance($balance) applied via public API")
                return
            } catch (e: SecurityException) {
                Log.w(TAG, "setMasterBalance blocked by SecurityException (needs MODIFY_AUDIO_ROUTING): ${e.message}")
            } catch (e: Exception) {
                Log.w(TAG, "setMasterBalance failed: ${e.message}")
            }
        }

        // Fallback: reflection for older builds or devices that expose the hidden method
        try {
            val method = audioManager.javaClass.getMethod("setMasterBalance", Float::class.javaPrimitiveType)
            method.invoke(audioManager, balance)
            Log.d(TAG, "setMasterBalance($balance) applied via reflection")
        } catch (e: SecurityException) {
            Log.w(TAG, "setMasterBalance (reflection) blocked by SecurityException: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "setMasterBalance not available via reflection: ${e.message}")
        }
    }

    /** Format [steps] as a human-readable label, e.g. "L50%", "C", "R30%". */
    fun formatLabel(steps: Int): String = when {
        steps < 0 -> "L${-steps * 10}%"
        steps > 0 -> "R${steps * 10}%"
        else -> "C"
    }
}
