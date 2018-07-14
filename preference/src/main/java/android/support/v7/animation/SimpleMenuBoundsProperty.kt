package android.support.v7.animation

import android.annotation.TargetApi
import android.graphics.Rect
import android.os.Build
import android.util.Property

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class SimpleMenuBoundsProperty(name: String) : Property<PropertyHolder, Rect>(Rect::class.java, name) {

    override fun get(holder: PropertyHolder): Rect {
        return holder.background.fixedBounds
    }

    override fun set(holder: PropertyHolder, value: Rect) {
        holder.background.fixedBounds = value
        holder.contentView.clipBounds = value
    }

    companion object {

        val BOUNDS: Property<PropertyHolder, Rect>

        init {
            BOUNDS = SimpleMenuBoundsProperty("bounds")
        }
    }
}
