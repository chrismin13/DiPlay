package com.shilapi.xcertplay.airplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarPlayDisplayScaleTest {
    @Test
    fun scalesHandshakeDisplayAtSupportedSteps() {
        val native = AirPlayDisplayConfig(widthPixels = 1080, heightPixels = 2160)

        val half = CarPlayDisplayScale.apply(native, 5)

        assertEquals(540, half.widthPixels)
        assertEquals(1080, half.heightPixels)
        assertEquals("0.5x", CarPlayDisplayScale.label(5))
    }

    @Test
    fun clampsScaleToTheUiRange() {
        assertEquals(3, CarPlayDisplayScale.sanitize(0))
        assertEquals(10, CarPlayDisplayScale.sanitize(20))
    }

    @Test
    fun alignsScaledDisplayDimensionsToEvenPixels() {
        val native = AirPlayDisplayConfig(widthPixels = 1920, heightPixels = 978)

        val scaled = CarPlayDisplayScale.apply(native, 7)

        assertEquals(1344, scaled.widthPixels)
        assertEquals(686, scaled.heightPixels)
    }
    @Test fun arbitraryPercentagesPreserveEvenDimensionsAndPhysicalSize() {
        val native = AirPlayDisplayConfig(widthPixels = 1920, heightPixels = 978)
        val scaled = CarPlayDisplayScale.applyPercent(native, 55)
        assertEquals(1056, scaled.widthPixels)
        assertEquals(538, scaled.heightPixels)
        assertEquals(native.widthPhysicalMm, scaled.widthPhysicalMm)
        assertEquals(native.heightPhysicalMm, scaled.heightPhysicalMm)
        assertEquals(CarPlayDisplayScale.apply(native, 6), CarPlayDisplayScale.applyPercent(native, 60))
    }

    @Test fun customPercentagesReachTheConfiguredMaximum() {
        val native = AirPlayDisplayConfig(widthPixels = 1920, heightPixels = 978)

        val widened = CarPlayDisplayScale.applyPercent(native, CarPlayDisplayScale.MAX_PERCENT)

        assertEquals(160, CarPlayDisplayScale.MAX_PERCENT)
        assertEquals(3072, widened.widthPixels)
        assertEquals(1566, widened.heightPixels)
        assertEquals(native.widthPhysicalMm, widened.widthPhysicalMm)
        assertEquals(widened, CarPlayDisplayScale.applyPercent(native, CarPlayDisplayScale.MAX_PERCENT + 60))
    }

    @Test fun fittingPercentStepsDownToTheDecoderLimit() {
        // Nexus 7 (2013): OMX.qcom.video.decoder.avc accepts at most 1920x1088.
        val native = AirPlayDisplayConfig(widthPixels = 1920, heightPixels = 1200)
        val percent = CarPlayDisplayScale.fittingPercent(native, 100) { it.widthPixels <= 1920 && it.heightPixels <= 1088 }
        assertEquals(90, percent)
        assertEquals(1080, CarPlayDisplayScale.applyPercent(native, 90).heightPixels)
    }

    @Test fun fittingPercentKeepsASupportedCanvasAndReportsNoFit() {
        val native = AirPlayDisplayConfig(widthPixels = 1280, heightPixels = 720)
        assertEquals(70, CarPlayDisplayScale.fittingPercent(native, 70) { true })
        assertNull(CarPlayDisplayScale.fittingPercent(native, 100) { false })
    }

}
