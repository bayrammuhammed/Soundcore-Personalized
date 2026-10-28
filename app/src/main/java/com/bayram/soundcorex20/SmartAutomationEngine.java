package com.bayram.soundcorex20;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.Virtualizer;
import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@SuppressLint("MissingPermission")
public class SmartAutomationEngine implements SensorEventListener, LocationListener {

    public interface Listener {
        void onStateUpdated();
    }

    public static final int ACTIVITY_STATIONARY = 0;
    public static final int ACTIVITY_WALKING_RUNNING = 1;
    public static final int ACTIVITY_TRANSIT_FAST = 2;

    // Built-in EQ Presets (8 bands: 100Hz, 200Hz, 400Hz, 800Hz, 1.6kHz, 3.2kHz, 6.4kHz, 12.8kHz in 0.1dB)
    public static final int[][] EQ_PRESETS = new int[][]{
            {0, 0, 0, 0, 0, 0, 0, 0},                // 0: Soundcore İmza (Orijinal Dengeli & Doğal)
            {35, 25, 10, 0, 10, 15, 20, 15},         // 1: Müzik + Dengeli BassUp
            {10, 15, 20, 20, 15, 10, 5, 0},          // 2: YouTube / Doğal Sinema
            {30, 25, 10, 5, 15, 20, 25, 20},         // 3: Sport X20 Antrenman
            {25, 15, -5, 0, 10, 20, 20, 20},         // 4: Rock & Enerji
            {20, 10, 15, 15, 20, 20, 20, 10}         // 5: Akustik Sahne
    };

    public static final String[] EQ_PRESET_NAMES = new String[]{
            "Soundcore İmza (Orijinal)",
            "Müzik + BassUp",
            "YouTube / Doğal Sinema",
            "Sport X20 Antrenman",
            "Rock & Enerji",
            "Akustik Sahne"
    };

    private static SmartAutomationEngine instance;

