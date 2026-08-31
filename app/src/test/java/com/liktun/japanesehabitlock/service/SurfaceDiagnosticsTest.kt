package com.liktun.japanesehabitlock.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** An in-memory stand-in for SharedPreferences, so the contract is testable off-device. */
private class FakeStore : SurfaceDiagnostics.Store {
  var enabled = false
  var captures: Map<String, Set<String>> = emptyMap()
  var verdicts: Map<String, String> = emptyMap()

  override fun loadEnabled() = enabled

  override fun saveEnabled(value: Boolean) {
    enabled = value
  }

  override fun loadCaptures() = captures

  override fun saveCaptures(captures: Map<String, Set<String>>) {
    this.captures = captures
  }

  override fun loadVerdicts() = verdicts

  override fun saveVerdicts(verdicts: Map<String, String>) {
    this.verdicts = verdicts
  }
}

private const val IG = "com.instagram.android"

class SurfaceDiagnosticsTest {

  private lateinit var store: FakeStore

  @Before
  fun setUp() {
    store = FakeStore()
    SurfaceDiagnostics.attach(store)
    SurfaceDiagnostics.setEnabled(false)
    SurfaceDiagnostics.clear()
  }

  @Test
  fun `nothing is recorded while disabled`() {
    SurfaceDiagnostics.record(IG, setOf("$IG:id/clips_viewer"), "allowed")
    assertTrue(SurfaceDiagnostics.packages().isEmpty())
  }

  @Test
  fun `ids are recorded once enabled`() {
    SurfaceDiagnostics.setEnabled(true)
    SurfaceDiagnostics.record(IG, setOf("$IG:id/clips_viewer"), "allowed")
    assertEquals(listOf(IG), SurfaceDiagnostics.packages())
    assertEquals(listOf("clips_viewer"), SurfaceDiagnostics.idsFor(IG))
  }

  @Test
  fun `the package prefix is stripped so ids match the rule form`() {
    SurfaceDiagnostics.setEnabled(true)
    SurfaceDiagnostics.record(IG, setOf("$IG:id/direct_thread_toggle"), "allowed")
    assertEquals(listOf("direct_thread_toggle"), SurfaceDiagnostics.idsFor(IG))
  }

  @Test
  fun `recording survives the process being killed`() {
    // THE BUG. The workflow is: enable, LEAVE the app, use Instagram, come back. Android
    // routinely kills this process while the user is in Instagram, and the service is
    // then restarted with fresh static state. Before persistence, the flag reverted to
    // false and every captured id was lost, so the report was always empty - exactly the
    // window the tool exists to observe.
    SurfaceDiagnostics.setEnabled(true)
    SurfaceDiagnostics.record(IG, setOf("$IG:id/clips_viewer"), "allowed (no rule matched)")

    // Simulate process death: same persisted store, wiped in-memory state.
    SurfaceDiagnostics.clearInMemoryForTest()
    SurfaceDiagnostics.attach(store)

    assertTrue("recording must still be on after a restart", SurfaceDiagnostics.enabled)
    assertEquals(listOf("clips_viewer"), SurfaceDiagnostics.idsFor(IG))
    assertEquals("allowed (no rule matched)", SurfaceDiagnostics.verdictFor(IG))
  }

  @Test
  fun `turning recording off clears what was captured`() {
    SurfaceDiagnostics.setEnabled(true)
    SurfaceDiagnostics.record(IG, setOf("$IG:id/clips_viewer"), "allowed")
    SurfaceDiagnostics.setEnabled(false)
    assertTrue(SurfaceDiagnostics.packages().isEmpty())
    assertTrue(store.captures.isEmpty())
  }

  @Test
  fun `the empty report distinguishes off from on-but-nothing-seen`() {
    // The user's actual confusion was "I turned it on and nothing happened". The two
    // states must not look identical.
    val off = SurfaceDiagnostics.report()
    assertTrue(off.contains("OFF"))

    SurfaceDiagnostics.setEnabled(true)
    val onEmpty = SurfaceDiagnostics.report()
    assertTrue(onEmpty.contains("ON"))
    assertFalse(onEmpty == off)
  }

  @Test
  fun `the report names the package and its ids`() {
    SurfaceDiagnostics.setEnabled(true)
    SurfaceDiagnostics.record(IG, setOf("$IG:id/clips_viewer"), "allowed (no rule matched)")
    val report = SurfaceDiagnostics.report()
    assertTrue(report.contains(IG))
    assertTrue(report.contains("clips_viewer"))
    assertTrue(report.contains("allowed (no rule matched)"))
  }

  @Test
  fun `an empty id set still records the verdict`() {
    // A screen whose tree could not be read is itself diagnostic information.
    SurfaceDiagnostics.setEnabled(true)
    SurfaceDiagnostics.record(IG, emptySet(), "allowed (no rule matched)")
    assertEquals("allowed (no rule matched)", SurfaceDiagnostics.verdictFor(IG))
  }

  @Test
  fun `a verdict with no ids reports the null-id failure specifically`() {
    // THE SHIPPED BUG, as seen from the user's report: every app showed a verdict and
    // not one view id. That is FLAG_REPORT_VIEW_IDS being absent - the service reads the
    // tree fine but Android returns null for every getViewIdResourceName(). A generic
    // "nothing captured" message sent the user looking in the wrong place.
    SurfaceDiagnostics.setEnabled(true)
    SurfaceDiagnostics.record("bunpro.jp.bunpro_srs", emptySet(), "allowed (no rule matched)")
    SurfaceDiagnostics.record(IG, emptySet(), "allowed (no rule matched)")

    val report = SurfaceDiagnostics.report()
    assertTrue(report.contains("NO view ids were readable"))
    assertTrue(report.contains("bunpro.jp.bunpro_srs"))
    assertTrue(report.contains(IG))
    // And it must tell the user what to actually do about it.
    assertTrue(report.contains("Accessibility"))
  }

  @Test
  fun `duplicate ids are not written repeatedly`() {
    SurfaceDiagnostics.setEnabled(true)
    SurfaceDiagnostics.record(IG, setOf("$IG:id/clips_viewer"), "allowed")
    val afterFirst = store.captures
    SurfaceDiagnostics.record(IG, setOf("$IG:id/clips_viewer"), "allowed")
    // Same content: the second call must not have grown anything.
    assertEquals(afterFirst[IG], store.captures[IG])
  }
}
