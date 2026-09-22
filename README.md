# Scanner Pro - Android Document Scanner

Scanner Pro is a native Android document scanner application built with Kotlin and Jetpack Compose.

## Features
- Automatic edge detection & 4-point perspective cropping
- Document enhancement filters (Color, Grayscale, High-Contrast B&W)
- Multi-page document organization & category tags
- On-device OCR text extraction with ML Kit
- Local PDF generation and secure passcode-protected document vault
- Offline-first local storage using Room Database

## Build & Test
```bash
# Run unit and Robolectric tests
gradle :app:testDebugUnitTest

# Assemble debug APK
gradle :app:assembleDebug
```
