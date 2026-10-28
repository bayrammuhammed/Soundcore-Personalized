package com.bayram.soundcorex20;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements SmartAutomationEngine.Listener {

    private SmartAutomationEngine engine;

    // UI References
    private TextView tvConnStatus;
    private TextView tvModelName;
    private TextView tvDeviceMeta;
    private TextView btnGrantNotifAccess;
    private TextView tvLeftBatt;
    private TextView tvRightBatt;
    private TextView tvCaseBatt;
    private ProgressBar pbLeftBatt;
    private ProgressBar pbRightBatt;
    private ProgressBar pbCaseBatt;

    // Sound Mode Buttons
    private TextView btnModeAnc;
    private TextView btnModeTrans;
    private TextView btnModeNormal;
    private LinearLayout llAncSubOptions;
    private LinearLayout llTransSubOptions;
    private TextView btnAncWeak, btnAncMod, btnAncStrong, btnAncAdaptive, btnWindNoise;
    private TextView btnTransFull, btnTransVocal;

    // #1A Auto-EQ UI
    private Switch swAutoEq;
    private TextView tvNowPlayingInfo;
    private TextView tvAutoEqRuleStatus;

    // #1B Smart Activity UI
    private Switch swSmartActivity;
    private TextView tvActivityTelemetry;
    private TextView btnActStationary, btnActWalking, btnActTransit;

    // #1C Speak-to-Chat UI
    private Switch swSpeakToChat;
    private TextView tvSpeakStatus;
    private ProgressBar pbMicLevel;
    private TextView tvThresholdVal;

    // Equalizer & BassUp UI
    private Switch swBassUp;
    private final TextView[] presetButtons = new TextView[6];
    private final SeekBar[] eqSeekBars = new SeekBar[8];
    private final TextView[] eqValueLabels = new TextView[8];
    private boolean updatingUiProgrammatically = false;

    // Telemetry Console UI
    private TextView tvTxRxHex;
    private TextView tvTelemetryLogs;

    private static final String[] BAND_LABELS = {
            "100Hz", "200Hz", "400Hz", "800Hz", "1.6kHz", "3.2kHz", "6.4kHz", "12.8kHz"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(Color.parseColor("#080B14"));
        w.setNavigationBarColor(Color.parseColor("#080B14"));

        requestNeededPermissions();

        engine = SmartAutomationEngine.getInstance(this);
        setContentView(buildRootLayout());
        engine.addListener(this);
        onStateUpdated();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (engine != null) {
            engine.addListener(this);
            MediaNotificationListener.pollActiveMediaSessions(this);
            onStateUpdated();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (engine != null) {
            engine.removeListener(this);
        }
    }

    private void requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] perms = new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
            boolean need = false;
            for (String p : perms) {
                if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                    need = true;
                    break;
                }
            }
            if (need) {
                requestPermissions(perms, 101);
            }
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private GradientDrawable roundedBox(String fillColorHex, String strokeColorHex, int cornerDp, int strokeDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(fillColorHex));
        gd.setCornerRadius(dp(cornerDp));
        if (strokeColorHex != null && strokeDp > 0) {
            gd.setStroke(dp(strokeDp), Color.parseColor(strokeColorHex));
        }
        return gd;
    }

    private View buildRootLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#080B14"));
        scroll.setFillViewport(true);
        scroll.setFitsSystemWindows(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFocusable(true);
        root.setFocusableInTouchMode(true);
        root.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        root.setPadding(dp(16), dp(14), dp(16), dp(36));
        scroll.addView(root);
        scroll.post(() -> scroll.scrollTo(0, 0));

        // 1. HEADER BAR
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(titleCol, titleLp);

        TextView tvAppTitle = new TextView(this);
        tvAppTitle.setText("SOUNDCORE • PERSONALIZED");
        tvAppTitle.setTextColor(Color.parseColor("#00E5FF"));
        tvAppTitle.setTextSize(19);
        tvAppTitle.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(tvAppTitle);

        tvConnStatus = new TextView(this);
        tvConnStatus.setText("Bağlanıyor...");
        tvConnStatus.setTextColor(Color.parseColor("#94A3B8"));
        tvConnStatus.setTextSize(12);
        titleCol.addView(tvConnStatus);

        TextView btnReconnect = new TextView(this);
        btnReconnect.setText("🔄 Senkronize Et");
        btnReconnect.setTextColor(Color.parseColor("#00E5FF"));
        btnReconnect.setTextSize(12);
        btnReconnect.setTypeface(Typeface.DEFAULT_BOLD);
        btnReconnect.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnReconnect.setBackground(roundedBox("#112233", "#00E5FF", 12, 1));
        btnReconnect.setOnClickListener(v -> engine.connectToEarbuds());
        header.addView(btnReconnect);

        // Self-service Notification Listener permission banner (only visible on phones without ADB grant)
        btnGrantNotifAccess = new TextView(this);
        btnGrantNotifAccess.setText("🔓 Otomatik Medya Algılama İznini Aç (Tek Dokunuşla Onayla)");
        btnGrantNotifAccess.setTextColor(Color.parseColor("#080B14"));
        btnGrantNotifAccess.setTextSize(12);
        btnGrantNotifAccess.setTypeface(Typeface.DEFAULT_BOLD);
        btnGrantNotifAccess.setGravity(Gravity.CENTER);
        btnGrantNotifAccess.setPadding(dp(12), dp(10), dp(12), dp(10));
        btnGrantNotifAccess.setBackground(roundedBox("#FBBF24", "#FFFFFF", 12, 1));
        LinearLayout.LayoutParams gnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gnLp.topMargin = dp(10);
        btnGrantNotifAccess.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            } catch (Throwable ignored) {
            }
        });
        root.addView(btnGrantNotifAccess, gnLp);

        // 2. HERO DEVICE CARD (Sport X20 Earbuds + Battery Telemetry)
        root.addView(buildHeroDeviceCard());

        // 3. MANUAL ANC / TRANSPARENCY / NORMAL CONTROL CARD
        root.addView(buildSoundModesCard());

        // 4. SECTION TITLE: #1 AUTONOMOUS MODES
        TextView tvSec1 = new TextView(this);
        tvSec1.setText("🧠 #1 OTONOM & BAĞLAM DUYARLI MODLAR");
        tvSec1.setTextColor(Color.parseColor("#E2E8F0"));
        tvSec1.setTextSize(14);
        tvSec1.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams secLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        secLp.topMargin = dp(18);
        secLp.bottomMargin = dp(8);
        root.addView(tvSec1, secLp);

        // 4A. Per-App & Song Auto-EQ Card
        root.addView(buildAutoEqCard());

        // 4B. Smart Activity & Speed ANC Card
        root.addView(buildSmartActivityCard());

        // 4C. Speak-to-Chat (Voice Triggered Transparency) Card
        root.addView(buildSpeakToChatCard());

        // 5. 8-BAND HARDWARE EQUALIZER & BASSUP STUDIO
        root.addView(buildEqualizerStudioCard());

        // 6. IOT & BLUETOOTH RFCOMM PACKET CONSOLE
        root.addView(buildTelemetryConsoleCard());

        return scroll;
    }

    private View buildHeroDeviceCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(roundedBox("#11172A", "#1E293B", 20, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        card.setLayoutParams(lp);

        // Earbud images + model metadata row
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(topRow);

        ImageView imgLeft = new ImageView(this);
        imgLeft.setImageResource(R.drawable.a3968_device_line_l);
        imgLeft.setScaleType(ImageView.ScaleType.FIT_CENTER);
        topRow.addView(imgLeft, new LinearLayout.LayoutParams(dp(58), dp(58)));

        ImageView imgRight = new ImageView(this);
        imgRight.setImageResource(R.drawable.a3968_device_line_r);
        imgRight.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(dp(58), dp(58));
        rLp.leftMargin = dp(4);
        topRow.addView(imgRight, rLp);

        LinearLayout metaCol = new LinearLayout(this);
        metaCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mLp.leftMargin = dp(14);
        topRow.addView(metaCol, mLp);

        tvModelName = new TextView(this);
        tvModelName.setText("soundcore Sport X20");
        tvModelName.setTextColor(Color.WHITE);
        tvModelName.setTextSize(17);
        tvModelName.setTypeface(Typeface.DEFAULT_BOLD);
        metaCol.addView(tvModelName);

        tvDeviceMeta = new TextView(this);
        tvDeviceMeta.setText("Model: A3968 • MAC: B0:38:E2:FC:55:7E\nFW: v01.65 • Kodek: AAC Stereo");
        tvDeviceMeta.setTextColor(Color.parseColor("#94A3B8"));
        tvDeviceMeta.setTextSize(11);
        metaCol.addView(tvDeviceMeta);

        // Battery Row (Left, Right, Case)
        LinearLayout battRow = new LinearLayout(this);
        battRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bRowLp.topMargin = dp(14);
        card.addView(battRow, bRowLp);

        pbLeftBatt = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        tvLeftBatt = new TextView(this);
        battRow.addView(buildBatteryBox("SOL KULAKLIK", tvLeftBatt, pbLeftBatt, "#00E5FF"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        View spacer1 = new View(this);
        battRow.addView(spacer1, new LinearLayout.LayoutParams(dp(8), 1));

        pbRightBatt = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        tvRightBatt = new TextView(this);
        battRow.addView(buildBatteryBox("SAĞ KULAKLIK", tvRightBatt, pbRightBatt, "#00E5FF"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        View spacer2 = new View(this);
        battRow.addView(spacer2, new LinearLayout.LayoutParams(dp(8), 1));

        pbCaseBatt = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        tvCaseBatt = new TextView(this);
        battRow.addView(buildBatteryBox("ŞARJ KUTUSU", tvCaseBatt, pbCaseBatt, "#A855F7"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return card;
    }

    private LinearLayout buildBatteryBox(String label, TextView tvVal, ProgressBar pb, String accentHex) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(8), dp(10), dp(8));
        box.setBackground(roundedBox("#182238", "#263554", 12, 1));

        TextView tvLbl = new TextView(this);
        tvLbl.setText(label);
        tvLbl.setTextColor(Color.parseColor("#94A3B8"));
        tvLbl.setTextSize(10);
        tvLbl.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(tvLbl);

        tvVal.setText("%100");
        tvVal.setTextColor(Color.WHITE);
        tvVal.setTextSize(15);
        tvVal.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(tvVal);

        pb.setMax(100);
        pb.setProgress(100);
        pb.setProgressTintList(ColorStateList.valueOf(Color.parseColor(accentHex)));
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        pbLp.topMargin = dp(4);
        box.addView(pb, pbLp);
        return box;
    }

    private View buildSoundModesCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundedBox("#11172A", "#1E293B", 20, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        card.setLayoutParams(lp);

        TextView title = new TextView(this);
        title.setText("🎧 AKTİF GÜRÜLTÜ KONTROLÜ (DONANIM DSP)");
        title.setTextColor(Color.WHITE);
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams mrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mrLp.topMargin = dp(10);
        card.addView(modeRow, mrLp);

        btnModeAnc = createPillButton("🛡️ ANC\nGürültü Engelle");
        btnModeTrans = createPillButton("👂 Şeffaf Mod\nDış Ses Açık");
        btnModeNormal = createPillButton("🎵 Normal\nPasif Mod");

        btnModeAnc.setOnClickListener(v -> engine.setAmbientMode(SoundcoreProtocol.AMBIENT_ANC, "Manuel Seçim"));
        btnModeTrans.setOnClickListener(v -> engine.setAmbientMode(SoundcoreProtocol.AMBIENT_TRANSPARENCY, "Manuel Seçim"));
        btnModeNormal.setOnClickListener(v -> engine.setAmbientMode(SoundcoreProtocol.AMBIENT_NORMAL, "Manuel Seçim"));

        modeRow.addView(btnModeAnc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        View s1 = new View(this);
        modeRow.addView(s1, new LinearLayout.LayoutParams(dp(8), 1));
        modeRow.addView(btnModeTrans, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        View s2 = new View(this);
        modeRow.addView(s2, new LinearLayout.LayoutParams(dp(8), 1));
        modeRow.addView(btnModeNormal, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // ANC Sub-options (Weak / Moderate / Strong / Adaptive / Wind Noise)
        llAncSubOptions = new LinearLayout(this);
        llAncSubOptions.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(10);
        card.addView(llAncSubOptions, subLp);

        LinearLayout strengthRow = new LinearLayout(this);
        strengthRow.setOrientation(LinearLayout.HORIZONTAL);
        llAncSubOptions.addView(strengthRow);

        btnAncWeak = createSmallSubBtn("Zayıf (1)");
        btnAncMod = createSmallSubBtn("Orta (2)");
        btnAncStrong = createSmallSubBtn("Güçlü (3)");
        btnAncAdaptive = createSmallSubBtn("Adaptif AI");

        btnAncWeak.setOnClickListener(v -> engine.setAncDetails(
                SoundcoreProtocol.NC_MODE_MANUAL, SoundcoreProtocol.ANC_STRENGTH_WEAK,
                engine.getState().windNoiseSuppression, "ANC Seviye 1 (Zayıf)"));
        btnAncMod.setOnClickListener(v -> engine.setAncDetails(
                SoundcoreProtocol.NC_MODE_MANUAL, SoundcoreProtocol.ANC_STRENGTH_MODERATE,
                engine.getState().windNoiseSuppression, "ANC Seviye 2 (Orta)"));
        btnAncStrong.setOnClickListener(v -> engine.setAncDetails(
                SoundcoreProtocol.NC_MODE_MANUAL, SoundcoreProtocol.ANC_STRENGTH_STRONG,
                engine.getState().windNoiseSuppression, "ANC Seviye 3 (Güçlü)"));
        btnAncAdaptive.setOnClickListener(v -> engine.setAncDetails(
                SoundcoreProtocol.NC_MODE_ADAPTIVE, engine.getState().manualAncStrength,
                engine.getState().windNoiseSuppression, "Adaptif ANC Modu"));

        strengthRow.addView(btnAncWeak, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        strengthRow.addView(new View(this), new LinearLayout.LayoutParams(dp(6), 1));
        strengthRow.addView(btnAncMod, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        strengthRow.addView(new View(this), new LinearLayout.LayoutParams(dp(6), 1));
        strengthRow.addView(btnAncStrong, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        strengthRow.addView(new View(this), new LinearLayout.LayoutParams(dp(6), 1));
        strengthRow.addView(btnAncAdaptive, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnWindNoise = createSmallSubBtn("💨 Rüzgar Gürültüsü Engelleme: KAPALI");
        LinearLayout.LayoutParams windLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        windLp.topMargin = dp(8);
        btnWindNoise.setOnClickListener(v -> {
            boolean next = !engine.getState().windNoiseSuppression;
            engine.setAncDetails(engine.getState().ncMode, engine.getState().manualAncStrength,
                    next, "Rüzgar Engelleme " + (next ? "AÇIK" : "KAPALI"));
        });
        llAncSubOptions.addView(btnWindNoise, windLp);

        // Transparency Sub-options (Fully Transparent / Vocal Mode)
        llTransSubOptions = new LinearLayout(this);
        llTransSubOptions.setOrientation(LinearLayout.HORIZONTAL);
        llTransSubOptions.setVisibility(View.GONE);
        card.addView(llTransSubOptions, subLp);

        btnTransFull = createSmallSubBtn("🌐 Tam Şeffaf Mod");
        btnTransVocal = createSmallSubBtn("🗣️ Vokal (Net İnsan Sesi)");
        btnTransFull.setOnClickListener(v -> engine.setTransparencyDetails(
                SoundcoreProtocol.TRANS_FULLY_TRANSPARENT, "Tam Şeffaf Mod"));
        btnTransVocal.setOnClickListener(v -> engine.setTransparencyDetails(
                SoundcoreProtocol.TRANS_VOCAL_MODE, "Vokal Şeffaf Mod"));

        llTransSubOptions.addView(btnTransFull, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        llTransSubOptions.addView(new View(this), new LinearLayout.LayoutParams(dp(8), 1));
        llTransSubOptions.addView(btnTransVocal, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return card;
    }

    private View buildAutoEqCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundedBox("#11172A", "#00E5FF", 18, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top);

        LinearLayout txtCol = new LinearLayout(this);
        txtCol.setOrientation(LinearLayout.VERTICAL);
        top.addView(txtCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("1A • Uygulama & Şarkı Duyarlı Otomatik EQ");
        tvTitle.setTextColor(Color.parseColor("#00E5FF"));
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        txtCol.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("YouTube'da Net Konuşma EQ, YT Music/Spotify'da BassUp Müzik EQ");
        tvSub.setTextColor(Color.parseColor("#94A3B8"));
        tvSub.setTextSize(11);
        txtCol.addView(tvSub);

        swAutoEq = new Switch(this);
        swAutoEq.setChecked(true);
        swAutoEq.setOnCheckedChangeListener((b, checked) -> {
            if (!updatingUiProgrammatically) {
                engine.setAutoEqByAppEnabled(checked);
            }
        });
        top.addView(swAutoEq);

        tvNowPlayingInfo = new TextView(this);
        tvNowPlayingInfo.setPadding(dp(12), dp(10), dp(12), dp(10));
        tvNowPlayingInfo.setBackground(roundedBox("#182238", "#263554", 12, 1));
        tvNowPlayingInfo.setTextColor(Color.WHITE);
        tvNowPlayingInfo.setTextSize(12);
        LinearLayout.LayoutParams npLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        npLp.topMargin = dp(10);
        card.addView(tvNowPlayingInfo, npLp);

        tvAutoEqRuleStatus = new TextView(this);
        tvAutoEqRuleStatus.setTextColor(Color.parseColor("#38BDF8"));
        tvAutoEqRuleStatus.setTextSize(11);
        LinearLayout.LayoutParams rsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rsLp.topMargin = dp(6);
        card.addView(tvAutoEqRuleStatus, rsLp);

        // Quick test buttons for YouTube vs YT Music
        LinearLayout simRow = new LinearLayout(this);
        simRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srLp.topMargin = dp(8);
        card.addView(simRow, srLp);

        TextView btnSimYt = createSmallSubBtn("▶️ Test: YouTube (Konuşma EQ)");
        styleSelectableButton(btnSimYt, false, "#00E5FF");
        btnSimYt.setOnClickListener(v -> engine.simulateMediaAppSwitch(
                "com.google.android.youtube",
                "How the US Turned Iran Into a Dictatorship",
                "Real Time History"
        ));
        TextView btnSimYtm = createSmallSubBtn("🎵 Test: YT Music (BassUp EQ)");
        styleSelectableButton(btnSimYtm, false, "#00E5FF");
        btnSimYtm.setOnClickListener(v -> engine.simulateMediaAppSwitch(
                "com.google.android.apps.youtube.music",
                "Şar",
                "Koma Azad"
        ));

        simRow.addView(btnSimYt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        simRow.addView(new View(this), new LinearLayout.LayoutParams(dp(8), 1));
        simRow.addView(btnSimYtm, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return card;
    }

    private View buildSmartActivityCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundedBox("#11172A", "#A855F7", 18, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top);

        LinearLayout txtCol = new LinearLayout(this);
        txtCol.setOrientation(LinearLayout.VERTICAL);
        top.addView(txtCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("1B • Harekete ve Hıza Göre Otonom ANC");
        tvTitle.setTextColor(Color.parseColor("#C084FC"));
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        txtCol.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("Sabitken Max ANC • Koşarken Şeffaf Mod + Sport EQ • Araçta Rüzgar ANC");
        tvSub.setTextColor(Color.parseColor("#94A3B8"));
        tvSub.setTextSize(11);
        txtCol.addView(tvSub);

        swSmartActivity = new Switch(this);
        swSmartActivity.setChecked(true);
        swSmartActivity.setOnCheckedChangeListener((b, checked) -> {
            if (!updatingUiProgrammatically) {
                engine.setSmartActivityAncEnabled(checked);
            }
        });
        top.addView(swSmartActivity);

        tvActivityTelemetry = new TextView(this);
        tvActivityTelemetry.setPadding(dp(12), dp(8), dp(12), dp(8));
        tvActivityTelemetry.setBackground(roundedBox("#182238", "#263554", 10, 1));
        tvActivityTelemetry.setTextColor(Color.parseColor("#E2E8F0"));
        tvActivityTelemetry.setTextSize(11);
        LinearLayout.LayoutParams atLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        atLp.topMargin = dp(10);
        card.addView(tvActivityTelemetry, atLp);

        LinearLayout actBtnRow = new LinearLayout(this);
        actBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams abLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        abLp.topMargin = dp(8);
        card.addView(actBtnRow, abLp);

        btnActStationary = createSmallSubBtn("🪑 Sabit\n(Max ANC)");
        btnActWalking = createSmallSubBtn("🏃 Yürüyüş/Koşu\n(Şeffaf+Sport)");
        btnActTransit = createSmallSubBtn("🚆 Araç/Hızlı\n(Rüzgar ANC)");

        btnActStationary.setOnClickListener(v -> engine.simulateActivityState(SmartAutomationEngine.ACTIVITY_STATIONARY));
        btnActWalking.setOnClickListener(v -> engine.simulateActivityState(SmartAutomationEngine.ACTIVITY_WALKING_RUNNING));
        btnActTransit.setOnClickListener(v -> engine.simulateActivityState(SmartAutomationEngine.ACTIVITY_TRANSIT_FAST));

        actBtnRow.addView(btnActStationary, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actBtnRow.addView(new View(this), new LinearLayout.LayoutParams(dp(6), 1));
        actBtnRow.addView(btnActWalking, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actBtnRow.addView(new View(this), new LinearLayout.LayoutParams(dp(6), 1));
        actBtnRow.addView(btnActTransit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return card;
    }

    private View buildSpeakToChatCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundedBox("#11172A", "#10B981", 18, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top);

        LinearLayout txtCol = new LinearLayout(this);
        txtCol.setOrientation(LinearLayout.VERTICAL);
        top.addView(txtCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("1C • Speak-to-Chat (Konuşunca Şeffaf Mod)");
        tvTitle.setTextColor(Color.parseColor("#34D399"));
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        txtCol.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("Konuştuğunuzda sesi %20'ye kısıp Vokal Şeffaf Moda geçer, 5 sn sonra ANC'ye döner");
        tvSub.setTextColor(Color.parseColor("#94A3B8"));
        tvSub.setTextSize(11);
        txtCol.addView(tvSub);

        swSpeakToChat = new Switch(this);
        swSpeakToChat.setChecked(false);
        swSpeakToChat.setOnCheckedChangeListener((b, checked) -> {
            if (!updatingUiProgrammatically) {
                engine.setSpeakToChatEnabled(checked);
            }
        });
        top.addView(swSpeakToChat);

        tvSpeakStatus = new TextView(this);
        tvSpeakStatus.setText("Mikrofon Dinleme: Kapalı (Aktif etmek için anahtarı açın)");
        tvSpeakStatus.setTextColor(Color.parseColor("#E2E8F0"));
        tvSpeakStatus.setTextSize(12);
        LinearLayout.LayoutParams ssLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ssLp.topMargin = dp(8);
        card.addView(tvSpeakStatus, ssLp);

        pbMicLevel = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pbMicLevel.setMax(100);
        pbMicLevel.setProgress(0);
        pbMicLevel.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
        LinearLayout.LayoutParams pmLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        pmLp.topMargin = dp(6);
        card.addView(pbMicLevel, pmLp);

        LinearLayout threshRow = new LinearLayout(this);
        threshRow.setOrientation(LinearLayout.HORIZONTAL);
        threshRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trLp.topMargin = dp(8);
        card.addView(threshRow, trLp);

        tvThresholdVal = new TextView(this);
        tvThresholdVal.setText("Hassasiyet Eşiği: 58 dB");
        tvThresholdVal.setTextColor(Color.parseColor("#94A3B8"));
        tvThresholdVal.setTextSize(11);
        threshRow.addView(tvThresholdVal);

        SeekBar sbThresh = new SeekBar(this);
        sbThresh.setMax(65);
        sbThresh.setProgress(58 - 25);
        sbThresh.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    engine.setSpeakThresholdLevel(25 + progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        threshRow.addView(sbThresh, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return card;
    }

    private View buildEqualizerStudioCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.setBackground(roundedBox("#11172A", "#1E293B", 20, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        card.setLayoutParams(lp);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(topRow);

        TextView title = new TextView(this);
        title.setText("🎛️ 8-BANT DONANIM EKOLAYZIR & BASSUP");
        title.setTextColor(Color.WHITE);
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        topRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvBassLbl = new TextView(this);
        tvBassLbl.setText("🔥 BassUp ");
        tvBassLbl.setTextColor(Color.parseColor("#F59E0B"));
        tvBassLbl.setTextSize(12);
        tvBassLbl.setTypeface(Typeface.DEFAULT_BOLD);
        topRow.addView(tvBassLbl);

        swBassUp = new Switch(this);
        swBassUp.setChecked(true);
        swBassUp.setOnCheckedChangeListener((b, checked) -> {
            if (!updatingUiProgrammatically) {
                engine.setBassUpEnabled(checked);
            }
        });
        topRow.addView(swBassUp);

        // Horizontal scroll of EQ Presets
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams hsvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hsvLp.topMargin = dp(10);
        hsvLp.bottomMargin = dp(8);
        card.addView(hsv, hsvLp);

        LinearLayout presetRow = new LinearLayout(this);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(presetRow);

        for (int i = 0; i < SmartAutomationEngine.EQ_PRESET_NAMES.length; i++) {
            final int presetIdx = i;
            TextView chip = createSmallSubBtn(SmartAutomationEngine.EQ_PRESET_NAMES[i]);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cLp.rightMargin = dp(8);
            chip.setOnClickListener(v -> engine.applyEqPreset(presetIdx, engine.isBassUpEnabled(), "EQ Profili Seçildi"));
            presetButtons[i] = chip;
            presetRow.addView(chip, cLp);
        }

        // 8 Band Sliders
        for (int b = 0; b < 8; b++) {
            final int bandIdx = b;
            LinearLayout bandRow = new LinearLayout(this);
            bandRow.setOrientation(LinearLayout.HORIZONTAL);
            bandRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams brLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            brLp.topMargin = dp(4);
            card.addView(bandRow, brLp);

            TextView tvFreq = new TextView(this);
            tvFreq.setText(BAND_LABELS[b]);
            tvFreq.setTextColor(Color.parseColor("#94A3B8"));
            tvFreq.setTextSize(11);
            bandRow.addView(tvFreq, new LinearLayout.LayoutParams(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT));

            SeekBar sb = new SeekBar(this);
            sb.setMax(240); // -120 to +120 tenths dB
            sb.setProgress(120);
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int tenthsDb = progress - 120;
                    eqValueLabels[bandIdx].setText(String.format(Locale.US, "%+.1f dB", tenthsDb / 10.0f));
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (!updatingUiProgrammatically) {
                        int tenthsDb = seekBar.getProgress() - 120;
                        engine.setCustomEqBand(bandIdx, tenthsDb);
                    }
                }
            });
            eqSeekBars[b] = sb;
            bandRow.addView(sb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvVal = new TextView(this);
            tvVal.setText("+0.0 dB");
            tvVal.setTextColor(Color.parseColor("#00E5FF"));
            tvVal.setTextSize(11);
            tvVal.setGravity(Gravity.END);
            eqValueLabels[b] = tvVal;
            bandRow.addView(tvVal, new LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        return card;
    }

    private View buildTelemetryConsoleCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        card.setBackground(roundedBox("#0B0F1A", "#1E293B", 16, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        card.setLayoutParams(lp);

        TextView title = new TextView(this);
        title.setText("📡 CANLI BLUETOOTH RFCOMM & IOT TELEMETRİ KONSOLU");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(12);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        tvTxRxHex = new TextView(this);
        tvTxRxHex.setTypeface(Typeface.MONOSPACE);
        tvTxRxHex.setTextSize(10);
        tvTxRxHex.setTextColor(Color.parseColor("#A7F3D0"));
        tvTxRxHex.setPadding(0, dp(6), 0, dp(6));
        card.addView(tvTxRxHex);

        tvTelemetryLogs = new TextView(this);
        tvTelemetryLogs.setTypeface(Typeface.MONOSPACE);
        tvTelemetryLogs.setTextSize(10);
        tvTelemetryLogs.setTextColor(Color.parseColor("#94A3B8"));
        card.addView(tvTelemetryLogs);

        return card;
    }

    private TextView createPillButton(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(12);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(dp(8), dp(12), dp(8), dp(12));
        return tv;
    }

    private TextView createSmallSubBtn(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(11);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(dp(8), dp(8), dp(8), dp(8));
        return tv;
    }

    private void styleSelectableButton(TextView btn, boolean active, String activeColorHex) {
        if (active) {
            btn.setBackground(roundedBox(activeColorHex, "#FFFFFF", 12, 1));
            btn.setTextColor(Color.parseColor("#080B14"));
        } else {
            btn.setBackground(roundedBox("#182238", "#263554", 12, 1));
            btn.setTextColor(Color.parseColor("#CBD5E1"));
        }
    }

    private boolean isNotificationListenerEnabled() {
        try {
            String flat = android.provider.Settings.Secure.getString(
                    getContentResolver(), "enabled_notification_listeners");
            return flat != null && flat.contains(getPackageName());
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void onStateUpdated() {
        if (engine == null) return;
        updatingUiProgrammatically = true;
        try {
            if (btnGrantNotifAccess != null) {
                btnGrantNotifAccess.setVisibility(isNotificationListenerEnabled() ? View.GONE : View.VISIBLE);
            }
            SoundcoreProtocol.DeviceState st = engine.getState();
            tvConnStatus.setText(engine.getConnectionStatusText());
            tvConnStatus.setTextColor(st.connected ? Color.parseColor("#34D399") : Color.parseColor("#FBBF24"));

            if (tvModelName != null && st.deviceName != null) {
                tvModelName.setText(st.deviceName);
            }
            tvDeviceMeta.setText(String.format(Locale.US,
                    "Model: %s • MAC: %s\nFW: v%s (L) / v%s (R) • SN: %s",
                    st.modelCode, st.deviceMac, st.firmwareLeft, st.firmwareRight, st.serialNumber));

            int leftPct = Math.max(10, st.leftBattery * 20);
            int rightPct = Math.max(10, st.rightBattery * 20);
            int casePct = Math.max(10, st.caseBattery * 20);

            tvLeftBatt.setText("%" + leftPct + (st.leftCharging ? " ⚡" : ""));
            pbLeftBatt.setProgress(leftPct);
            tvRightBatt.setText("%" + rightPct + (st.rightCharging ? " ⚡" : ""));
            pbRightBatt.setProgress(rightPct);
            tvCaseBatt.setText("%" + casePct);
            pbCaseBatt.setProgress(casePct);

            // Sound Modes
            styleSelectableButton(btnModeAnc, st.ambientMode == SoundcoreProtocol.AMBIENT_ANC, "#00E5FF");
            styleSelectableButton(btnModeTrans, st.ambientMode == SoundcoreProtocol.AMBIENT_TRANSPARENCY, "#34D399");
            styleSelectableButton(btnModeNormal, st.ambientMode == SoundcoreProtocol.AMBIENT_NORMAL, "#A855F7");

            llAncSubOptions.setVisibility(st.ambientMode == SoundcoreProtocol.AMBIENT_ANC ? View.VISIBLE : View.GONE);
            llTransSubOptions.setVisibility(st.ambientMode == SoundcoreProtocol.AMBIENT_TRANSPARENCY ? View.VISIBLE : View.GONE);

            boolean isManual = (st.ncMode == SoundcoreProtocol.NC_MODE_MANUAL);
            styleSelectableButton(btnAncWeak, isManual && st.manualAncStrength == SoundcoreProtocol.ANC_STRENGTH_WEAK, "#00E5FF");
            styleSelectableButton(btnAncMod, isManual && st.manualAncStrength == SoundcoreProtocol.ANC_STRENGTH_MODERATE, "#00E5FF");
            styleSelectableButton(btnAncStrong, isManual && st.manualAncStrength == SoundcoreProtocol.ANC_STRENGTH_STRONG, "#00E5FF");
            styleSelectableButton(btnAncAdaptive, !isManual, "#00E5FF");

            btnWindNoise.setText("💨 Rüzgar Gürültüsü Engelleme: " + (st.windNoiseSuppression ? "AÇIK" : "KAPALI"));
            styleSelectableButton(btnWindNoise, st.windNoiseSuppression, "#38BDF8");

            styleSelectableButton(btnTransFull, st.transparencyMode == SoundcoreProtocol.TRANS_FULLY_TRANSPARENT, "#34D399");
            styleSelectableButton(btnTransVocal, st.transparencyMode == SoundcoreProtocol.TRANS_VOCAL_MODE, "#34D399");

            // #1A Auto-EQ
            swAutoEq.setChecked(engine.isAutoEqByAppEnabled());
            String pkg = engine.getCurrentMediaPackage();
            String friendlyApp = pkg.contains("youtube.music") ? "YouTube Music"
                    : pkg.contains("youtube") ? "YouTube"
                    : pkg.contains("spotify") ? "Spotify" : pkg;
            tvNowPlayingInfo.setText("🎶 Algılanan Medya: " + friendlyApp + "\nParça: "
                    + engine.getCurrentMediaTitle() + " — " + engine.getCurrentMediaArtist());
            tvAutoEqRuleStatus.setText("⚡ Aktif Otonom EQ Profili: "
                    + SmartAutomationEngine.EQ_PRESET_NAMES[engine.getCurrentPresetIndex()]
                    + (engine.isBassUpEnabled() ? " + 🔥 BassUp Açık" : ""));

            // #1B Smart Activity
            swSmartActivity.setChecked(engine.isSmartActivityAncEnabled());
            int actState = engine.getCurrentActivityState();
            String actName = (actState == SmartAutomationEngine.ACTIVITY_STATIONARY) ? "🪑 Sabit / Masa Başı (Max ANC)"
                    : (actState == SmartAutomationEngine.ACTIVITY_WALKING_RUNNING) ? "🏃 Yürüyüş / Koşu (Şeffaf + Sport EQ)"
                    : "🚆 Araç / Hızlı Hareket (ANC + Rüzgar)";
            tvActivityTelemetry.setText(String.format(Locale.US,
                    "Sensör İvme (RMS): %.2f m/s² • GPS Hız: %.1f km/s\nAktif Durum: %s",
                    engine.getCurrentMovementRms(), engine.getCurrentSpeedKmh(), actName));
            styleSelectableButton(btnActStationary, actState == SmartAutomationEngine.ACTIVITY_STATIONARY, "#C084FC");
            styleSelectableButton(btnActWalking, actState == SmartAutomationEngine.ACTIVITY_WALKING_RUNNING, "#C084FC");
            styleSelectableButton(btnActTransit, actState == SmartAutomationEngine.ACTIVITY_TRANSIT_FAST, "#C084FC");

            // #1C Speak-to-Chat
            swSpeakToChat.setChecked(engine.isSpeakToChatEnabled());
            pbMicLevel.setProgress(engine.getCurrentMicDbLevel());
            tvThresholdVal.setText("Hassasiyet Eşiği: " + engine.getSpeakThresholdLevel() + " dB");
            if (engine.isSpeakTriggeredActive()) {
                tvSpeakStatus.setText("🗣️ KONUŞMA ALGILANDI! Vokal Şeffaf Mod Aktif (Ses %20) • ANC'ye dönüş: "
                        + engine.getSpeakCountdownSec() + " sn");
                tvSpeakStatus.setTextColor(Color.parseColor("#34D399"));
            } else if (engine.isSpeakToChatEnabled()) {
                tvSpeakStatus.setText("🎙️ Mikrofon Dinleniyor (Anlık Ses: " + engine.getCurrentMicDbLevel() + " dB)");
                tvSpeakStatus.setTextColor(Color.parseColor("#E2E8F0"));
            } else {
                tvSpeakStatus.setText("Mikrofon Dinleme: Kapalı");
                tvSpeakStatus.setTextColor(Color.parseColor("#94A3B8"));
            }

            // Equalizer & BassUp
            swBassUp.setChecked(engine.isBassUpEnabled());
            for (int i = 0; i < presetButtons.length; i++) {
                if (presetButtons[i] != null) {
                    styleSelectableButton(presetButtons[i], i == engine.getCurrentPresetIndex(), "#00E5FF");
                }
            }
            for (int b = 0; b < 8; b++) {
                int tenthsDb = st.eqBandsTenthsDb[b];
                eqSeekBars[b].setProgress(tenthsDb + 120);
                eqValueLabels[b].setText(String.format(Locale.US, "%+.1f dB", tenthsDb / 10.0f));
            }

            // Telemetry Console
            tvTxRxHex.setText("TX (Giden) : " + engine.getLastTxHex() + "\nRX (Gelen) : " + engine.getLastRxHex());
            List<String> logs = engine.getTelemetryLogs();
            StringBuilder sb = new StringBuilder();
            int count = Math.min(10, logs.size());
            for (int i = 0; i < count; i++) {
                sb.append(logs.get(i)).append("\n");
            }
            tvTelemetryLogs.setText(sb.toString().trim());
        } finally {
            updatingUiProgrammatically = false;
        }
    }
}
