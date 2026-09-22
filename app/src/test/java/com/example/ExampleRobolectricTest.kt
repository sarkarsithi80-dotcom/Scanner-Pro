package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CornerPoints
import com.example.engine.EncryptionEngine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Scanner Pro", appName)
  }

  @Test
  fun `corner points json serialization and deserialization`() {
    val original = CornerPoints.default()
    val json = original.toJson()
    val restored = CornerPoints.fromJson(json)

    assertEquals(original.topLeft.x, restored.topLeft.x, 0.001f)
    assertEquals(original.topLeft.y, restored.topLeft.y, 0.001f)
    assertEquals(original.bottomRight.x, restored.bottomRight.x, 0.001f)
    assertEquals(original.bottomRight.y, restored.bottomRight.y, 0.001f)
  }

  @Test
  fun `encryption engine aes gcm roundtrip`() {
    val originalData = "Confidential Client Tax Statement & Signature 2026".toByteArray(Charsets.UTF_8)
    val passcode = "9876"

    val encrypted = EncryptionEngine.encryptBytes(originalData, passcode)
    assertNotNull(encrypted)

    val decrypted = EncryptionEngine.decryptBytes(encrypted, passcode)
    assertArrayEquals(originalData, decrypted)
  }
}

