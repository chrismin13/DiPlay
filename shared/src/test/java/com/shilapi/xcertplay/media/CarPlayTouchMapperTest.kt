package com.shilapi.xcertplay.media

import android.view.MotionEvent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE)
class CarPlayTouchMapperTest {
    private val content = CarPlayVideoLayout.fit(1920, 990, 1920, 942)

    @Test fun contentCornersMapToCanvasCorners() {
        contact(MotionEvent.ACTION_DOWN, content.left, content.top).let {
            assertEquals(0.0, it.x, 1e-6)
            assertEquals(0.0, it.y, 1e-6)
            assertTrue(it.down)
        }
        contact(MotionEvent.ACTION_UP, content.left + content.width, content.top + content.height).let {
            assertEquals(1.0, it.x, 1e-6)
            assertEquals(1.0, it.y, 1e-6)
            assertFalse(it.down)
        }
    }

    @Test fun centreRemainsCentred() {
        contact(MotionEvent.ACTION_MOVE, 960f, 471f).let {
            assertEquals(0.5, it.x, 1e-6)
            assertEquals(0.5, it.y, 1e-6)
        }
    }

    @Test fun dragIntoABarClampsToTheCanvasEdge() {
        assertEquals(0.0, contact(MotionEvent.ACTION_MOVE, 0f, 471f).x, 1e-6)
        assertEquals(1.0, contact(MotionEvent.ACTION_MOVE, 1920f, 471f).x, 1e-6)
    }

    @Test fun cancelInABarReleasesTheContact() {
        assertFalse(contact(MotionEvent.ACTION_CANCEL, 0f, 471f).down)
    }

    @Test fun fullViewOverloadKeepsExistingCoordinates() {
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 480f, 247.5f, 0)
        try {
            val contact = CarPlayTouchMapper.contacts(event, 1920, 990).single()
            assertEquals(0.25, contact.x, 1e-6)
            assertEquals(0.25, contact.y, 1e-6)
        } finally {
            event.recycle()
        }
    }

    @Test fun theSecondFingerKeepsItsIdentityAfterTheFirstLifts() {
        // A pinch whose first finger (id 0) has lifted: the second finger (id 1) is now index 0.
        val remaining = pointers(MotionEvent.ACTION_MOVE, 1 to (960f to 471f))
        val contact = CarPlayTouchMapper.contacts(remaining, content).single()
        assertEquals(1, contact.id)
        assertTrue(contact.down)

        val report = com.shilapi.xcertplay.airplay.AirPlayHid.touchReport(listOf(contact.copy(x = 10.0, y = 20.0)))
        assertEquals("slot 0 is released", 0, report[1].toInt())
        assertEquals("slot 1 holds the finger", 1, report[7].toInt())
        assertEquals(10, report[8].toInt())
        assertEquals(20, report[10].toInt())
    }

    @Test fun aThirdFingerIsNotSent() {
        val event = pointers(MotionEvent.ACTION_MOVE, 0 to (100f to 100f), 1 to (200f to 200f), 2 to (300f to 300f))
        assertEquals(listOf(0, 1), CarPlayTouchMapper.contacts(event, content).map { it.id })
    }

    private fun pointers(action: Int, vararg fingers: Pair<Int, Pair<Float, Float>>): MotionEvent {
        val properties = fingers.map { (id, _) -> MotionEvent.PointerProperties().apply { this.id = id } }.toTypedArray()
        val coords = fingers.map { (_, xy) -> MotionEvent.PointerCoords().apply { x = xy.first; y = xy.second } }.toTypedArray()
        return MotionEvent.obtain(0, 0, action, fingers.size, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
    }

    private fun contact(action: Int, x: Float, y: Float) = MotionEvent.obtain(0, 0, action, x, y, 0).let {
        try {
            CarPlayTouchMapper.contacts(it, content).single()
        } finally {
            it.recycle()
        }
    }
}
