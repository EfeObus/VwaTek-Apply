# Asset Directory

This directory contains visual assets for VwaTek Apply.

## Contents

| File | Description | Format |
|------|-------------|--------|
| logo.png | Main application logo | PNG (200x200) |
| icon.png | App icon | PNG (1024x1024) |
| splash.png | Splash screen image | PNG |

## Guidelines

- All icons should be provided in multiple resolutions for different screen densities
- Use vector formats (SVG) where possible for scalability
- No emojis in any visual assets
- Clipart and vector icons are permitted

## Platform Requirements

### iOS
- App Icon: 1024x1024 PNG (no alpha channel)
- Place in `iosApp/Assets.xcassets/AppIcon.appiconset/`

### Android
- Adaptive Icon: Foreground and background layers
- Place in `androidApp/src/main/res/mipmap-*/`

### Web
- Favicon: 16x16, 32x32, and 192x192 PNG
- Place in `webApp/src/jsMain/resources/`
