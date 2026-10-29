# 🎧 Soundcore Personalized — Universal Autonomous DSP & IoT Gateway

**Soundcore Personalized** is an ultra-lightweight (~125 KB), zero-bloat, privacy-first native Android application that communicates directly with **Anker Soundcore** earbuds and headphones over Bluetooth RFCOMM (`0cf12d31-...` / `08 EE 00 00 00` binary protocol).

It brings flagship autonomous features—normally locked behind $300+ headphones—to the entire Soundcore lineup (**Sport X20, Liberty 4 / 4 NC, Space A40, Space One, Life Q30 / Q35 / Q45, P20i / P40i, R50i, Life P2 / P3**, etc.) with **zero cloud dependency, zero telemetry tracking, and zero hardware modification**.

---

## ✨ Key Features (v2.0)

### 1. 🧠 4 Autonomous Context-Aware Engines
- **1A • Smart Cinema & Media Auto-Sound:**
  - Automatically monitors active Android `MediaSession` controllers in real time.
  - Applies a warm, distortion-free **🎬 Cinema & Natural Stage (`{35, 25, 10, 5, 15, 20, 25, 20}`)** profile with volume-adaptive headroom scaling when watching **YouTube / Netflix / Prime Video / Twitch**.
  - Switches to **🎵 Music + Balanced Bass** when listening on **YouTube Music / Spotify / Apple Music** and **🎙️ Clear Voice** on **Podcasts / Audible**.
- **1B • Step-Counter & Speed-Aware ANC:**
  - Uses Android's hardware `TYPE_STEP_DETECTOR` + Linear Accelerometer + GPS speed:
    - 🪑 **Stationary (12s+ without steps):** Automatically enables **Max ANC (Strong Level 3)**.
    - 🚶 **Walking (8+ steps within 10s):** Switches to **Full Transparency Mode** so you hear surroundings and traffic safely.
    - 🚆 **Transit / High Wind (>18 km/h):** Enables **Max ANC + Hardware Wind Noise Suppression**.
- **1C • Speak-to-Chat (Voice-Triggered Transparency):**
  - Continuously monitors voice energy (`dB`) when enabled.
  - The moment you speak, it ducks media volume to **20%** and switches the earbuds to **Vocal Transparency Mode**; 5 seconds after speech ends, it restores **ANC** and your previous volume.
- **1D • Whisper & Call Mic Booster (HFP VGM 15/15 + AGC + HD Voice):**
  - Automatically detects incoming/active phone & VoIP calls (Phone, WhatsApp, Telegram, Zoom).
  - Boosts Bluetooth HFP microphone gain (`bt_headset_vgm=15`), enables Wideband Speech (`bt_wbs=on`), activates hardware `AutomaticGainControl` (`AGC`), `NoiseSuppressor` (`NS`), and `AcousticEchoCanceler` (`AEC`), and switches the earbud DSP to **Wind Filter + Vocal Mode** so even a quiet whisper is transmitted clearly to the other party.
  - Includes a **Live Whisper Loopback Test** with an adjustable `1.0x – 5.0x` pre-amp slider and real-time input/output dB meter.

### 2. 🌍 Multi-Lingual System Locale Adaptation (7 Languages)
- Automatically detects the phone's system language (`Locale.getDefault()`) and supports instant one-tap switching inside the app header:
  - 🇹🇷 **Türkçe** (`tr`)
  - 🇬🇧 **English** (`en`)
  - 🇩🇪 **Deutsch** (`de`)
  - 🇪🇸 **Español** (`es`)
  - 🇫🇷 **Français** (`fr`)
  - 🇷🇺 **Русский** (`ru`)
  - 🇸🇦 **العربية** (`ar` — with automatic RTL layout support)

### 3. ⚙️ Master Power Control, Favorite Profile & Factory Default Reset
- **🟢 Master Power Switch & `⏻` Button:** Instantly disconnects the RFCOMM socket and disables all audio effects so the earbuds operate in 100% stock standalone mode whenever desired.
- **🔗 Auto-Connect on Bluetooth Toggle:** Choose whether the app automatically connects when earbuds are taken out of the charging case (`ACTION_ACL_CONNECTED`).
- **⭐ Save / 📂 Load Favorite Profile:** Save your personal EQ, ANC, and microphone configuration to `SharedPreferences` and restore it anytime.
- **🔄 One-Tap Factory Default Reset:** Instantly restores the golden Cinema EQ, Max ANC, and 3.2x Whisper Mic Booster defaults.

### 4. 📡 Universal Hardware RFCOMM Protocol (`08 EE 00 00 00`)
- Dynamic `0cf12d31-fac3-4553-bd80-d6832e7bXXXX` service UUID discovery across all Soundcore models.
- Dual-generation command framing: supports both **6-byte `[0x06, 0x81]`** (Sport X20, Liberty 4 NC, Space One, P40i) and **4-byte `[0x06, 0x81]`** (Life Q30/Q35, Space A40, Life P3) sound mode packets automatically.
- **8-Band Hardware DSP Equalizer + Anker DRC Matrix (`[0x03, 0x86]`):** Writes custom 8-band stereo curves directly to the earbud EEPROM/DSP.

---

## 🛠️ Build & Install

```bash
./build_and_install.sh
adb install -r -g build/Soundcore_Universal_Personalized.apk
```