    public static synchronized SmartAutomationEngine getInstance(Context context) {
        if (instance == null) {
            instance = new SmartAutomationEngine(context.getApplicationContext());
        }
        return instance;
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();
    private final SoundcoreProtocol.DeviceState state = new SoundcoreProtocol.DeviceState();
    private final List<String> telemetryLogs = new ArrayList<>();

    private BluetoothSocket btSocket;
    private OutputStream btOut;
    private InputStream btIn;
    private volatile boolean connecting = false;
    private String connectionStatusText = "Bağlantı Bekleniyor...";
    private String lastTxHex = "-";
    private String lastRxHex = "-";

    // AudioFX Dual Engine (Hardware DSP + System Cinema BassBoost & 3D Virtualizer)
    private Equalizer systemEq;
    private LoudnessEnhancer bassUpEnhancer;
    private BassBoost systemBassBoost;
    private Virtualizer systemVirtualizer;
    private boolean bassUpEnabled = true;
    private int currentPresetIndex = 0;

    // #1A: Auto-EQ by Media App
    private boolean autoEqByAppEnabled = true;
    private String currentMediaPackage = "com.google.android.apps.youtube.music";
    private String currentMediaTitle = "Şar";
    private String currentMediaArtist = "Koma Azad";
    private boolean currentMediaPlaying = false;
    private String lastAutoEqAppliedForPkg = "";

    // #1B: Smart Activity & Speed ANC
    private boolean smartActivityAncEnabled = true;
    private int currentActivityState = ACTIVITY_STATIONARY;
    private float currentMovementRms = 0.15f;
    private float currentSpeedKmh = 0.0f;
    private long lastActivityChangeMs = 0L;
    private boolean simulationOverrideActive = false;

    // #1C: Speak-to-Chat (Voice Detection -> Transparency + Volume Duck)
    private boolean speakToChatEnabled = false;
    private volatile boolean speakThreadRunning = false;
    private int currentMicDbLevel = 0; // 0..100
    private int speakThresholdLevel = 58; // 0..100
    private boolean speakTriggeredActive = false;
    private int speakCountdownSec = 0;
    private int savedAmbientModeBeforeSpeech = SoundcoreProtocol.AMBIENT_ANC;
    private int savedVolumeBeforeSpeech = -1;
    private long lastSpeechDetectedTimeMs = 0L;

    private final SensorManager sensorManager;
    private final AudioManager audioManager;

    private SmartAutomationEngine(Context context) {
        this.context = context;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        addLog("🚀 Soundcore Personalized Otonom Motoru başlatıldı.");
        initSystemAudioFx();
        registerBluetoothReceiver();
        startSensors();
        startPeriodicMediaPoll();
        connectToEarbuds();
    }

    private void registerBluetoothReceiver() {
        try {
            android.content.IntentFilter filter = new android.content.IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            filter.addAction( android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            filter.addAction(android.bluetooth.BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            context.registerReceiver(new BluetoothAutoConnectReceiver(), filter);
        } catch (Throwable ignored) {
        }
    }

    public synchronized void onEarbudsDisconnected(String reason) {
        try {
            if (btSocket != null) {
                btSocket.close();
            }
        } catch (Throwable ignored) {
        }
        btSocket = null;
        btOut = null;
        btIn = null;
        connecting = false;
        state.connected = false;
        connectionStatusText = "Kulaklık Kutuda / Bağlantı Bekleniyor (Otomatik Bağlanacak)";
        addLog(reason);
        notifyListeners();
    }

    private boolean isEarbudAclConnected() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) return false;
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            if (bonded != null) {
                for (BluetoothDevice dev : bonded) {
                    if (SoundcoreProtocol.isSoundcoreDevice(dev)) {
                        Method m = dev.getClass().getMethod("isConnected");
                        Object res = m.invoke(dev);
                        if (res instanceof Boolean && (Boolean) res) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public void addListener(Listener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    private void notifyListeners() {
        mainHandler.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) {
                l.onStateUpdated();
            }
        });
    }

    public synchronized void addLog(String msg) {
        String ts = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        telemetryLogs.add(0, "[" + ts + "] " + msg);
        if (telemetryLogs.size() > 40) {
            telemetryLogs.remove(telemetryLogs.size() - 1);
        }
        notifyListeners();
    }

    public synchronized List<String> getTelemetryLogs() {
        return new ArrayList<>(telemetryLogs);
    }

    public SoundcoreProtocol.DeviceState getState() {
        return state;
    }

    public String getConnectionStatusText() {
        return connectionStatusText;
    }

    public String getLastTxHex() {
        return lastTxHex;
    }

    public String getLastRxHex() {
        return lastRxHex;
    }

    public boolean isBassUpEnabled() {
        return bassUpEnabled;
    }

    public int getCurrentPresetIndex() {
        return currentPresetIndex;
    }

    public boolean isAutoEqByAppEnabled() {
        return autoEqByAppEnabled;
    }

    public boolean isSmartActivityAncEnabled() {
        return smartActivityAncEnabled;
    }

    public boolean isSpeakToChatEnabled() {
        return speakToChatEnabled;
    }

    public String getCurrentMediaPackage() {
        return currentMediaPackage;
    }

    public String getCurrentMediaTitle() {
        return currentMediaTitle;
    }

    public String getCurrentMediaArtist() {
        return currentMediaArtist;
    }

    public int getCurrentActivityState() {
        return currentActivityState;
    }

    public float getCurrentMovementRms() {
        return currentMovementRms;
    }

    public float getCurrentSpeedKmh() {
        return currentSpeedKmh;
    }

    public int getCurrentMicDbLevel() {
        return currentMicDbLevel;
    }

    public int getSpeakThresholdLevel() {
        return speakThresholdLevel;
    }

    public void setSpeakThresholdLevel(int level) {
        this.speakThresholdLevel = Math.max(25, Math.min(90, level));
        notifyListeners();
    }

    public boolean isSpeakTriggeredActive() {
        return speakTriggeredActive;
    }

    public int getSpeakCountdownSec() {
        return speakCountdownSec;
    }

    private void initSystemAudioFx() {
        try {
            systemEq = new Equalizer(100, 0);
            systemEq.setEnabled(false);
        } catch (Throwable ignored) {
        }
        try {
            bassUpEnhancer = new LoudnessEnhancer(0);
            bassUpEnhancer.setTargetGain(0);
            bassUpEnhancer.setEnabled(false);
        } catch (Throwable ignored) {
        }
    }

    private void applySystemAudioFx(int[] bands8TenthsDb, boolean bassUp) {
        try {
            if (systemEq != null) {
                boolean allZero = true;
                for (int v : bands8TenthsDb) {
                    if (v != 0) {
                        allZero = false;
                        break;
                    }
                }
                if (allZero && !bassUp) {
                    systemEq.setEnabled(false);
                } else {
                    systemEq.setEnabled(true);
                    short numBands = systemEq.getNumberOfBands();
                    short[] range = systemEq.getBandLevelRange();
                    short minMb = range[0];
                    short maxMb = range[1];
                    for (short b = 0; b < numBands; b++) {
                        int idx = Math.min(7, (b * 8) / numBands);
                        int mb = bands8TenthsDb[idx] * 8;
                        if (bassUp && b == 0) {
                            mb += 150;
                        }
                        short clamped = (short) Math.max(minMb, Math.min(maxMb, mb));
                        systemEq.setBandLevel(b, clamped);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            if (bassUpEnhancer != null) {
                bassUpEnhancer.setTargetGain(bassUp ? 200 : 0);
                bassUpEnhancer.setEnabled(bassUp);
            }
        } catch (Throwable ignored) {
        }
    }

    public synchronized void connectToEarbuds() {
        if (connecting || (btSocket != null && btSocket.isConnected())) {
            if (btSocket != null && btSocket.isConnected()) {
                sendPacket(SoundcoreProtocol.buildRequestStatePacket(), "Durum Sorgulama");
            }
            return;
        }
        connecting = true;
        connectionStatusText = "soundcore Sport X20'ye Bağlanılıyor...";
        notifyListeners();

        new Thread(() -> {
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter == null || !adapter.isEnabled()) {
                    connectionStatusText = "Bluetooth Kapalı";
                    connecting = false;
                    notifyListeners();
                    return;
                }

                BluetoothDevice target = null;
                Set<BluetoothDevice> bonded = adapter.getBondedDevices();
                if (bonded != null) {
                    // First priority: a Soundcore device that is currently ACL connected
                    for (BluetoothDevice d : bonded) {
                        if (SoundcoreProtocol.isSoundcoreDevice(d)) {
                            try {
                                Method m = d.getClass().getMethod("isConnected");
                                Object res = m.invoke(d);
                                if (res instanceof Boolean && (Boolean) res) {
                                    target = d;
                                    break;
                                }
                            } catch (Throwable ignored) {
                            }
                            if (target == null) {
                                target = d;
                            }
                        }
                    }
                }
                if (target == null) {
                    target = adapter.getRemoteDevice(SoundcoreProtocol.TARGET_MAC);
                }

                if (target.getName() != null && !target.getName().isEmpty()) {
                    state.deviceName = target.getName();
                }
                if (target.getAddress() != null) {
                    state.deviceMac = target.getAddress();
                }
                java.util.UUID dynamicUuid = SoundcoreProtocol.findDeviceRfcommUuid(target);

                BluetoothSocket sock = null;
                // Strategy 1: Dynamically discovered Soundcore 0cf12d31-... UUID for this model
                try {
                    sock = target.createRfcommSocketToServiceRecord(dynamicUuid);
                    sock.connect();
                } catch (Throwable e1) {
                    try {
                        if (sock != null) sock.close();
                    } catch (Throwable ignored) {}
                    // Strategy 2: Insecure RFCOMM with dynamic UUID
                    try {
                        sock = target.createInsecureRfcommSocketToServiceRecord(dynamicUuid);
                        sock.connect();
                    } catch (Throwable e2) {
                        try {
                            if (sock != null) sock.close();
                        } catch (Throwable ignored) {}
                        // Strategy 3: Direct RFCOMM SCN channel 10 / 1 fallback
                        try {
                            Method m = target.getClass().getMethod("createRfcommSocket", int.class);
                            sock = (BluetoothSocket) m.invoke(target, 10);
                            if (sock != null) sock.connect();
                        } catch (Throwable e3) {
                            try {
                                if (sock != null) sock.close();
                            } catch (Throwable ignored) {}
                            // Strategy 4: Standard SPP UUID (00001101-...)
                            sock = target.createRfcommSocketToServiceRecord(SoundcoreProtocol.SPP_FALLBACK_UUID);
                            sock.connect();
                        }
                    }
                }

                btSocket = sock;
                btOut = sock.getOutputStream();
                btIn = sock.getInputStream();
                state.connected = true;
                connectionStatusText = "Bağlı • " + state.deviceName + " (RFCOMM Aktif)";
                addLog("✅ " + state.deviceName + " (" + state.deviceMac + ") RFCOMM kanalına bağlandı!");
                connecting = false;
                notifyListeners();

                startReaderThread();
                Thread.sleep(250);
                sendPacket(SoundcoreProtocol.buildRequestStatePacket(), "Tam Durum Sorgusu [01 01]");
                Thread.sleep(350);
                // Restore clean Soundcore Signature (Preset 0, flat 0.0 dB) on hardware DSP
                int[] flat = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
                sendPacket(SoundcoreProtocol.buildSetEqualizerPacket(0x0000, flat), "Orijinal Soundcore İmza EQ Yüklendi");

            } catch (Throwable e) {
                connecting = false;
                state.connected = false;
                connectionStatusText = "A2DP Bağlı (RFCOMM için Yeniden Bağlan'a dokunun)";
                addLog("⚠️ RFCOMM meşgul olabilir (Resmi Soundcore uygulaması açık olabilir): " + e.getMessage());
                notifyListeners();
            }
        }, "X20-RFCOMM-Connect").start();
    }

    private void startReaderThread() {
        new Thread(() -> {
            byte[] buf = new byte[512];
            while (btSocket != null && btSocket.isConnected() && btIn != null) {
                try {
                    int n = btIn.read(buf);
                    if (n <= 0) break;
                    lastRxHex = SoundcoreProtocol.toHex(buf, n);
                    boolean updated = SoundcoreProtocol.parseInboundPacket(buf, n, state);
                    if (updated) {
                        addLog("📥 Kulaklıktan veri alındı: " + lastRxHex);
                    }
                    notifyListeners();
                } catch (Throwable e) {
                    break;
                }
            }
            state.connected = false;
            connectionStatusText = "RFCOMM Bağlantısı Kesildi";
            notifyListeners();
        }, "X20-RFCOMM-Reader").start();
    }

    public synchronized void sendPacket(byte[] packet, String label) {
        lastTxHex = SoundcoreProtocol.toHex(packet, packet.length);
        addLog("📤 " + label + " -> " + lastTxHex);
        new Thread(() -> {
            try {
                if (btSocket != null && btSocket.isConnected() && btOut != null) {
                    btOut.write(packet);
                    btOut.flush();
                } else {
                    connectToEarbuds();
                }
            } catch (Throwable e) {
                addLog("❌ Paket gönderme hatası: " + e.getMessage());
            }
        }).start();
    }

    // ==================== SOUND MODES (ANC / TRANSPARENCY / NORMAL) ====================

    public void setAmbientMode(int ambientMode, String reason) {
        state.ambientMode = ambientMode;
        byte[] pkt = state.uses4ByteSoundMode
                ? SoundcoreProtocol.buildSetSoundModes4BytePacket(state.ambientMode, state.manualAncStrength, state.transparencyMode)
                : SoundcoreProtocol.buildSetSoundModesPacket(
                        state.ambientMode,
                        state.manualAncStrength,
                        state.adaptiveAncLevel,
                        state.transparencyMode,
                        state.ncMode,
                        state.windNoiseSuppression
                );
        String modeName = (ambientMode == SoundcoreProtocol.AMBIENT_ANC) ? "ANC (Gürültü Engelleme)"
                : (ambientMode == SoundcoreProtocol.AMBIENT_TRANSPARENCY) ? "Şeffaf Mod" : "Normal Mod";
        sendPacket(pkt, reason + ": " + modeName);
        notifyListeners();
    }

    public void setAncDetails(int ncMode, int manualStrength, boolean windSuppression, String reason) {
        state.ambientMode = SoundcoreProtocol.AMBIENT_ANC;
        state.ncMode = ncMode;
        state.manualAncStrength = manualStrength;
        state.windNoiseSuppression = windSuppression;
        byte[] pkt = SoundcoreProtocol.buildSetSoundModesPacket(
                state.ambientMode,
                state.manualAncStrength,
                state.adaptiveAncLevel,
                state.transparencyMode,
                state.ncMode,
                state.windNoiseSuppression
        );
        sendPacket(pkt, reason);
        notifyListeners();
    }

    public void setTransparencyDetails(int transMode, String reason) {
        state.ambientMode = SoundcoreProtocol.AMBIENT_TRANSPARENCY;
        state.transparencyMode = transMode;
        byte[] pkt = SoundcoreProtocol.buildSetSoundModesPacket(
                state.ambientMode,
                state.manualAncStrength,
                state.adaptiveAncLevel,
                state.transparencyMode,
                state.ncMode,
                state.windNoiseSuppression
        );
        sendPacket(pkt, reason);
        notifyListeners();
    }

    // ==================== EQUALIZER & BASSUP ====================

    public void applyEqPreset(int presetIndex, boolean enableBassUp, String reason) {
        if (presetIndex < 0 || presetIndex >= EQ_PRESETS.length) return;
        this.currentPresetIndex = presetIndex;
        this.bassUpEnabled = enableBassUp;
        int[] bands = new int[8];
        for (int i = 0; i < 8; i++) {
            bands[i] = EQ_PRESETS[presetIndex][i];
            if (enableBassUp && presetIndex != 0 && i == 0) {
                bands[i] = Math.min(120, bands[i] + 15);
            }
            state.eqBandsTenthsDb[i] = bands[i];
        }
        applySystemAudioFx(bands, enableBassUp && presetIndex != 0);
        int hwPresetId = (presetIndex == 0) ? 0x0000 : 0xFEFE;
        byte[] pkt = SoundcoreProtocol.buildSetEqualizerPacket(hwPresetId, bands);
        sendPacket(pkt, reason + " [" + EQ_PRESET_NAMES[presetIndex] + "]");
        notifyListeners();
    }

    public void setCustomEqBand(int bandIndex, int tenthsDb) {
        if (bandIndex < 0 || bandIndex >= 8) return;
        state.eqBandsTenthsDb[bandIndex] = Math.max(-120, Math.min(120, tenthsDb));
        applySystemAudioFx(state.eqBandsTenthsDb, bassUpEnabled);
        byte[] pkt = SoundcoreProtocol.buildSetEqualizerPacket(0xFEFE, state.eqBandsTenthsDb);
        sendPacket(pkt, "Özel EQ Bant " + (bandIndex + 1) + " (" + (tenthsDb / 10.0f) + " dB)");
        notifyListeners();
    }

    public void setBassUpEnabled(boolean enabled) {
        this.bassUpEnabled = enabled;
        applyEqPreset(currentPresetIndex, enabled, "BassUp Değiştirildi");
    }

    // ==================== #1A: PER-APP / PER-SONG AUTO-EQ ====================

    public void setAutoEqByAppEnabled(boolean enabled) {
        this.autoEqByAppEnabled = enabled;
        addLog("🎵 Uygulama Duyarlı Otomatik EQ: " + (enabled ? "AÇIK" : "KAPALI"));
        if (enabled) {
            lastAutoEqAppliedForPkg = "";
            evaluateAutoEqForCurrentMedia();
        }
        notifyListeners();
    }

    private void startPeriodicMediaPoll() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MediaNotificationListener.pollActiveMediaSessions(context);
                // Auto-reconnect watchdog if earbuds are out of case (ACL connected) but RFCOMM socket is not open
                if (!state.connected && !connecting && isEarbudAclConnected()) {
                    connectToEarbuds();
                }
                mainHandler.postDelayed(this, 3000);
            }
        }, 1500);
    }

