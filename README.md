# 🎧 Soundcore Personalized — Universal Autonomous DSP & IoT Gateway

**Soundcore Personalized** is an ultra-lightweight (~100 KB), zero-bloat Android native application & IoT edge gateway that communicates directly with **Anker Soundcore** earbuds and headphones over Bluetooth RFCOMM (`0cf12d31-...` / `08 EE 00 00 00` binary protocol).

It brings flagship autonomous features—normally locked behind $300+ headphones—to the entire Soundcore lineup with **zero hardware modification**.

---

## ✨ Key Features

### 1. 🧠 Context-Aware Autonomous Modes
- **1A • Per-App & Content Auto-EQ (Cinema IMAX & BassUp Engine):**
  - Automatically monitors active Android `MediaSession` controllers in real time.
  - Switches to **🎬 Cinema & YouTube IMAX** (deep sub-bass + crystal-clear vocal presence + 3D Virtualizer + BassUp) when watching **YouTube / Netflix**.
  - Switches to **🎵 Music + BassUp Pro** when listening on **YouTube Music / Spotify**.
- **1B • Smart Activity & Speed ANC:**
  - Fuses real-time **Linear Accelerometer (RMS `m/s²`)** and **GPS Speed (`km/h`)** telemetry:
    - 🪑 **Stationary / Desk:** Enables **Max ANC (Strong Level 3)**.
    - 🏃 **Walking / Running:** Switches to **Full Transparency Mode + Sport X20 Workout EQ** for outdoor safety.
    - 🚆 **Transit / High Speed (>18 km/h):** Enables **Max ANC + Wind Noise Suppression**.
- **1C • Speak-to-Chat (Voice-Triggered Transparency):**
  - Continuously analyzes voice energy (`dB`) when enabled.
  - The moment you start speaking, it ducks media volume to **20%** and switches the earbuds to **Vocal Transparency Mode**; 5 seconds after you finish speaking, it automatically restores **ANC** and your previous volume.

### 2. 📡 Direct Hardware RFCOMM Protocol (`08 EE 00 00 00`)
- Dynamic `0cf12d31-fac3-4553-bd80-d6832e7bXXXX` service UUID discovery across all Soundcore models (*Sport X20 `A3968`, Liberty 4 NC `A3947`, Space A40 `A3936`, Life Q30 `A3028`, P40i, R50i, etc.*).
- **143-Byte State Telemetry Parser (`[0x01, 0x01]`):** Live Left, Right, and Charging Case battery levels, firmware versions, serial number, and physical button state sync (`[0x06, 0x01]`).
- **8-Band Hardware DSP Equalizer + Anker DRC Matrix (`[0x03, 0x86]`):** Computes the exact 8-band Dynamic Range Compression matrix on-the-fly, paired with Android's native `Equalizer`, `BassBoost`, `LoudnessEnhancer`, and `Virtualizer` 3D audio stages.
- **24/7 Auto-Reconnect Watchdog:** Automatically establishes the RFCOMM control channel within 1.2 seconds of taking the earbuds out of the charging case (`ACTION_ACL_CONNECTED`).

---

## 🛠️ Build & Install

```bash
./build_and_install.sh
adb install -r -g build/Soundcore_Universal_Personalized.apk
```
