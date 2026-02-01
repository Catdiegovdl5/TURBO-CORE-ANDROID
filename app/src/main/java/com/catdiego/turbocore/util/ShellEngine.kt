package com.catdiego.turbocore.util

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.File
import java.lang.reflect.Method

object ShellEngine {

    /**
     * Checks if Shizuku service is available.
     * Uses direct API.
     */
    fun isAvailable(): Boolean {
        // Shizuku.pingBinder() returns true if the service is running and binder is alive
        return Shizuku.pingBinder()
    }

    /**
     * Checks if the app has permission to use Shizuku.
     * Uses direct API.
     */
    fun checkPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) {
                // Pre-v11 Shizuku (adb) doesn't require explicit runtime permission in the same way,
                // but typically checkSelfPermission is enough.
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Executes a command using Shizuku.
     *
     * Note: This method uses Reflection because `Shizuku.newProcess` is currently inaccessible
     * (marked private/hidden) in the `dev.rikka.shizuku:api` library version used, preventing
     * direct Kotlin access.
     *
     * @param command The command and arguments to execute.
     * @param env Environment variables (optional).
     * @param dir Working directory (optional).
     */
    fun newProcess(command: Array<String>, env: Array<String>? = null, dir: File? = null): Process? {
        return try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val method: Method = clazz.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            // Pass dir as String path
            method.invoke(null, command, env, dir?.absolutePath) as? Process
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Helper to run a simple command string using "sh -c".
     */
    fun runCommand(command: String): Process? {
        return newProcess(arrayOf("sh", "-c", command))
    }

    /**
     * Alias for runCommand to match specific request requirement.
     */
    fun runShizukuCommand(command: String): Process? {
        return runCommand(command)
    }
}
