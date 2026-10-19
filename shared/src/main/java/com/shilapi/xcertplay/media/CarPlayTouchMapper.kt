package com.shilapi.xcertplay.media

import android.view.MotionEvent
import com.shilapi.xcertplay.airplay.AirPlayContact

/** Converts Android MotionEvents into normalized CarPlay touch contacts. */
object CarPlayTouchMapper {
    private const val MAX_CONTACTS = 2

    fun contacts(event: MotionEvent, viewWidth: Int, viewHeight: Int): List<AirPlayContact> =
        contacts(event, CarPlayVideoLayout(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat()))

    fun contacts(event: MotionEvent, content: CarPlayVideoLayout): List<AirPlayContact> {
        val width = content.width.coerceAtLeast(1f)
        val height = content.height.coerceAtLeast(1f)
        val action = event.actionMasked
        val liftedIndex = if (action == MotionEvent.ACTION_POINTER_UP) event.actionIndex else -1
        val allUp = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
        val contacts = ArrayList<AirPlayContact>(MAX_CONTACTS)
        for (index in 0 until event.pointerCount) {
            // The pointer id, not the index, names a finger: when the first finger of a pinch lifts, the
            // second becomes index 0, and an index-based contact 0 would jump to it. Android hands out the
            // lowest free id, so two fingers keep 0 and 1; a third finger is not sent.
            val id = event.getPointerId(index)
            if (id >= MAX_CONTACTS) continue
            contacts.add(
                AirPlayContact(
                    id = id,
                    x = ((event.getX(index) - content.left).toDouble() / width).coerceIn(0.0, 1.0),
                    y = ((event.getY(index) - content.top).toDouble() / height).coerceIn(0.0, 1.0),
                    down = !allUp && index != liftedIndex,
                ),
            )
        }
        return contacts
    }
}
