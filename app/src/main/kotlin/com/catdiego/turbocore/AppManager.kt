package com.catdiego.turbocore

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: ImageBitmap?
)

object AppManager {
    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        // Retrieve third-party apps
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val appList = mutableListOf<AppInfo>()

        for (app in apps) {
            // Filter: Non-system apps or updated system apps
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                try {
                    val name = pm.getApplicationLabel(app).toString()
                    val iconDrawable = pm.getApplicationIcon(app)
                    val iconBitmap = drawableToBitmap(iconDrawable).asImageBitmap()
                    appList.add(AppInfo(name, app.packageName, iconBitmap))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return appList.sortedBy { it.name }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bitmap = Bitmap.createBitmap(
            if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1,
            if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
