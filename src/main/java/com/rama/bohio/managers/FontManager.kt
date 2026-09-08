package com.rama.bohio.managers

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.rama.bohio.objects.PrefFontStyle
import java.io.File

object FontManager {

    private val cache = mutableMapOf<String, Typeface?>()

    fun applyFont(context: Context, root: View) {
        val prefs = PrefsManager.getInstance(context)
        val fontStyle = prefs.getFontStyle().ifBlank { "system" }

        val typeface = getTypeface(context, fontStyle)
        applyRecursively(root, typeface)
    }

    fun getTypeface(context: Context, style: String): Typeface? {
        // Returning null here means ThemeManager's `typeface?.let` skips the assignment,
        // so a view keeps whatever the previous pass gave it and switching back to the
        // default appears to do nothing until the process restarts.
        if (style == PrefFontStyle.DEFAULT) return Typeface.DEFAULT

        // Custom font: always reload from path (don't cache by style key alone)
        if (style == PrefFontStyle.CUSTOM) {
            val path = PrefsManager.getInstance(context).getCustomFontPath()
            if (path.isBlank()) return Typeface.DEFAULT
            val cacheKey = "custom:$path"
            if (cache.containsKey(cacheKey)) return cache[cacheKey] ?: Typeface.DEFAULT
            val tf = runCatching { Typeface.createFromFile(File(path)) }.getOrNull()
            cache[cacheKey] = tf
            return tf ?: Typeface.DEFAULT
        }

        if (cache.containsKey(style)) return cache[style] ?: Typeface.DEFAULT

        val tf = when (style) {
            PrefFontStyle.JERSEY_25 ->
                Typeface.createFromAsset(context.assets, "fonts/jersey25_regular.otf")

            else -> null
        }
        cache[style] = tf
        return tf ?: Typeface.DEFAULT
    }

    // Call this after saving a new custom font so the old cached entry is evicted.
    fun clearCustomCache() {
        cache.keys.filter { it.startsWith("custom:") }.forEach { cache.remove(it) }
    }

    private fun applyRecursively(view: View, typeface: Typeface?) {
        if (view is TextView) view.typeface = typeface ?: Typeface.DEFAULT
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) applyRecursively(view.getChildAt(i), typeface)
        }
    }

    // Public single-view entry point used by adapters to theme individual rows.
    fun applyTypefaceToView(view: View, typeface: Typeface?) = applyRecursively(view, typeface)
}
