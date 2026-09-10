package com.blockforge.installer.ui.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private const val MOJANGLES_ASSET_PATH = "fonts/Mojangles.ttf"

/**
 * Tries to load the Mojangles typeface from assets/fonts/Mojangles.ttf (the user must
 * supply this file themselves, see the README in that folder). Falls back to a clean
 * monospace family if it isn't present, so a missing font asset never breaks the build
 * or crashes the app at runtime.
 *
 * We explicitly check the asset exists first (AssetManager.open throws IOException if
 * not) rather than only relying on catching a failure from Font()/FontFamily(), since
 * Compose can resolve fonts lazily during layout rather than at construction time.
 */
fun loadMojanglesFontFamily(context: Context): FontFamily {
    val assetExists = try {
        context.assets.open(MOJANGLES_ASSET_PATH).use { true }
    } catch (t: Throwable) {
        false
    }

    if (!assetExists) return FontFamily.Monospace

    return try {
        FontFamily(Font(MOJANGLES_ASSET_PATH, context.assets))
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
        headlineSmall = TextStyle(fontFamily = mojangles, fontSize = 22.sp),
        titleLarge = TextStyle(fontFamily = mojangles, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = mojangles, fontSize = 16.sp),
        bodyLarge = TextStyle(fontFamily = mojangles, fontSize = 15.sp),
        bodyMedium = TextStyle(fontFamily = mojangles, fontSize = 13.sp),
        labelLarge = TextStyle(fontFamily = mojangles, fontSize = 14.sp)
    )
}