    public void onMediaSessionDetected(String pkg, String title, String artist, boolean playing) {
        boolean changed = !pkg.equals(currentMediaPackage) || !title.equals(currentMediaTitle) || playing != currentMediaPlaying;
        this.currentMediaPackage = pkg;
        if (title != null && !title.isEmpty()) this.currentMediaTitle = title;
        if (artist != null && !artist.isEmpty()) this.currentMediaArtist = artist;
        this.currentMediaPlaying = playing;

        if (autoEqByAppEnabled && !pkg.equals(lastAutoEqAppliedForPkg)) {
            evaluateAutoEqForCurrentMedia();
        } else if (changed) {
            notifyListeners();
        }
    }

    public void simulateMediaAppSwitch(String pkg, String sampleTitle, String sampleArtist) {
        this.lastAutoEqAppliedForPkg = "";
        onMediaSessionDetected(pkg, sampleTitle, sampleArtist, true);
    }

    private void evaluateAutoEqForCurrentMedia() {
        if (!autoEqByAppEnabled || currentMediaPackage == null) return;
        lastAutoEqAppliedForPkg = currentMediaPackage;
        String pkg = currentMediaPackage.toLowerCase(Locale.ROOT);

        if (pkg.contains("youtube.music") || pkg.contains("spotify") || pkg.contains("deezer") || pkg.contains("apple.android.music")) {
            applyEqPreset(1, true, "🤖 Auto-EQ (Müzik Algılandı)");
        } else {
            // Keep original Soundcore Signature (0.0 dB flat, clean cinema sound) for YouTube & general videos
            applyEqPreset(0, false, "🤖 Auto-EQ (Orijinal Soundcore İmza)");
        }
    }

