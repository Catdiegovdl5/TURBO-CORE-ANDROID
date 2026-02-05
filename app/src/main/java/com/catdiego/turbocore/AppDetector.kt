package com.catdiego.turbocore

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.SortedMap
import java.util.TreeMap

object AppDetector {
    fun getForegroundApp(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        // Query last 2 minutes roughly
        val appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 120000, time)

        if (appList != null && appList.isNotEmpty()) {
            val mySortedMap: SortedMap<Long, android.app.usage.UsageStats> = TreeMap()
            for (usageStats in appList) {
                mySortedMap[usageStats.lastTimeUsed] = usageStats
            }
            if (mySortedMap.isNotEmpty()) {
                return mySortedMap[mySortedMap.lastKey()]?.packageName ?: "com.android.launcher"
            }
        }
        return null
    }
}
