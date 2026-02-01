package com.catdiego.turbocore

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class AppInfo(
    val name: String,
    val packageName: String
    // Icon removed for simplified boot
)

object AppManager {
    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return apps.filter {
            (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        }.map {
            AppInfo(
                name = it.loadLabel(pm).toString(),
                packageName = it.packageName
            )
        }.sortedBy { it.name }
    }
}
