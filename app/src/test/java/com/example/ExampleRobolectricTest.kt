package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.NetworkStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BGFCL Dashboard", appName)
    }

    @Test
    fun `verify bgfcl target url`() {
        assertEquals("http://180.211.173.178:3000", MainActivity.BGFCL_URL)
    }

    @Test
    fun `verify network status enum`() {
        assertNotNull(NetworkStatus.CONNECTED)
        assertNotNull(NetworkStatus.DISCONNECTED)
    }
}
