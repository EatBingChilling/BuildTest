package com.project.lumina.client.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.project.lumina.client.R

val MyFontFamily = FontFamily(
    Font(R.font.fredoka_light)
)

val MyTypography = Typography(
    displayLarge = TextStyle(fontFamily = MyFontFamily, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = MyFontFamily, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = MyFontFamily, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = MyFontFamily, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = MyFontFamily, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = MyFontFamily, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = MyFontFamily, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = MyFontFamily, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = MyFontFamily, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = MyFontFamily, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = MyFontFamily, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = MyFontFamily, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = MyFontFamily, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = MyFontFamily, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = MyFontFamily, fontSize = 11.sp),
)

@Composable
fun LuminaClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MyTypography,
        content = content
    )
}