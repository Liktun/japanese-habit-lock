package com.liktun.japanesehabitlock.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OemBatteryGuidanceTest {

  private val known = listOf(
    "samsung",
    "xiaomi",
    "redmi",
    "poco",
    "huawei",
    "honor",
    "oppo",
    "realme",
    "vivo",
    "oneplus",
  )

  private fun guidance(manufacturer: String) = OemBatteryGuidance.forManufacturer(manufacturer)

  @Test
  fun `every listed manufacturer returns non-empty steps`() {
    known.forEach { name ->
      assertTrue("$name had no steps", guidance(name).steps.isNotEmpty())
    }
  }

  @Test
  fun `every listed manufacturer has a non-blank label`() {
    known.forEach { name ->
      assertTrue("$name had a blank label", guidance(name).manufacturerLabel.isNotBlank())
    }
  }

  @Test
  fun `no returned step is blank for any known manufacturer`() {
    known.forEach { name ->
      guidance(name).steps.forEach { step ->
        assertTrue("$name had a blank step", step.isNotBlank())
      }
    }
  }

  @Test
  fun `no returned steps list is empty for known or unknown manufacturers`() {
    (known + listOf("", "  ", "Google", "Nothing", "Fairphone", "zzz-unknown")).forEach { name ->
      assertTrue("[$name] had no steps", guidance(name).steps.isNotEmpty())
    }
  }

  @Test
  fun `matching is case-insensitive across upper lower and mixed case`() {
    assertEquals(guidance("samsung"), guidance("SAMSUNG"))
    assertEquals(guidance("samsung"), guidance("Samsung"))
    assertEquals(guidance("samsung"), guidance("sAmSuNg"))
  }

  @Test
  fun `surrounding whitespace does not defeat matching`() {
    assertEquals(guidance("xiaomi"), guidance("  Xiaomi  "))
  }

  @Test
  fun `redmi resolves to the same guidance as xiaomi`() {
    assertEquals(guidance("xiaomi"), guidance("redmi"))
  }

  @Test
  fun `poco resolves to the same guidance as xiaomi`() {
    assertEquals(guidance("xiaomi"), guidance("POCO"))
  }

  @Test
  fun `samsung points at the device care battery component`() {
    assertEquals(
      "com.samsung.android.lool/com.samsung.android.sm.ui.battery.BatteryActivity",
      guidance("Samsung").settingsComponent,
    )
  }

  @Test
  fun `samsung calls out Never sleeping apps`() {
    assertTrue(guidance("Samsung").steps.any { it.contains("Never sleeping apps") })
  }

  @Test
  fun `xiaomi points at the MIUI autostart component`() {
    assertEquals(
      "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",
      guidance("Xiaomi").settingsComponent,
    )
  }

  @Test
  fun `xiaomi covers both autostart and locking the app in recents`() {
    val steps = guidance("Xiaomi").steps
    assertTrue("Autostart missing", steps.any { it.contains("Autostart", ignoreCase = true) })
    assertTrue("Recents lock missing", steps.any { it.contains("Recents", ignoreCase = true) })
  }

  @Test
  fun `every OEM-specific brand ships a component`() {
    known.forEach { name ->
      assertNotNull("$name had no component", guidance(name).settingsComponent)
    }
  }

  @Test
  fun `every OEM component is a parseable flattened package slash class`() {
    known.forEach { name ->
      val component = guidance(name).settingsComponent.orEmpty()
      val parts = component.split('/')
      assertEquals("$name component was not pkg/class: $component", 2, parts.size)
      assertTrue("$name had a blank package", parts[0].isNotBlank())
      assertTrue("$name had a blank class", parts[1].isNotBlank())
    }
  }

  @Test
  fun `an unknown manufacturer returns the generic fallback with a non-null action`() {
    val fallback = guidance("SomeBrandWeHaveNeverHeardOf")
    assertEquals(
      "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS",
      fallback.settingsIntentAction,
    )
    assertTrue(fallback.steps.isNotEmpty())
  }

  @Test
  fun `the generic fallback has no component`() {
    assertNull(guidance("SomeBrandWeHaveNeverHeardOf").settingsComponent)
  }

  @Test
  fun `Google Pixel gets the generic battery-optimization guidance`() {
    val google = guidance("Google")
    assertNull(google.settingsComponent)
    assertEquals(OemBatteryGuidance.GENERIC_ACTION, google.settingsIntentAction)
  }

  @Test
  fun `an unknown manufacturer is echoed back in the label`() {
    assertEquals("Nothing", guidance("Nothing").manufacturerLabel)
  }

  @Test
  fun `a blank manufacturer still yields usable generic guidance`() {
    val blank = guidance("")
    assertTrue(blank.steps.isNotEmpty())
    assertEquals(OemBatteryGuidance.GENERIC_ACTION, blank.settingsIntentAction)
    assertTrue(blank.manufacturerLabel.isNotBlank())
  }

  @Test
  fun `each brand has genuinely distinct instructions rather than a copied stub`() {
    // Samsung, MIUI and ColorOS need different screens; identical step lists would mean
    // someone pasted a placeholder and the user would be sent to a screen that is not
    // there on their phone.
    val samsung = guidance("samsung").steps
    val xiaomi = guidance("xiaomi").steps
    val oppo = guidance("oppo").steps
    assertTrue(samsung != xiaomi)
    assertTrue(xiaomi != oppo)
    assertTrue(samsung != oppo)
  }

  @Test
  fun `steps name concrete screens rather than vague advice`() {
    known.forEach { name ->
      assertTrue(
        "$name never mentions Settings or a manager app",
        guidance(name).steps.any {
          it.contains("Settings", ignoreCase = true) ||
            it.contains("Manager", ignoreCase = true) ||
            it.contains("Security", ignoreCase = true)
        },
      )
    }
  }
}
