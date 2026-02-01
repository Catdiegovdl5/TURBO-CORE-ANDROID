package com.catdiego.turbocore.util

import java.io.File
import java.lang.reflect.Method

object ShellEngine {

    private const val SHIZUKU_CLASS = "rikka.shizuku.Shizuku"

    /**
     * Checks if Shizuku service is available using reflection.
     */
    fun isAvailable(): Boolean {
        return try {
            val clazz = Class.forName(SHIZUKU_CLASS)
            val method: Method = clazz.getMethod("pingBinder")
            method.invoke(null) as Boolean
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Executes a command using Shizuku via Reflection.
     * Returns the Process object or null if execution failed.
     *
     * @param command The command and arguments to execute.
     * @param env Environment variables (optional).
     * @param dir Working directory (optional).
     */
    fun newProcess(command: Array<String>, env: Array<String>? = null, dir: File? = null): Process? {
        return try {
            val clazz = Class.forName(SHIZUKU_CLASS)
            // signature: public static Process newProcess(@NonNull String[] cmd, @Nullable String[] env, @Nullable File dir)
            val method = clazz.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                File::class.java
            )
            method.invoke(null, command, env, dir) as? Process
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
}
