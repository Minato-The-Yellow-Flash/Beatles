package com.beatles.player.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun testIsNewerVersion() {
        // Standard semver tests
        assertTrue(AppUpdateManager.isNewerVersion("1.3", "1.2"))
        assertTrue(AppUpdateManager.isNewerVersion("v1.3", "1.2"))
        assertTrue(AppUpdateManager.isNewerVersion("1.3.0", "1.2.0"))
        assertTrue(AppUpdateManager.isNewerVersion("1.2.1", "1.2"))
        assertTrue(AppUpdateManager.isNewerVersion("2.0", "1.9.9"))

        // Equal or older tests
        assertFalse(AppUpdateManager.isNewerVersion("1.2", "1.2"))
        assertFalse(AppUpdateManager.isNewerVersion("1.1", "1.2"))
        assertFalse(AppUpdateManager.isNewerVersion("1.2.0", "1.2"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0", "1.3"))
    }

    @Test
    fun testLiveCheckForUpdatesFindsRelease1_3() = runBlocking {
        val update = AppUpdateManager.checkForUpdates("1.2")
        println("Discovered live update: $update")
        assertNotNull("Update must be detected when local is 1.2 and GitHub has releases newer than 1.2", update)
        assertTrue("Live version should be newer than 1.2", AppUpdateManager.isNewerVersion(update?.versionName ?: "", "1.2"))
        assertTrue("APK URL should point to Beatles-App.apk", update?.apkUrl?.endsWith(".apk", ignoreCase = true) == true)
    }
}
