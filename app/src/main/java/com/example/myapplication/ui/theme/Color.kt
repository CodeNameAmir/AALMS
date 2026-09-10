package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Modern Emerald Terminal Palette: Deep Charcoal Dark & Vibrant Emerald Green
// ============================================================================

// --- Brand Accent: Vibrant Emerald Green (Developer Console Style) ---
val EmeraldPrimary = Color(0xFF10B981)          // Emerald 500 - vibrant, high-tech, clean
val EmeraldPrimaryLight = Color(0xFF34D399)     // Emerald 400
val EmeraldPrimaryDark = Color(0xFF059669)      // Emerald 600
val EmeraldPrimaryContainer = Color(0xFF064E3B)  // Deep emerald forest container
val EmeraldOnPrimaryContainer = Color(0xFFD1FAE5)// Mint 100
val AccentGlow = Color(0xFF10B981)

// Backward compatibility mappings so existing references work seamlessly
val PurplePrimary = EmeraldPrimary
val PurplePrimaryLight = EmeraldPrimaryLight
val PurplePrimaryDark = EmeraldPrimaryDark
val PurplePrimaryContainer = EmeraldPrimaryContainer
val PurpleOnPrimaryContainer = EmeraldOnPrimaryContainer

// Compatibility tokens for standard M3
val Purple80 = EmeraldPrimaryLight
val PurpleGrey80 = Color(0xFFA7F3D0)
val Pink80 = Color(0xFF6EE7B7)

val Purple40 = EmeraldPrimaryDark
val PurpleGrey40 = Color(0xFF065F46)
val Pink40 = Color(0xFF047857)

// Aliases for brand indigo (redirected to emerald)
val BrandIndigo400 = EmeraldPrimary
val BrandIndigo500 = EmeraldPrimaryDark
val BrandIndigo600 = EmeraldPrimaryDark
val BrandIndigoLight = EmeraldPrimaryLight
val BrandIndigoContainerDark = EmeraldPrimaryContainer
val BrandIndigoOnContainerDark = EmeraldOnPrimaryContainer
val BrandIndigoContainerLight = Color(0xFFECFDF5)
val BrandIndigoOnContainerLight = Color(0xFF064E3B)

val BrandViolet400 = Color(0xFF2DD4BF) // Teal 400
val BrandViolet600 = Color(0xFF0D9488) // Teal 600

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


