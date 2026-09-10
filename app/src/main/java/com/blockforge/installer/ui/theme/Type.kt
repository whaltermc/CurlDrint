package com.blockforge.installer.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.toFontFamily

private const val MOJANGLES_ASSET_PATH = "fonts/Mojangles.ttf"

/**
 * Tries to load the Mojangles typeface from assets/fonts/Mojangles.ttf (the user must
 * supply this file themselves, see the README in that folder). Falls back to a clean
 * monospace family if it isn't present, so a missing font asset never breaks the build
 * or crashes the app at runtime.
 */
fun loadMojanglesFontFamily(context: Context): FontFamily {
    return try {
        val typeface = Typeface.createFromAsset(context.assets, MOJANGLES_ASSET_PATH)
        typeface.toFontFamily()
    } catch (t: Throwable) {
        FontFamily.Monospace
    }
}

@Composable
fun rememberMojanglesFontFamily(): FontFamily {
    val context = LocalContext.current
    return remember { loadMojanglesFontFamily(context) }
}

@Composable
fun blockForgeTypography(): Typography {
    val mojangles = rememberMojanglesFontFamily()
    return Typography(
        headlineSmall = TextStyle(fontFamily = mojangles, fontSize = 22.sp()),
        titleLarge = TextStyle(fontFamily = mojangles, fontSize = 20.sp()),
        titleMedium = TextStyle(fontFamily = mojangles, fontSize = 16.sp()),
        bodyLarge = TextStyle(fontFamily = mojangles, fontSize = 15.sp()),
        bodyMedium = TextStyle(fontFamily = mojangles, fontSize = 13.sp()),
        labelLarge = TextStyle(fontFamily = mojangles, fontSize = 14.sp())
    )
}

// Small helper so this file has no extra Compose unit import clutter above.
private fun Int.sp() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
