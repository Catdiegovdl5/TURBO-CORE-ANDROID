package com.catdiego.turbocore

import org.junit.Test
import java.lang.reflect.Method
import kotlin.system.measureNanoTime

class ReflectionBenchmark {

    class TestTarget {
        fun newProcess(cmd: Array<String>, env: Array<String>, dir: String): Process? {
            return null
        }
    }

    @Test
    fun benchmarkReflection() {
        val iterations = 1_000_000
        val targetClass = TestTarget::class.java

        // Warmup
        repeat(1000) {
            val method = targetClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true
        }

        // Measure Uncached
        val uncachedTime = measureNanoTime {
            repeat(iterations) {
                val method = targetClass.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java, Array<String>::class.java, String::class.java
                )
                method.isAccessible = true
            }
        }

        // Measure Cached
        val cachedMethod = targetClass.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java, Array<String>::class.java, String::class.java
        )
        cachedMethod.isAccessible = true

        val cachedTime = measureNanoTime {
            repeat(iterations) {
                // Accessing the cached method
                val m = cachedMethod
                // Prevent dead code elimination
                if (m.name.length == 0) throw RuntimeException("Should not happen")
            }
        }

        println("Benchmarking Reflection Performance (${iterations} iterations)")
        println("------------------------------------------------------------")
        println("Uncached time: ${uncachedTime / 1_000_000} ms")
        println("Cached time:   ${cachedTime / 1_000_000} ms")
        val improvement = if (cachedTime > 0) uncachedTime.toDouble() / cachedTime.toDouble() else Double.POSITIVE_INFINITY
        println("Improvement:   ${"%.2f".format(improvement)}x faster")
    }
}