    // ==================== #1B: SMART ACTIVITY & SPEED ANC ====================

    public void setSmartActivityAncEnabled(boolean enabled) {
        this.smartActivityAncEnabled = enabled;
        this.simulationOverrideActive = false;
        addLog("🏃 Harekete Göre Otonom ANC: " + (enabled ? "AÇIK" : "KAPALI"));
        notifyListeners();
    }

    private void startSensors() {
        try {
            if (sensorManager != null) {
                Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
                if (accel == null) {
                    accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                }
                if (accel != null) {
                    sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 1.0f, this);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null || event.values.length < 3) return;
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        double mag = Math.sqrt(x * x + y * y + z * z);
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mag = Math.abs(mag - SensorManager.GRAVITY_EARTH);
        }
        currentMovementRms = (float) (currentMovementRms * 0.85 + mag * 0.15);

        if (!smartActivityAncEnabled || simulationOverrideActive || speakTriggeredActive) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastActivityChangeMs < 4000L) return;

        int detectedState;
        if (currentSpeedKmh >= 18.0f) {
            detectedState = ACTIVITY_TRANSIT_FAST;
        } else if (currentMovementRms >= 1.85f || (currentSpeedKmh >= 3.5f && currentSpeedKmh < 18.0f)) {
            detectedState = ACTIVITY_WALKING_RUNNING;
        } else {
            detectedState = ACTIVITY_STATIONARY;
        }

