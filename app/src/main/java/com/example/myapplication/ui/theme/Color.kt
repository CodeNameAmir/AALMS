package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Modern Electric Blue Terminal Palette: Deep Charcoal Dark & Vibrant Blue
// ============================================================================

// --- Brand Accent: Vibrant Electric Blue (Developer Console Style) ---
val BluePrimary = Color(0xFF3B82F6)          // Blue 500 - vibrant, crisp, modern developer blue
val BluePrimaryLight = Color(0xFF60A5FA)     // Blue 400
val BluePrimaryDark = Color(0xFF2563EB)      // Blue 600
val BluePrimaryContainer = Color(0xFF1E3A8A).copy(alpha = 0.5f)  // Midnight Blue container
val BlueOnPrimaryContainer = Color(0xFFDBEAFE)// Blue 100
val AccentGlow = Color(0xFF3B82F6)

// Backward compatibility mappings so existing references work seamlessly
val EmeraldPrimary = BluePrimary
val EmeraldPrimaryLight = BluePrimaryLight
val EmeraldPrimaryDark = BluePrimaryDark
val EmeraldPrimaryContainer = BluePrimaryContainer
val EmeraldOnPrimaryContainer = BlueOnPrimaryContainer

val PurplePrimary = BluePrimary
val PurplePrimaryLight = BluePrimaryLight
val PurplePrimaryDark = BluePrimaryDark
val PurplePrimaryContainer = BluePrimaryContainer
val PurpleOnPrimaryContainer = BlueOnPrimaryContainer

// Compatibility tokens for standard M3
val Purple80 = BluePrimaryLight
val PurpleGrey80 = Color(0xFFBFDBFE)
val Pink80 = Color(0xFF93C5FD)

val Purple40 = BluePrimaryDark
val PurpleGrey40 = Color(0xFF1E40AF)
val Pink40 = Color(0xFF1D4ED8)

// Aliases for brand indigo (redirected to blue)
val BrandIndigo400 = BluePrimary
val BrandIndigo500 = BluePrimaryDark
val BrandIndigo600 = BluePrimaryDark
val BrandIndigoLight = BluePrimaryLight
val BrandIndigoContainerDark = BluePrimaryContainer
val BrandIndigoOnContainerDark = BlueOnPrimaryContainer
val BrandIndigoContainerLight = Color(0xFFEFF6FF)
val BrandIndigoOnContainerLight = Color(0xFF1E3A8A)

val BrandViolet400 = Color(0xFF38BDF8) // Sky 400
val BrandViolet600 = Color(0xFF0284C7) // Sky 600

// --- Dark Theme Surfaces (True Deep Charcoal / Neutral Dark - NO Blue/Purple Tint) ---
val DarkBg = Color(0xFF0F0F12)                 // Clean neutral deep dark canvas
val DarkBg2 = Color(0xFF18181C)                // Surface for cards, dialogs & app bars
val DarkBg3 = Color(0xFF222228)                // Surface for inputs, inner containers & chips
val DarkSurfaceHighest = Color(0xFF2C2C35)     // Popups, active states
val DarkBorder = Color(0xFF2A2A33)             // Subtle neutral hairline border
val DarkBorderSubtle = Color(0xFF1E1E24)

val DarkCanvas = DarkBg
val DarkSurface = DarkBg2
val DarkSurfaceVariant = DarkBg3
val DarkBorderRefined = DarkBorder
val DarkBorderSubtleRefined = DarkBorderSubtle

// --- Light Theme Surfaces (Clean Neutral White & Soft Warm Gray) ---
val LightCanvas = Color(0xFFFAFAFA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF4F4F6)
val LightSurfaceHighest = Color(0xFFE5E5EA)
val LightBorderRefined = Color(0xFFE4E4E8)
val LightBorderSubtleRefined = Color(0xFFEEEEF2)

// --- Text & Typography Tokens (Neutral Zinc, Crisp & High Contrast) ---
val TextPrimaryDark = Color(0xFFF9FAFB)
val TextSecondaryDark = Color(0xFFA1A1AA)      // Zinc 400 neutral
val TextMutedDark = Color(0xFF71717A)          // Zinc 500

val TextPrimaryLight = Color(0xFF18181B)       // Zinc 900
val TextSecondaryLight = Color(0xFF71717A)     // Zinc 500
val TextMutedLight = Color(0xFFA1A1AA)

val NeutralGray = TextSecondaryDark
val NeutralMuted = TextMutedDark
val NeutralDark = DarkBg3

// --- Semantic Log Level Colors ---
val ColorDebug = Color(0xFFA1A1AA)             // Neutral Zinc 400
val ColorInfo = Color(0xFF38BDF8)              // Clear Sky Blue (reserved exclusively for INFO logs)
val ColorWarning = Color(0xFFFBBF24)           // Amber 400
val ColorError = Color(0xFFF87171)             // Rose 400
val ColorCritical = Color(0xFFFB7185)          // Rose/Crimson 400 (no purple)

// --- Semantic Status Tints ---
val EmeraldStatus = Color(0xFF34D399)
val EmeraldStatusContainer = Color(0xFF10B981)
val AmberPinnedDark = Color(0xFF1E1C11)        // Amber pinned card background
val AmberPinnedLight = Color(0xFFFEF9C3)
val AmberPinnedBorder = Color(0xFFEAB308)


