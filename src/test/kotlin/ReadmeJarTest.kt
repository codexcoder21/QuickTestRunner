package org.example

import kotlin.test.*
import java.io.File
import java.util.jar.JarFile

class ReadmeJarTest {
    @Test
    fun readmeIncludedInJar() {
        val jar = File("build/libs/QuickTestRunner-1.0-SNAPSHOT-all.jar")
        assertTrue(jar.exists(), "Fat jar should be built")
        JarFile(jar).use { jf ->
            assertNotNull(jf.getEntry("README.md"), "README.md should be in the jar")
        }
    }
}