        if (detectedState != currentActivityState) {
            lastActivityChangeMs = now;
            applyActivityStateRule(detectedState, "🤖 Sensör Otonom Kararı");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null && location.hasSpeed()) {
            currentSpeedKmh = location.getSpeed() * 3.6f;
            notifyListeners();
        }
    }

    public void simulateActivityState(int newState) {
        this.simulationOverrideActive = true;
        applyActivityStateRule(newState, "⚡ Akıllı Hareket Testi");
    }

    private void applyActivityStateRule(int newState, String prefix) {
        this.currentActivityState = newState;
        if (newState == ACTIVITY_STATIONARY) {
            setAncDetails(SoundcoreProtocol.NC_MODE_MANUAL, SoundcoreProtocol.ANC_STRENGTH_STRONG, false,
                    prefix + " [Sabit -> Max ANC]");
        } else if (newState == ACTIVITY_WALKING_RUNNING) {
            setTransparencyDetails(SoundcoreProtocol.TRANS_FULLY_TRANSPARENT,
                    prefix + " [Yürüyüş/Koşu -> Şeffaf Mod]");
            applyEqPreset(3, true, prefix + " [Sport X20 Antrenman EQ]");
        } else if (newState == ACTIVITY_TRANSIT_FAST) {
            setAncDetails(SoundcoreProtocol.NC_MODE_MANUAL, SoundcoreProtocol.ANC_STRENGTH_STRONG, true,
                    prefix + " [Araç/Hızlı -> ANC + Rüzgar Engelleme]");
        }
        notifyListeners();
    }

    // ==================== #1C: SPEAK-TO-CHAT (VOICE TRIGGERED TRANSPARENCY) ====================

    public void setSpeakToChatEnabled(boolean enabled) {
        this.speakToChatEnabled = enabled;
        addLog("🗣️ Konuşunca Şeffaf Mod (Speak-to-Chat): " + (enabled ? "AÇIK" : "KAPALI"));
        if (enabled) {
            startSpeakToChatThread();
        } else {
            speakThreadRunning = false;
            currentMicDbLevel = 0;
            if (speakTriggeredActive) {
                restoreAfterSpeech();
            }
        }
        notifyListeners();
    }

    private void startSpeakToChatThread() {
        if (speakThreadRunning) return;
        speakThreadRunning = true;
        new Thread(() -> {
            int sampleRate = 16000;
            int minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (minBuf <= 0) minBuf = 2048;
            AudioRecord recorder = null;
            try {
                recorder = new AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBuf * 2
                );
                if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    addLog("⚠️ Mikrofon başlatılamadı (İzin kontrol edin).");
                    speakThreadRunning = false;
                    return;
                }
                recorder.startRecording();
                short[] buffer = new short[1024];
                int consecutiveSpeechFrames = 0;

                while (speakThreadRunning && speakToChatEnabled) {
                    int read = recorder.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        double sumSq = 0;
                        for (int i = 0; i < read; i++) {
                            sumSq += (double) buffer[i] * buffer[i];
                        }
                        double rms = Math.sqrt(sumSq / read);
                        // Convert RMS (0..32768) to a 0..100 UI scale
                        int level = (int) Math.min(100, Math.max(0, (20.0 * Math.log10(Math.max(1.0, rms))) * 1.15 - 10));
                        currentMicDbLevel = level;

                        long now = System.currentTimeMillis();
                        if (level >= speakThresholdLevel) {
                            consecutiveSpeechFrames++;
                            if (consecutiveSpeechFrames >= 3) {
                                lastSpeechDetectedTimeMs = now;
                                if (!speakTriggeredActive) {
                                    triggerSpeakToChatOn();
                                }
                            }
                        } else {
                            consecutiveSpeechFrames = 0;
                        }

                        if (speakTriggeredActive) {
                            long elapsedMs = now - lastSpeechDetectedTimeMs;
                            int rem = Math.max(0, 5 - (int) (elapsedMs / 1000L));
                            speakCountdownSec = rem;
                            if (elapsedMs >= 5000L) {
                                restoreAfterSpeech();
                            }
                        }
                        notifyListeners();
                    }
                    Thread.sleep(65);
                }
            } catch (Throwable e) {
                addLog("⚠️ Speak-to-Chat durdu: " + e.getMessage());
            } finally {
                if (recorder != null) {
                    try {
                        recorder.stop();
                        recorder.release();
                    } catch (Throwable ignored) {}
                }
                speakThreadRunning = false;
            }
        }, "X20-SpeakToChat").start();
    }

    private void triggerSpeakToChatOn() {
        speakTriggeredActive = true;
        speakCountdownSec = 5;
        savedAmbientModeBeforeSpeech = state.ambientMode;
        if (audioManager != null) {
            savedVolumeBeforeSpeech = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int duckVol = Math.max(2, (int) (maxVol * 0.22f));
            if (savedVolumeBeforeSpeech > duckVol) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, duckVol, 0);
            }
        }
        setTransparencyDetails(SoundcoreProtocol.TRANS_VOCAL_MODE, "🗣️ Konuşma Algılandı -> Vokal Şeffaf Mod");
    }

    private void restoreAfterSpeech() {
        speakTriggeredActive = false;
        speakCountdownSec = 0;
        if (audioManager != null && savedVolumeBeforeSpeech >= 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolumeBeforeSpeech, 0);
            savedVolumeBeforeSpeech = -1;
        }
        setAmbientMode(savedAmbientModeBeforeSpeech, "🗣️ Konuşma Bitti -> Önceki Moda Dönüldü");
    }
}
