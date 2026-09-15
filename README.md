# 🌐 WexBrowser (VexBrowser) v2.0

<p align="center">
  <img src="logo.png" alt="WexBrowser Logo" width="128" height="128">
</p>

<p align="center">
  <b>Browser Android Cepat, Ringan, Hemat Kuota & Dilengkapi Perlindungan VexShield</b><br>
  Mendukung Android 5.0 (Lollipop) hingga Android 14+ (API 21 - 34)
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-2.0-blue.svg" alt="Version">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/minSdk-21-orange.svg" alt="minSdk">
  <img src="https://img.shields.io/badge/targetSdk-34-blueviolet.svg" alt="targetSdk">
  <img src="https://img.shields.io/badge/License-MIT-brightgreen.svg" alt="License">
</p>

---

## 🚀 Fitur Unggulan

- 🛡️ **VexShield AdBlock & Tracker Protection**: Memblokir iklan agresif, script pelacak, dan banner pop-up yang memperlambat browser.
- ⚡ **Ultra-Ringan & Cepat**: Ukuran aplikasi hanya ~8 MB, dioptimalkan untuk perangkat lawas seperti Samsung Galaxy Note 3 maupun smartphone flagship modern.
- 📱 **Multi-Window & Modern Web Compatibility**:
  - Dukungan penuh untuk halaman modern berbasis SPA / JavaScript (seperti DuckDuckGo, Linktree, Google, YouTube).
  - Penanganan link eksternal dan pop-up pintar tanpa freeze atau layar hitam.
- 🛍️ **Smart Quick Dials**: Pintasan cepat ke e-commerce terpercaya (Shopee, Tokopedia, Lazada, Traveloka, dsb.).
- 🔒 **Privasi Terjamin**: Perlindungan privasi tanpa pelacakan data pribadi yang berlebihan. Kebijakan privasi transparan dan siap untuk standar Google Play Store.
- 🎯 **Monetisasi Terintegrasi**: Terintegrasi dengan Google AdMob SDK v23.0.0 siap rilis.

---

## 🛠️ Persyaratan Sistem & Spesifikasi

| Komponen | Spesifikasi |
| :--- | :--- |
| **Nama Aplikasi** | WexBrowser (VexBrowser) |
| **Package Name** | `com.vex.browser` |
| **Versi** | 2.0 (VersionCode: 10) |
| **Minimum SDK** | Android 5.0 Lollipop (API 21) |
| **Target SDK** | Android 14 (API 34) |
| **Gradle Plugin** | 8.3.1 |
| **Java / JDK** | Java 8 / 17 compatible |

---

## 📦 Panduan Build & Kompilasi

### 1. Prasyarat
- Android Studio Iguana / Jellyfish atau lebih baru.
- Android SDK Platform 34 & Build-Tools 34.0.0.
- JDK 17.

### 2. Clone Repositori
```bash
git clone https://github.com/dikkayulianto/WexBrowser.git
cd WexBrowser
```

### 3. Build APK (Debug / Release)
Untuk menghasilkan file debug APK:
```bash
./gradlew assembleDebug
```
Untuk menghasilkan file signed Release APK:
```bash
./gradlew assembleRelease
```
File APK siap instal akan berada di: `app/build/outputs/apk/release/app-release.apk`

### 4. Build Android App Bundle (AAB) untuk Google Play Store
```bash
./gradlew bundleRelease
```
File AAB akan berada di: `app/build/outputs/bundle/release/app-release.aab`

---

## 📜 Kebijakan Privasi (Privacy Policy)

Dokumen lengkap kebijakan privasi tersedia di [PRIVACY_POLICY.md](PRIVACY_POLICY.md). URL ini dapat langsung dicantumkan pada Google Play Console.

---

## 📄 Lisensi

Proyek ini dilisensikan di bawah lisensi MIT - lihat file [LICENSE](LICENSE) untuk detail lebih lanjut.
