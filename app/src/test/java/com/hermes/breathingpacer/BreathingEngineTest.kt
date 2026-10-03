package com.nuks.slowlight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathingEngineTest {
    private val eightMinute = BreathingConfiguration(sessionDurationSeconds = 8 * 60.0)
    private val twentyMinute = BreathingConfiguration(sessionDurationSeconds = 20 * 60.0)

    @Test fun startsAtElevenBreathsPerMinute() {
        assertEquals(11.0, BreathingEngine.snapshot(0.0, eightMinute).currentBpm, 0.0001)
    }

    @Test fun reachesSixBreathsPerMinuteAfterRamp() {
        assertEquals(6.0, BreathingEngine.snapshot(120.0, eightMinute).currentBpm, 0.0001)
        assertEquals(6.0, BreathingEngine.snapshot(300.0, eightMinute).currentBpm, 0.0001)
    }

    @Test fun rampIsLinearForOneHundredTwentySeconds() {
        assertEquals(8.5, BreathingEngine.snapshot(60.0, eightMinute).currentBpm, 0.0001)
    }

    @Test fun targetCycleIsFourSecondInhaleAndSixSecondExhale() {
        val snap = BreathingEngine.snapshot(120.0, eightMinute)
        assertEquals(10.0, snap.cycleDurationSeconds, 0.0001)
        assertEquals(4.0, snap.inhaleDurationSeconds, 0.0001)
        assertEquals(6.0, snap.exhaleDurationSeconds, 0.0001)
    }

    @Test fun calculatesInhaleAndExhalePhasesAtTargetRate() {
        assertEquals(BreathingPhase.INHALE, BreathingEngine.snapshot(120.5, eightMinute).phase)
        assertEquals(BreathingPhase.EXHALE, BreathingEngine.snapshot(124.5, eightMinute).phase)
    }

    @Test fun completesEightAndTwentyMinuteSessions() {
        assertFalse(BreathingEngine.snapshot(479.0, eightMinute).isComplete)
        assertTrue(BreathingEngine.snapshot(480.0, eightMinute).isComplete)
        assertFalse(BreathingEngine.snapshot(1199.0, twentyMinute).isComplete)
        assertTrue(BreathingEngine.snapshot(1200.0, twentyMinute).isComplete)
    }

    @Test fun rejectsInvalidConfigurationValues() {
        val invalid = BreathingConfiguration(startBpm = 0.0)
        try {
            BreathingEngine.snapshot(0.0, invalid)
            throw AssertionError("Expected invalid configuration to fail")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun pauseResumeTimingModelCanUseAccumulatedElapsedTime() {
        val beforePause = 42.0
        val afterResumeActiveOnly = beforePause + 8.0
        assertEquals(BreathingEngine.snapshot(afterResumeActiveOnly, eightMinute).phase, BreathingEngine.snapshot(50.0, eightMinute).phase)
    }
}
