package com.example.balancewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Widget provider for the audio balance widget.
 *
 * The widget shows:
 *  - Current balance label (e.g. "L30%", "C", "R50%")
 *  - A simple visual bar with a movable thumb
 *  - ◀ button  — move balance one step to the left
 *  - ● button  — reset balance to center
 *  - ▶ button  — move balance one step to the right
 *
 * Tapping the balance display opens [BalanceConfigActivity] for fine-grained SeekBar control.
 */
class BalanceWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_LEFT   = "com.example.balancewidget.ACTION_BALANCE_LEFT"
        const val ACTION_CENTER = "com.example.balancewidget.ACTION_BALANCE_CENTER"
        const val ACTION_RIGHT  = "com.example.balancewidget.ACTION_BALANCE_RIGHT"

        /** Rebuild all widget instances after a balance change. */
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, BalanceWidget::class.java)
            )
            val intent = Intent(context, BalanceWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
            val steps = AudioBalanceManager.getSavedSteps(context)
            val views = RemoteViews(context.packageName, R.layout.widget_balance)

            // Balance label
            views.setTextViewText(R.id.tv_balance_value, AudioBalanceManager.formatLabel(steps))

            // Progress bar thumb position (0–100 scale, 50 = center)
            val progress = ((steps - AudioBalanceManager.MIN_STEPS).toFloat() /
                    (AudioBalanceManager.MAX_STEPS - AudioBalanceManager.MIN_STEPS) * 100).toInt()
            views.setProgressBar(R.id.pb_balance, 100, progress, false)

            // Left step button
            val leftIntent = Intent(context, BalanceWidget::class.java).apply { action = ACTION_LEFT }
            val leftPi = PendingIntent.getBroadcast(
                context, appWidgetId * 10 + 1, leftIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_left, leftPi)

            // Center button
            val centerIntent = Intent(context, BalanceWidget::class.java).apply { action = ACTION_CENTER }
            val centerPi = PendingIntent.getBroadcast(
                context, appWidgetId * 10 + 2, centerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_center, centerPi)

            // Right step button
            val rightIntent = Intent(context, BalanceWidget::class.java).apply { action = ACTION_RIGHT }
            val rightPi = PendingIntent.getBroadcast(
                context, appWidgetId * 10 + 3, rightIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_right, rightPi)

            // Open config activity on tapping the display area
            val configIntent = BalanceConfigActivity.createIntent(context, appWidgetId)
            val configPi = PendingIntent.getActivity(
                context, appWidgetId, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tv_balance_value, configPi)

            return views
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, id))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val steps = AudioBalanceManager.getSavedSteps(context)
        when (intent.action) {
            ACTION_LEFT   -> AudioBalanceManager.setBalance(context, steps - 1)
            ACTION_RIGHT  -> AudioBalanceManager.setBalance(context, steps + 1)
            ACTION_CENTER -> AudioBalanceManager.setBalance(context, AudioBalanceManager.CENTER_STEPS)
            else          -> return
        }
        updateAllWidgets(context)
    }

    override fun onEnabled(context: Context) {
        // Apply the persisted balance when the first widget instance is added
        AudioBalanceManager.setBalance(context, AudioBalanceManager.getSavedSteps(context))
    }

    override fun onDisabled(context: Context) {
        // Restore center balance when the last widget instance is removed
        AudioBalanceManager.setBalance(context, AudioBalanceManager.CENTER_STEPS)
    }
}
