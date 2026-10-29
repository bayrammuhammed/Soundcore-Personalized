package com.bayram.soundcorex20;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.NoiseSuppressor;
import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class SmartAutomationEngine implements SensorEventListener, LocationListener {

    public interface Listener {
        void onStateUpdated();
    }

    public static final int ACTIVITY_STATIONARY = 0;
    public static final int ACTIVITY_WALKING_RUNNING = 1;
    public static final int ACTIVITY_TRANSIT_FAST = 2;

    /**
     * Golden audiophile EQ curves (8 bands: 100Hz..12.8kHz in 0.1 dB units).
     * Preset 0 is the exact warm, distortion-free Cinema & Natural Stage curve for YouTube/Movies.
     */
    public static final int[][] EQ_PRESETS = new int[][]{
            {35, 25, 10, 5, 15, 20, 25, 20},          // 0: 🎬 Sinema & Doğal Sahne (YouTube / Film)
            {45, 35, 15, 0, 15, 25, 30, 25},          // 1: 🎵 Müzik + Dengeli Bass
            {15, 20, 30, 30, 25, 15, 10, 0},          // 2: 🎙️ Net Konuşma & Podcast
            {45, 35, 15, 10, 20, 30, 35, 25},         // 3: 🏃 Sport X20 Antrenman
            {35, 25, -5, 5, 15, 30, 30, 25},          // 4: 🎸 Rock & Canlı Sahne
            {0, 0, 0, 0, 0, 0, 0, 0}                  // 5: 🎻 Saf Dengeli (0.0 dB)
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
    private String lastTxHex = "-";
    private String lastRxHex = "-";

    // Smart Volume-Adaptive Cinema AudioFX
    private Equalizer systemEq;
    private LoudnessEnhancer cinemaEnhancer;
    private boolean cinemaDepthEnabled = true;
    private int currentPresetIndex = 0;

    // #1A: Auto-EQ by Media App
    private boolean autoEqByAppEnabled = true;
    private String currentMediaPackage = "com.google.android.youtube";
    private String currentMediaTitle = "YouTube Video";
    private String currentMediaArtist = "Cinema Audio";
    private boolean currentMediaPlaying = false;
    private String lastAutoEqAppliedForPkg = "";

    // #1B: Smart Step-Counter & Speed ANC
    private boolean smartActivityAncEnabled = true;
    private int currentActivityState = ACTIVITY_STATIONARY;
    private float currentMovementRms = 0.12f;
    private float currentSpeedKmh = 0.0f;
    private int totalSessionSteps = 0;
    private final List<Long> recentStepTimestamps = new ArrayList<>();
    private long lastStepDetectedMs = 0L;
    private boolean simulationOverrideActive = false;

    // #1C: Speak-to-Chat & Smart Call Assistant
    private boolean speakToChatEnabled = false;
    private volatile boolean speakThreadRunning = false;
    private int currentMicDbLevel = 0;
    private int speakThresholdLevel = 58;
    private boolean speakTriggeredActive = false;
    private int speakCountdownSec = 0;
    private int savedAmbientModeBeforeSpeech = SoundcoreProtocol.AMBIENT_ANC;
    private int savedVolumeBeforeSpeech = -1;
    private long lastSpeechDetectedTimeMs = 0L;
    private boolean inPhoneCallMode = false;

    // Master Power & Auto-Connect Controls (allows user to completely disable the app when desired)
    private final SharedPreferences prefs;
    private boolean masterAppEnabled = true;
    private boolean autoConnectOnBt = true;

    // #1D: Whisper & Call Mic Booster (VGM 15/15 + Hardware AGC + NS + AEC)
    private boolean whisperCallBoostEnabled = true;
    private int whisperGainMultiplierTenths = 32; // 3.2x boost for quiet whispers (10..50 = 1.0x..5.0x)
    private volatile boolean whisperTestLoopbackActive = false;
    private int currentWhisperInputDb = 0;
    private int currentWhisperOutputDb = 0;

    private final SensorManager sensorManager;
    private final AudioManager audioManager;

    private SmartAutomationEngine(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("soundcore_personalized_prefs", Context.MODE_PRIVATE);
        this.masterAppEnabled = prefs.getBoolean("master_app_enabled", true);
        this.autoConnectOnBt = prefs.getBoolean("auto_connect_on_bt", true);
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        addLog("🚀 Soundcore Personalized v2.0 Autonomous Engine Ready");
        registerBluetoothReceiver();
        startPeriodicWatchdog();
        if (masterAppEnabled) {
            initSystemAudioFx();
            applyBluetoothCallMicBoostParameters();
            startSensors();
            connectToEarbuds();
        } else {
            addLog("⛔ Uygulama Devre Dışı (Kullanıcı tarafından kapatıldı - Kulaklık serbest)");
        }
    }

    private void registerBluetoothReceiver() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            filter.addAction(android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            filter.addAction(android.bluetooth.BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            context.registerReceiver(new BluetoothAutoConnectReceiver(), filter);
        } catch (Throwable ignored) {
        }
    }

    public synchronized void onEarbudsDisconnected(String reason) {
        try {
            if (btSocket != null) btSocket.close();
        } catch (Throwable ignored) {
        }
        btSocket = null;
        btOut = null;
        btIn = null;
        connecting = false;
        state.connected = false;
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
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public void notifyListeners() {
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
        String lang = I18n.getActiveLang();
        if (!masterAppEnabled) {
            return I18n.pick(lang,
                    "⛔ Uygulama Kapalı • Kulaklık Serbest (Standart Mod)",
                    "⛔ App Disabled • Earbuds Free (Stock Mode)",
                    "⛔ App Deaktiviert • Kopfhörer frei",
                    "⛔ App Desactivada • Auriculares libres",
                    "⛔ App Désactivée • Écouteurs libres",
                    "⛔ Приложение выключено • Наушники свободны",
                    "⛔ التطبيق معطل • السماعة حرة");
        }
        if (state.connected) {
            return I18n.pick(lang,
                    "Bağlı • " + state.deviceName + " (RFCOMM Aktif)",
                    "Connected • " + state.deviceName + " (RFCOMM Active)",
                    "Verbunden • " + state.deviceName + " (RFCOMM Aktiv)",
                    "Conectado • " + state.deviceName + " (RFCOMM Activo)",
                    "Connecté • " + state.deviceName + " (RFCOMM Actif)",
                    "Подключено • " + state.deviceName + " (RFCOMM)",
                    "متصل • " + state.deviceName + " (RFCOMM نشط)");
        } else if (connecting) {
            return I18n.pick(lang,
                    "Bağlanılıyor • " + state.deviceName + "...",
                    "Connecting • " + state.deviceName + "...",
                    "Verbinde • " + state.deviceName + "...",
                    "Conectando • " + state.deviceName + "...",
                    "Connexion • " + state.deviceName + "...",
                    "Подключение • " + state.deviceName + "...",
                    "جاري الاتصال • " + state.deviceName + "...");
        } else {
            return I18n.pick(lang,
                    "Otomatik Bağlantı Bekleniyor (Kutudan Çıkarın)",
                    "Waiting for Auto-Connect (Take out of case)",
                    "Warte auf Auto-Verbindung",
                    "Esperando conexión automática",
                    "En attente de connexion auto",
                    "Ожидание автоподключения",
                    "في انتظار الاتصال التلقائي");
        }
    }

    public boolean isMasterAppEnabled() { return masterAppEnabled; }
    public boolean isAutoConnectOnBt() { return autoConnectOnBt; }

    public synchronized void setMasterAppEnabled(boolean enabled) {
        this.masterAppEnabled = enabled;
        prefs.edit().putBoolean("master_app_enabled", enabled).apply();
        if (!enabled) {
            whisperTestLoopbackActive = false;
            speakThreadRunning = false;
            try {
                if (systemEq != null) systemEq.setEnabled(false);
            } catch (Throwable ignored) {}
            try {
                if (cinemaEnhancer != null) cinemaEnhancer.setEnabled(false);
            } catch (Throwable ignored) {}
            try {
                if (sensorManager != null) sensorManager.unregisterListener(this);
            } catch (Throwable ignored) {}
            onEarbudsDisconnected("⏻ Uygulama Kapatıldı -> RFCOMM Bağlantısı Kesildi, Kulaklık Serbest Bırakıldı.");
        } else {
            addLog("⚡ Uygulama Yeniden Aktif Edildi -> Kulaklığa bağlanılıyor...");
            initSystemAudioFx();
            applyBluetoothCallMicBoostParameters();
            startSensors();
            connectToEarbuds();
        }
        notifyListeners();
    }

    public void setAutoConnectOnBt(boolean enabled) {
        this.autoConnectOnBt = enabled;
        prefs.edit().putBoolean("auto_connect_on_bt", enabled).apply();
        addLog(enabled
                ? "🔗 Bluetooth Otomatik Bağlantı: AÇIK"
                : "🔗 Bluetooth Otomatik Bağlantı: KAPALI (Sadece elle açıldığında bağlanır)");
        notifyListeners();
    }

    private String lastSettingsActionBanner = "";

    public String getLastSettingsActionBanner() {
        if (lastSettingsActionBanner == null || lastSettingsActionBanner.isEmpty()) {
            boolean hasFav = prefs.getBoolean("has_user_favorite", false);
            String lang = I18n.getActiveLang();
            if (hasFav) {
                return I18n.pick(lang,
                        "✅ Kayıtlı favori profiliniz hazır • İstediğiniz zaman dönebilirsiniz",
                        "✅ Saved favorite profile ready • Restore anytime",
                        "✅ Gespeichertes Favoritenprofil bereit",
                        "✅ Perfil favorito guardado listo",
                        "✅ Profil favori sauvegardé prêt",
                        "✅ Любимый профиль сохранен и готов",
                        "✅ الملف المفضل محفوظ وجاهز");
            }
            return I18n.pick(lang,
                    "✨ Aktif Profil: Altın Sinema & Otomatik Akıllı Mod (Fabrika Varsayılanı)",
                    "✨ Active Profile: Golden Cinema & Smart Auto Mode (Factory Default)",
                    "✨ Aktives Profil: Golden-Kino & Smart-Auto (Standard)",
                    "✨ Perfil Activo: Cine Dorado y Modo Inteligente (Predeterminado)",
                    "✨ Profil Actif : Cinéma Doré & Mode Auto (Par Défaut)",
                    "✨ Активный профиль: Кино и Умный авто-режим (По умолчанию)",
                    "✨ الملف النشط: السينما الذهبية والوضع الذكي (الافتراضي)");
        }
        return lastSettingsActionBanner;
    }

    /**
     * Saves the user's current EQ sliders, Cinema Depth, ANC mode, Whisper Mic gain, and automation switches
     * into SharedPreferences as their personal favorite profile.
     */
    public synchronized void saveUserFavoriteProfile() {
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean("has_user_favorite", true);
        ed.putInt("fav_preset_idx", currentPresetIndex);
        ed.putBoolean("fav_cinema_depth", cinemaDepthEnabled);
        for (int i = 0; i < 8; i++) {
            ed.putInt("fav_eq_band_" + i, state.eqBandsTenthsDb[i]);
        }
        ed.putInt("fav_ambient_mode", state.ambientMode);
        ed.putInt("fav_anc_level", state.manualAncStrength);
        ed.putBoolean("fav_wind_ns", state.windNoiseSuppression);
        ed.putInt("fav_trans_type", state.transparencyMode);
        ed.putBoolean("fav_auto_eq", autoEqByAppEnabled);
        ed.putBoolean("fav_smart_act", smartActivityAncEnabled);
        ed.putBoolean("fav_speak_chat", speakToChatEnabled);
        ed.putInt("fav_speak_thresh", speakThresholdLevel);
        ed.putBoolean("fav_whisper_boost", whisperCallBoostEnabled);
        ed.putInt("fav_whisper_gain", whisperGainMultiplierTenths);
        ed.apply();

        String lang = I18n.getActiveLang();
        lastSettingsActionBanner = I18n.pick(lang,
                "⭐ Sevdiğiniz ayarlar başarıyla kaydedildi! (EQ + ANC + Mikrofon + Otomasyon)",
                "⭐ Your favorite settings have been saved! (EQ + ANC + Mic + Automation)",
                "⭐ Ihre Lieblingseinstellungen wurden gespeichert!",
                "⭐ ¡Tus ajustes favoritos han sido guardados!",
                "⭐ Vos paramètres favoris ont été sauvegardés !",
                "⭐ Ваши любимые настройки успешно сохранены!",
                "⭐ تم حفظ إعداداتك المفضلة بنجاح!");
        addLog(lastSettingsActionBanner);
        notifyListeners();
    }

    /**
     * Restores the user's saved favorite profile from SharedPreferences (or Factory Defaults if none saved yet).
     */
    public synchronized void restoreUserFavoriteProfile() {
        if (!prefs.getBoolean("has_user_favorite", false)) {
            resetToFactoryDefaults();
            return;
        }
        this.masterAppEnabled = true;
        prefs.edit().putBoolean("master_app_enabled", true).apply();

        this.currentPresetIndex = prefs.getInt("fav_preset_idx", 0);
        this.cinemaDepthEnabled = prefs.getBoolean("fav_cinema_depth", true);
        for (int i = 0; i < 8; i++) {
            state.eqBandsTenthsDb[i] = prefs.getInt("fav_eq_band_" + i, EQ_PRESETS[0][i]);
        }
        state.ambientMode = prefs.getInt("fav_ambient_mode", SoundcoreProtocol.AMBIENT_ANC);
        state.manualAncStrength = prefs.getInt("fav_anc_level", SoundcoreProtocol.ANC_STRENGTH_STRONG);
        state.windNoiseSuppression = prefs.getBoolean("fav_wind_ns", false);
        state.transparencyMode = prefs.getInt("fav_trans_type", SoundcoreProtocol.TRANS_FULLY_TRANSPARENT);

        this.autoEqByAppEnabled = prefs.getBoolean("fav_auto_eq", true);
        this.smartActivityAncEnabled = prefs.getBoolean("fav_smart_act", true);
        this.speakToChatEnabled = prefs.getBoolean("fav_speak_chat", false);
        this.speakThresholdLevel = prefs.getInt("fav_speak_thresh", 58);
        this.whisperCallBoostEnabled = prefs.getBoolean("fav_whisper_boost", true);
        this.whisperGainMultiplierTenths = prefs.getInt("fav_whisper_gain", 32);

        applySystemAudioFx(state.eqBandsTenthsDb, cinemaDepthEnabled);
        applyBluetoothCallMicBoostParameters();
        byte[] eqPkt = SoundcoreProtocol.buildSetEqualizerPacket(0xFEFE, state.eqBandsTenthsDb);
        sendPacket(eqPkt, "📂 Restore Favorite EQ");
        setAmbientMode(state.ambientMode, "📂 Restore Favorite ANC");

        String lang = I18n.getActiveLang();
        lastSettingsActionBanner = I18n.pick(lang,
                "📂 Kayıtlı favori ayarlarınız geri yüklendi!",
                "📂 Your saved favorite settings have been restored!",
                "📂 Ihr gespeichertes Favoritenprofil wurde geladen!",
                "📂 ¡Tus ajustes favoritos han sido restaurados!",
                "📂 Vos paramètres favoris ont été restaurés !",
                "📂 Ваши любимые настройки восстановлены!",
                "📂 تمت استعادة إعداداتك المفضلة المحفوظة!");
        addLog(lastSettingsActionBanner);
        notifyListeners();
    }

    /**
     * Resets all settings, EQ sliders, ANC modes, Whisper Mic Booster, and Automation rules
     * back to the golden Factory Defaults.
     */
    public synchronized void resetToFactoryDefaults() {
        this.masterAppEnabled = true;
        this.autoConnectOnBt = true;
        prefs.edit()
                .putBoolean("master_app_enabled", true)
                .putBoolean("auto_connect_on_bt", true)
                .apply();

        this.simulationOverrideActive = false;
        this.currentPresetIndex = 0; // 🎬 Sinema & Doğal Sahne
        this.cinemaDepthEnabled = true;
        for (int i = 0; i < 8; i++) {
            state.eqBandsTenthsDb[i] = EQ_PRESETS[0][i];
        }
        state.ambientMode = SoundcoreProtocol.AMBIENT_ANC;
        state.manualAncStrength = SoundcoreProtocol.ANC_STRENGTH_STRONG;
        state.windNoiseSuppression = false;
        state.transparencyMode = SoundcoreProtocol.TRANS_FULLY_TRANSPARENT;

        this.autoEqByAppEnabled = true;
        this.smartActivityAncEnabled = true;
        this.speakToChatEnabled = false;
        this.speakThreadRunning = false;
        this.speakThresholdLevel = 58;
        this.whisperCallBoostEnabled = true;
        this.whisperGainMultiplierTenths = 32; // 3.2x
        this.whisperTestLoopbackActive = false;

        initSystemAudioFx();
        applyBluetoothCallMicBoostParameters();
        startSensors();
        if (!state.connected) {
            connectToEarbuds();
        }
        applyEqPreset(0, true, "🔄 Factory Default EQ (Sinema & Doğal Sahne)");
        setAmbientMode(SoundcoreProtocol.AMBIENT_ANC, "🔄 Factory Default ANC (Max)");

        String lang = I18n.getActiveLang();
        lastSettingsActionBanner = I18n.pick(lang,
                "🔄 Tüm ayarlar ilk günkü Altın Varsayılan (Default) değerlere sıfırlandı!",
                "🔄 All settings reset to Golden Factory Defaults!",
                "🔄 Alle Einstellungen auf Werksstandard zurückgesetzt!",
                "🔄 ¡Todos los ajustes restablecidos a los valores predeterminados!",
                "🔄 Tous les paramètres ont été réinitialisés par défaut !",
                "🔄 Все настройки сброшены к заводским значениям по умолчанию!",
                "🔄 تمت إعادة تعيين جميع الإعدادات إلى الوضع الافتراضي الأصلي!");
        addLog(lastSettingsActionBanner);
        notifyListeners();
    }

    public String getLastTxHex() { return lastTxHex; }
    public String getLastRxHex() { return lastRxHex; }
    public boolean isBassUpEnabled() { return cinemaDepthEnabled; }
    public int getCurrentPresetIndex() { return currentPresetIndex; }
    public boolean isAutoEqByAppEnabled() { return autoEqByAppEnabled; }
    public boolean isSmartActivityAncEnabled() { return smartActivityAncEnabled; }
    public boolean isSpeakToChatEnabled() { return speakToChatEnabled; }
    public String getCurrentMediaPackage() { return currentMediaPackage; }
    public String getCurrentMediaTitle() { return currentMediaTitle; }
    public String getCurrentMediaArtist() { return currentMediaArtist; }
    public int getCurrentActivityState() { return currentActivityState; }
    public float getCurrentMovementRms() { return currentMovementRms; }
    public float getCurrentSpeedKmh() { return currentSpeedKmh; }
    public int getTotalSessionSteps() { return totalSessionSteps; }
    public int getRecentStepsCount() {
        pruneOldSteps(System.currentTimeMillis());
        return recentStepTimestamps.size();
    }
    public int getCurrentMicDbLevel() { return currentMicDbLevel; }
    public int getSpeakThresholdLevel() { return speakThresholdLevel; }
    public boolean isSpeakTriggeredActive() { return speakTriggeredActive; }
    public int getSpeakCountdownSec() { return speakCountdownSec; }
    public boolean isWhisperCallBoostEnabled() { return whisperCallBoostEnabled; }
    public int getWhisperGainMultiplierTenths() { return whisperGainMultiplierTenths; }
    public boolean isWhisperTestLoopbackActive() { return whisperTestLoopbackActive; }
    public int getCurrentWhisperInputDb() { return currentWhisperInputDb; }
    public int getCurrentWhisperOutputDb() { return currentWhisperOutputDb; }
    public boolean isInPhoneCallMode() { return inPhoneCallMode; }

    public void setSpeakThresholdLevel(int level) {
        this.speakThresholdLevel = Math.max(25, Math.min(90, level));
        notifyListeners();
    }

    /**
     * Volume-Adaptive Cinema AudioFX:
     * Gives the exact warm, movie-theater feel from v1.0 without ever distorting at high volume.
     */
    private void initSystemAudioFx() {
        try {
            systemEq = new Equalizer(100, 0);
            systemEq.setEnabled(true);
        } catch (Throwable ignored) {
        }
        try {
            cinemaEnhancer = new LoudnessEnhancer(0);
            cinemaEnhancer.setTargetGain(350);
            cinemaEnhancer.setEnabled(cinemaDepthEnabled);
        } catch (Throwable ignored) {
        }
        applySystemAudioFx(EQ_PRESETS[0], cinemaDepthEnabled);
    }

    private void applySystemAudioFx(int[] bands8TenthsDb, boolean cinemaDepth) {
        try {
            float volRatio = 0.5f;
            if (audioManager != null) {
                int cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int max = Math.max(1, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                volRatio = (float) cur / max;
            }
            // Dynamic head-room scaling: full cinema warmth at low/medium volume, gentle ceiling at high volume
            float headroomScale = (volRatio > 0.75f) ? 0.65f : 1.0f;

            if (systemEq != null) {
                systemEq.setEnabled(true);
                short numBands = systemEq.getNumberOfBands();
                short[] range = systemEq.getBandLevelRange();
                short minMb = range[0];
                short maxMb = range[1];
                for (short b = 0; b < numBands; b++) {
                    int idx = Math.min(7, (b * 8) / numBands);
                    int mb = Math.round(bands8TenthsDb[idx] * 9.5f * headroomScale);
                    if (cinemaDepth && b == 0) {
                        mb += Math.round(240 * headroomScale);
                    }
                    short clamped = (short) Math.max(minMb, Math.min(maxMb, mb));
                    systemEq.setBandLevel(b, clamped);
                }
            }
            if (cinemaEnhancer != null) {
                int targetGainMb = cinemaDepth ? Math.round(380 * headroomScale) : 0;
                cinemaEnhancer.setTargetGain(targetGainMb);
                cinemaEnhancer.setEnabled(cinemaDepth);
            }
        } catch (Throwable ignored) {
        }
    }

    public synchronized void connectToEarbuds() {
        if (!masterAppEnabled) {
            return;
        }
        if (connecting || (btSocket != null && btSocket.isConnected())) {
            if (btSocket != null && btSocket.isConnected()) {
                sendPacket(SoundcoreProtocol.buildRequestStatePacket(), "State Sync [01 01]");
            }
            return;
        }
        connecting = true;
        notifyListeners();

        new Thread(() -> {
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter == null || !adapter.isEnabled()) {
                    connecting = false;
                    notifyListeners();
                    return;
                }

                BluetoothDevice target = null;
                Set<BluetoothDevice> bonded = adapter.getBondedDevices();
                if (bonded != null) {
                    for (BluetoothDevice d : bonded) {
                        if (SoundcoreProtocol.isSoundcoreDevice(d)) {
                            try {
                                Method m = d.getClass().getMethod("isConnected");
                                Object res = m.invoke(d);
                                if (res instanceof Boolean && (Boolean) res) {
                                    target = d;
                                    break;
                                }
                            } catch (Throwable ignored) {}
                            if (target == null) target = d;
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
                UUID dynamicUuid = SoundcoreProtocol.findDeviceRfcommUuid(target);

                BluetoothSocket sock = null;
                try {
                    sock = target.createRfcommSocketToServiceRecord(dynamicUuid);
                    sock.connect();
                } catch (Throwable e1) {
                    try { if (sock != null) sock.close(); } catch (Throwable ignored) {}
                    try {
                        sock = target.createInsecureRfcommSocketToServiceRecord(dynamicUuid);
                        sock.connect();
                    } catch (Throwable e2) {
                        try { if (sock != null) sock.close(); } catch (Throwable ignored) {}
                        try {
                            Method m = target.getClass().getMethod("createRfcommSocket", int.class);
                            sock = (BluetoothSocket) m.invoke(target, 10);
                            if (sock != null) sock.connect();
                        } catch (Throwable e3) {
                            try { if (sock != null) sock.close(); } catch (Throwable ignored) {}
                            sock = target.createRfcommSocketToServiceRecord(SoundcoreProtocol.SPP_FALLBACK_UUID);
                            sock.connect();
                        }
                    }
                }

                btSocket = sock;
                btOut = sock.getOutputStream();
                btIn = sock.getInputStream();
                state.connected = true;
                addLog("✅ Connected: " + state.deviceName + " (" + state.deviceMac + ")");
                connecting = false;
                notifyListeners();

                startReaderThread();
                Thread.sleep(220);
                sendPacket(SoundcoreProtocol.buildRequestStatePacket(), "State Query [01 01]");
                Thread.sleep(320);
                applyEqPreset(currentPresetIndex, cinemaDepthEnabled, "🎬 Cinema Stage Ready");

            } catch (Throwable e) {
                connecting = false;
                state.connected = false;
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
                        addLog("📥 RX Packet: " + lastRxHex);
                    }
                    notifyListeners();
                } catch (Throwable e) {
                    break;
                }
            }
            state.connected = false;
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
            } catch (Throwable ignored) {
            }
        }).start();
    }

    // ==================== SOUND MODES ====================

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
        sendPacket(pkt, reason);
        notifyListeners();
    }

    public void setAncDetails(int ncMode, int manualStrength, boolean windSuppression, String reason) {
        state.ambientMode = SoundcoreProtocol.AMBIENT_ANC;
        state.ncMode = ncMode;
        state.manualAncStrength = manualStrength;
        state.windNoiseSuppression = windSuppression;
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
        sendPacket(pkt, reason);
        notifyListeners();
    }

    public void setTransparencyDetails(int transMode, String reason) {
        state.ambientMode = SoundcoreProtocol.AMBIENT_TRANSPARENCY;
        state.transparencyMode = transMode;
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
        sendPacket(pkt, reason);
        notifyListeners();
    }

    // ==================== EQUALIZER & CINEMA DEPTH ====================

    public void applyEqPreset(int presetIndex, boolean enableCinemaDepth, String reason) {
        if (presetIndex < 0 || presetIndex >= EQ_PRESETS.length) return;
        this.currentPresetIndex = presetIndex;
        this.cinemaDepthEnabled = enableCinemaDepth;
        int[] bands = new int[8];
        for (int i = 0; i < 8; i++) {
            bands[i] = EQ_PRESETS[presetIndex][i];
            state.eqBandsTenthsDb[i] = bands[i];
        }
        applySystemAudioFx(bands, enableCinemaDepth);
        int hwPresetId = (presetIndex == 5) ? 0x0000 : 0xFEFE;
        byte[] pkt = SoundcoreProtocol.buildSetEqualizerPacket(hwPresetId, bands);
        String[] names = I18n.getPresetNames();
        sendPacket(pkt, reason + " [" + names[presetIndex] + "]");
        notifyListeners();
    }

    public void setCustomEqBand(int bandIndex, int tenthsDb) {
        if (bandIndex < 0 || bandIndex >= 8) return;
        state.eqBandsTenthsDb[bandIndex] = Math.max(-120, Math.min(120, tenthsDb));
        applySystemAudioFx(state.eqBandsTenthsDb, cinemaDepthEnabled);
        byte[] pkt = SoundcoreProtocol.buildSetEqualizerPacket(0xFEFE, state.eqBandsTenthsDb);
        sendPacket(pkt, "EQ Band " + (bandIndex + 1) + " (" + (tenthsDb / 10.0f) + " dB)");
        notifyListeners();
    }

    public void setBassUpEnabled(boolean enabled) {
        this.cinemaDepthEnabled = enabled;
        applyEqPreset(currentPresetIndex, enabled, "🎬 Cinema Depth: " + (enabled ? "ON" : "OFF"));
    }

    // ==================== #1A: SMART CINEMA & MEDIA AUTO-SOUND ====================

    public void setAutoEqByAppEnabled(boolean enabled) {
        this.autoEqByAppEnabled = enabled;
        if (enabled) {
            lastAutoEqAppliedForPkg = "";
            evaluateAutoEqForCurrentMedia();
        }
        notifyListeners();
    }

    private void startPeriodicWatchdog() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (masterAppEnabled) {
                    MediaNotificationListener.pollActiveMediaSessions(context);
                    checkPhoneCallState();
                    evaluateIdleStepReturnToAnc();
                    if (autoConnectOnBt && !state.connected && !connecting && isEarbudAclConnected()) {
                        connectToEarbuds();
                    }
                }
                mainHandler.postDelayed(this, 2500);
            }
        }, 1500);
    }

    private void checkPhoneCallState() {
        if (audioManager == null) return;
        int mode = audioManager.getMode();
        boolean callActive = (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_RINGTONE);
        if (callActive && !inPhoneCallMode) {
            inPhoneCallMode = true;
            savedAmbientModeBeforeSpeech = state.ambientMode;
            if (whisperCallBoostEnabled) {
                applyBluetoothCallMicBoostParameters();
                // Enable Wind Noise Suppression + Vocal Mode on Earbuds DSP so dual-mics focus on user's voice
                state.windNoiseSuppression = true;
                setTransparencyDetails(SoundcoreProtocol.TRANS_VOCAL_MODE,
                        "📞 Call Active -> Whisper Mic Boost (VGM 15/15 + AGC + Wind Filter)");
            } else {
                setTransparencyDetails(SoundcoreProtocol.TRANS_VOCAL_MODE, "📞 Smart Call Assistant -> Vocal Mode");
            }
        } else if (!callActive && inPhoneCallMode) {
            inPhoneCallMode = false;
            state.windNoiseSuppression = false;
            setAmbientMode(savedAmbientModeBeforeSpeech, "📞 Call Ended -> Restored ANC");
        }
    }

    // ==================== #1D: WHISPER & CALL MIC BOOSTER (HFP VGM 15 + AGC + NS) ====================

    public void setWhisperCallBoostEnabled(boolean enabled) {
        this.whisperCallBoostEnabled = enabled;
        if (enabled) {
            applyBluetoothCallMicBoostParameters();
            addLog("🎙️ Whisper Call Mic Booster ON (HFP VGM=15/15, Wideband mSBC, Hardware AGC+NS)");
        } else {
            addLog("🎙️ Whisper Call Mic Booster OFF");
        }
        notifyListeners();
    }

    public void setWhisperGainMultiplierTenths(int tenths) {
        this.whisperGainMultiplierTenths = Math.max(10, Math.min(50, tenths));
        applyBluetoothCallMicBoostParameters();
        notifyListeners();
    }

    /**
     * Configures the Android Audio HAL and Bluetooth HFP stack for maximum microphone sensitivity
     * (fixes mMicVolume=0 in HeadsetStateMachine -> sets VGM=15/15, enables Wideband Speech & AGC).
     */
    public void applyBluetoothCallMicBoostParameters() {
        if (audioManager == null || !whisperCallBoostEnabled) return;
        try {
            audioManager.setMicrophoneMute(false);
            // Boost HFP microphone gain (VGM) from 0 to 15 (max), enable Wideband Speech (16kHz mSBC),
            // dual-mic noise suppression, and hardware Automatic Gain Control
            audioManager.setParameters(
                    "bt_headset_vgm=15;" +
                    "bt_headset_vgs=15;" +
                    "bt_wbs=on;" +
                    "noise_suppression=on;" +
                    "aec_mode=on;" +
                    "agc_enabled=true;" +
                    "fluence=broadside;" +
                    "mic_gain_level=high"
            );
            int maxCallVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
            int curCallVol = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            if (curCallVol < (maxCallVol * 4) / 5) {
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, (maxCallVol * 4) / 5, 0);
            }
        } catch (Throwable ignored) {
        }
    }

    public void toggleWhisperTestLoopback() {
        if (whisperTestLoopbackActive) {
            whisperTestLoopbackActive = false;
            currentWhisperInputDb = 0;
            currentWhisperOutputDb = 0;
            addLog("🛑 Whisper Mic Test Stopped");
            notifyListeners();
        } else {
            startWhisperTestLoopbackThread();
        }
    }

    private void startWhisperTestLoopbackThread() {
        if (whisperTestLoopbackActive) return;
        whisperTestLoopbackActive = true;
        applyBluetoothCallMicBoostParameters();
        addLog("🎧 Live Whisper Test Started (Hardware AGC + NoiseSuppressor + " + (whisperGainMultiplierTenths / 10.0f) + "x Pre-Amp)");
        notifyListeners();

        new Thread(() -> {
            int sampleRate = 16000;
            int minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (minBuf <= 0) minBuf = 1280;
            AudioRecord rec = null;
            AudioTrack track = null;
            AutomaticGainControl agc = null;
            NoiseSuppressor ns = null;
            AcousticEchoCanceler aec = null;

            try {
                rec = new AudioRecord(
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBuf * 2
                );
                int sessionId = rec.getAudioSessionId();
                if (AutomaticGainControl.isAvailable()) {
                    agc = AutomaticGainControl.create(sessionId);
                    if (agc != null) agc.setEnabled(true);
                }
                if (NoiseSuppressor.isAvailable()) {
                    ns = NoiseSuppressor.create(sessionId);
                    if (ns != null) ns.setEnabled(true);
                }
                if (AcousticEchoCanceler.isAvailable()) {
                    aec = AcousticEchoCanceler.create(sessionId);
                    if (aec != null) aec.setEnabled(true);
                }

                track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(minBuf * 2)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build();

                rec.startRecording();
                track.play();

                short[] inBuf = new short[320]; // 20ms frame at 16kHz
                short[] outBuf = new short[320];
                double envelope = 1.0;

                while (whisperTestLoopbackActive) {
                    int n = rec.read(inBuf, 0, inBuf.length);
                    if (n > 0) {
                        double sumIn = 0;
                        double sumOut = 0;
                        float baseGain = whisperGainMultiplierTenths / 10.0f;

                        for (int i = 0; i < n; i++) {
                            double sample = inBuf[i];
                            sumIn += sample * sample;

                            // Smart Whisper Dynamic Compressor:
                            // Boosts quiet whispers strongly, smoothly compresses loud peaks so it never clips
                            double absVal = Math.abs(sample) / 32768.0;
                            envelope = envelope * 0.92 + absVal * 0.08;
                            double dynamicGain = baseGain;
                            if (envelope < 0.08) {
                                // Whisper zone -> extra clarity boost
                                dynamicGain = baseGain * 1.45;
                            } else if (envelope > 0.35) {
                                // Loud speech -> soft limiter
                                dynamicGain = 1.15;
                            }

                            // Noise gate for dead silence (< 80 PCM amplitude) to eliminate background hiss
                            double boosted = (Math.abs(sample) < 75) ? (sample * 0.4) : (sample * dynamicGain);
                            // Soft-knee tanh-like saturation limiter
                            if (boosted > 28000) boosted = 28000 + (boosted - 28000) * 0.15;
                            if (boosted < -28000) boosted = -28000 + (boosted + 28000) * 0.15;
                            short clamped = (short) Math.max(-32700, Math.min(32700, (int) boosted));
                            outBuf[i] = clamped;
                            sumOut += (double) clamped * clamped;
                        }

                        track.write(outBuf, 0, n);

                        double rmsIn = Math.sqrt(sumIn / n);
                        double rmsOut = Math.sqrt(sumOut / n);
                        currentWhisperInputDb = (int) Math.min(100, Math.max(0, 20.0 * Math.log10(Math.max(1.0, rmsIn)) * 1.15 - 10));
                        currentWhisperOutputDb = (int) Math.min(100, Math.max(0, 20.0 * Math.log10(Math.max(1.0, rmsOut)) * 1.15 - 10));
                        notifyListeners();
                    }
                }
            } catch (Throwable e) {
                addLog("⚠️ Whisper Test Error: " + e.getMessage());
            } finally {
                whisperTestLoopbackActive = false;
                if (agc != null) try { agc.release(); } catch (Throwable ignored) {}
                if (ns != null) try { ns.release(); } catch (Throwable ignored) {}
                if (aec != null) try { aec.release(); } catch (Throwable ignored) {}
                if (rec != null) try { rec.stop(); rec.release(); } catch (Throwable ignored) {}
                if (track != null) try { track.stop(); track.release(); } catch (Throwable ignored) {}
                notifyListeners();
            }
        }, "X20-WhisperBoostTest").start();
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
            // Also refresh volume-adaptive headroom whenever media state updates
            applySystemAudioFx(state.eqBandsTenthsDb, cinemaDepthEnabled);
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
            applyEqPreset(1, true, "🤖 Auto-EQ (Music)");
        } else if (pkg.contains("podcast") || pkg.contains("audible")) {
            applyEqPreset(2, false, "🤖 Auto-EQ (Podcast)");
        } else {
            // YouTube / Movies / General -> Warm Cinema & Natural Stage (Preset 0) + Cinema Depth ON
            applyEqPreset(0, true, "🤖 Auto-EQ (Cinema Stage)");
        }
    }

    // ==================== #1B: AUTO STEP-COUNTER & SPEED-AWARE ANC ====================

    public void setSmartActivityAncEnabled(boolean enabled) {
        this.smartActivityAncEnabled = enabled;
        this.simulationOverrideActive = false;
        notifyListeners();
    }

    private void startSensors() {
        try {
            if (sensorManager != null) {
                Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
                if (accel == null) accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (accel != null) {
                    sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
                }
                Sensor stepDet = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
                if (stepDet != null) {
                    sensorManager.registerListener(this, stepDet, SensorManager.SENSOR_DELAY_NORMAL);
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

    private synchronized void recordDetectedStep(long nowMs) {
        if (nowMs - lastStepDetectedMs < 260L) return; // debounce >230 spm
        lastStepDetectedMs = nowMs;
        totalSessionSteps++;
        recentStepTimestamps.add(nowMs);
        pruneOldSteps(nowMs);

        // If user takes 8+ steps within the 12-second window -> automatically switch to Walking Transparency!
        if (smartActivityAncEnabled && !simulationOverrideActive && !speakTriggeredActive && !inPhoneCallMode) {
            if (recentStepTimestamps.size() >= 8 && currentActivityState != ACTIVITY_WALKING_RUNNING && currentSpeedKmh < 18.0f) {
                applyActivityStateRule(ACTIVITY_WALKING_RUNNING, "🚶 Auto Step Engine (" + recentStepTimestamps.size() + " steps)");
            }
        }
        notifyListeners();
    }

    private synchronized void pruneOldSteps(long nowMs) {
        Iterator<Long> it = recentStepTimestamps.iterator();
        while (it.hasNext()) {
            if (nowMs - it.next() > 12000L) {
                it.remove();
            }
        }
    }

    private void evaluateIdleStepReturnToAnc() {
        if (!smartActivityAncEnabled || simulationOverrideActive || speakTriggeredActive || inPhoneCallMode) return;
        long now = System.currentTimeMillis();
        pruneOldSteps(now);
        if (currentActivityState == ACTIVITY_WALKING_RUNNING
                && recentStepTimestamps.isEmpty()
                && currentMovementRms < 0.8f
                && currentSpeedKmh < 2.5f
                && (now - lastStepDetectedMs > 12000L)) {
            applyActivityStateRule(ACTIVITY_STATIONARY, "🪑 Auto Step Engine (Stationary 12s -> Max ANC)");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) return;
        long now = System.currentTimeMillis();

        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            recordDetectedStep(now);
            return;
        }

        if (event.values == null || event.values.length < 3) return;
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        double mag = Math.sqrt(x * x + y * y + z * z);
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mag = Math.abs(mag - SensorManager.GRAVITY_EARTH);
        }
        currentMovementRms = (float) (currentMovementRms * 0.85 + mag * 0.15);

        // Accelerometer peak step detector fallback (triggers when rhythmic walking acceleration > 2.1 m/s²)
        if (mag > 2.15 && (now - lastStepDetectedMs > 360L)) {
            recordDetectedStep(now);
        }

        if (smartActivityAncEnabled && !simulationOverrideActive && !speakTriggeredActive && !inPhoneCallMode) {
            if (currentSpeedKmh >= 18.0f && currentActivityState != ACTIVITY_TRANSIT_FAST) {
                applyActivityStateRule(ACTIVITY_TRANSIT_FAST, "🚆 High Speed Detected -> Wind ANC");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onLocationChanged(Location location) {
        if (location != null && location.hasSpeed()) {
            currentSpeedKmh = location.getSpeed() * 3.6f;
            notifyListeners();
        }
    }

    public void simulateActivityState(int newState) {
        this.simulationOverrideActive = true;
        if (newState == ACTIVITY_WALKING_RUNNING) {
            totalSessionSteps += 8;
        }
        applyActivityStateRule(newState, "⚡ Smart Step Test");
    }

    private void applyActivityStateRule(int newState, String prefix) {
        this.currentActivityState = newState;
        if (newState == ACTIVITY_STATIONARY) {
            setAncDetails(SoundcoreProtocol.NC_MODE_MANUAL, SoundcoreProtocol.ANC_STRENGTH_STRONG, false,
                    prefix + " -> Max ANC");
        } else if (newState == ACTIVITY_WALKING_RUNNING) {
            setTransparencyDetails(SoundcoreProtocol.TRANS_FULLY_TRANSPARENT,
                    prefix + " -> Transparency");
            applyEqPreset(3, true, prefix + " -> Sport EQ");
        } else if (newState == ACTIVITY_TRANSIT_FAST) {
            setAncDetails(SoundcoreProtocol.NC_MODE_MANUAL, SoundcoreProtocol.ANC_STRENGTH_STRONG, true,
                    prefix + " -> Wind ANC");
        }
        notifyListeners();
    }

    // ==================== #1C: SPEAK-TO-CHAT ====================

    public void setSpeakToChatEnabled(boolean enabled) {
        this.speakToChatEnabled = enabled;
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
                            speakCountdownSec = Math.max(0, 5 - (int) (elapsedMs / 1000L));
                            if (elapsedMs >= 5000L) {
                                restoreAfterSpeech();
                            }
                        }
                        notifyListeners();
                    }
                    Thread.sleep(65);
                }
            } catch (Throwable ignored) {
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
        setTransparencyDetails(SoundcoreProtocol.TRANS_VOCAL_MODE, "🗣️ Speak-to-Chat -> Vocal Mode");
    }

    private void restoreAfterSpeech() {
        speakTriggeredActive = false;
        speakCountdownSec = 0;
        if (audioManager != null && savedVolumeBeforeSpeech >= 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolumeBeforeSpeech, 0);
            savedVolumeBeforeSpeech = -1;
        }
        setAmbientMode(savedAmbientModeBeforeSpeech, "🗣️ Speech Ended -> Restored ANC");
    }
}
