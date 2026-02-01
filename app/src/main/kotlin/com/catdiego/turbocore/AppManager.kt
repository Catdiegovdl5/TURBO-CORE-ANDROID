package com.catdiego.turbocore

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: ImageBitmap // Optimized for RAM
)

object AppManager {
    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }

        return apps.filter {
            (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        }.map {
            AppInfo(
                name = it.loadLabel(pm).toString(),
                packageName = it.packageName,
                icon = it.loadIcon(pm).toOptimalImageBitmap()
            )
        }.sortedBy { it.name }
    }

    private fun Drawable.toOptimalImageBitmap(): ImageBitmap {
        val targetSize = 128
        val bitmap = if (this is BitmapDrawable && this.bitmap != null) {
            Bitmap.createScaledBitmap(this.bitmap, targetSize, targetSize, true)
        } else {
            val b = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(b)
            this.setBounds(0, 0, canvas.width, canvas.height)
            this.draw(canvas)
            b
        }
        return bitmap.asImageBitmap()
    }
}
