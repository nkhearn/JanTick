package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Palette - Chalk & Slate Utilitarian Clipboard
val LightPrimary = Color(0xFF1E3A5F)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD6E3F5)
val LightOnPrimaryContainer = Color(0xFF0B223E)

val LightSecondary = Color(0xFF475569)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE2E8F0)
val LightOnSecondaryContainer = Color(0xFF1E293B)

val LightTertiary = Color(0xFFB45309)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFEF3C7)
val LightOnTertiaryContainer = Color(0xFF78350F)

val LightBackground = Color(0xFFF4F6F9)
val LightOnBackground = Color(0xFF111827)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF111827)
val LightSurfaceVariant = Color(0xFFE9EEF5)
val LightOnSurfaceVariant = Color(0xFF475569)
val LightOutline = Color(0xFFCBD5E1)
val LightOutlineVariant = Color(0xFFE2E8F0)

val LightError = Color(0xFFDC2626)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFEE2E2)
val LightOnErrorContainer = Color(0xFF7F1D1D)

// Dark Palette - Mechanical Room High-Readability Slate
val DarkPrimary = Color(0xFF93C5FD)
val DarkOnPrimary = Color(0xFF0A2540)
val DarkPrimaryContainer = Color(0xFF1E3A5F)
val DarkOnPrimaryContainer = Color(0xFFDBEAFE)

val DarkSecondary = Color(0xFF94A3B8)
val DarkOnSecondary = Color(0xFF0F172A)
val DarkSecondaryContainer = Color(0xFF283241)
val DarkOnSecondaryContainer = Color(0xFFE2E8F0)

val DarkTertiary = Color(0xFFFBBF24)
val DarkOnTertiary = Color(0xFF451A03)
val DarkTertiaryContainer = Color(0xFF5C3409)
val DarkOnTertiaryContainer = Color(0xFFFEF3C7)

val DarkBackground = Color(0xFF0E1319)
val DarkOnBackground = Color(0xFFF1F5F9)
val DarkSurface = Color(0xFF161D26)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkSurfaceVariant = Color(0xFF222B38)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)
val DarkOutlineVariant = Color(0xFF242F3D)

val DarkError = Color(0xFFF87171)
val DarkOnError = Color(0xFF450A0A)
val DarkErrorContainer = Color(0xFF7F1D1D)
val DarkOnErrorContainer = Color(0xFFFEE2E2)

@Immutable
data class CustodianSemanticColors(
    val verified: Color,
    val onVerified: Color,
    val verifiedContainer: Color,
    val onVerifiedContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val cardBorder: Color,
    val elevatedSurface: Color
)

val LightSemanticColors = CustodianSemanticColors(
    verified = Color(0xFF15803D),
    onVerified = Color(0xFFFFFFFF),
    verifiedContainer = Color(0xFFDCFCE7),
    onVerifiedContainer = Color(0xFF14532D),
    warning = Color(0xFFD97706),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF78350F),
    cardBorder = Color(0xFFD8E0EA),
    elevatedSurface = Color(0xFFF8FAFC)
)

val DarkSemanticColors = CustodianSemanticColors(
    verified = Color(0xFF4ADE80),
    onVerified = Color(0xFF052E16),
    verifiedContainer = Color(0xFF14532D),
    onVerifiedContainer = Color(0xFFBBF7D0),
    warning = Color(0xFFFBBF24),
    onWarning = Color(0xFF451A03),
    warningContainer = Color(0xFF5A320A),
    onWarningContainer = Color(0xFFFEF3C7),
    cardBorder = Color(0xFF283444),
    elevatedSurface = Color(0xFF1C2530)
)

val LocalCustodianColors = staticCompositionLocalOf { LightSemanticColors }
