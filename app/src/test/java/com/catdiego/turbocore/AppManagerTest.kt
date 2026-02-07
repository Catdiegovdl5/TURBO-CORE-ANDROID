package com.catdiego.turbocore

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.junit.MockitoJUnitRunner
import kotlin.system.measureTimeMillis

@RunWith(MockitoJUnitRunner::class)
class AppManagerTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var pm: PackageManager

    @Before
    fun setUp() {
        whenever(context.packageManager).thenReturn(pm)
    }

    @Test
    fun testGetFilteredAppsOptimization() = runBlocking {
        // Create 100 mock apps
        val mockApps = (1..100).map { i ->
            val app = mock(ApplicationInfo::class.java)
            // Fields are 0 by default, so flags & FLAG_SYSTEM == 0 (non-system)
            // No need to stub loadLabel because we expect it NOT to be called.
            app
        }

        // Mock getInstalledApplications
        whenever(pm.getInstalledApplications(anyInt())).thenReturn(mockApps)

        val time = measureTimeMillis {
            // Call the suspend function
            val result = AppManager.getFilteredApps(context)

            assertEquals(100, result.size)
            // Verify we got ApplicationInfo objects back
            assertTrue(result[0] is ApplicationInfo)
        }

        println("Optimized execution time: ${time}ms")

        // Assert that it is fast (< 100ms)
        assertTrue("Execution should be fast (<100ms)", time < 100)

        // Verify loadLabel was NEVER called on any app
        for (app in mockApps) {
            verify(app, never()).loadLabel(any())
        }
    }
}
