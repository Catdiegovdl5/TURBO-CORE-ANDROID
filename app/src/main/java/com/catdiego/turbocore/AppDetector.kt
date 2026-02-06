package com.catdiego.turbocore

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import java.util.TreeMap

object AppDetector {
    fun getForegroundApp(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        // Query last 5 minutes
        val appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60 * 5, time)

        if (appList != null && appList.isNotEmpty()) {
            val sortedMap = TreeMap<Long, UsageStats>()
            for (usageStats in appList) {
                sortedMap[usageStats.lastTimeUsed] = usageStats
            }

            if (sortedMap.isNotEmpty()) {
                // Itera do mais recente para o mais antigo
                val keys = sortedMap.descendingKeySet()
                for (key in keys) {
                    val pkg = sortedMap[key]?.packageName
                    // Pula o próprio Turbo Core e o Pixel Launcher/OneUI Home
                    if (pkg != null && pkg != context.packageName && !isLauncher(context, pkg)) {
                        return pkg
                    }
                }
            }
        }
        return null
    }

    private fun isLauncher(context: Context, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = context.packageManager.resolveActivity(intent, 0)
        return res?.activityInfo?.packageName == packageName
    }
}
