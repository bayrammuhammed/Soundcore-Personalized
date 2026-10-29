package com.bayram.soundcorex20;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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

    // Header & Language UI
    private TextView tvAppTitle;
    private TextView tvConnStatus;
    private TextView btnReconnect;
    private TextView btnHeaderPower;
    private TextView btnGrantNotifAccess;
    private final TextView[] langButtons = new TextView[I18n.SUPPORTED_CODES.length];

    // Master Power & Auto-Connect Card
    private LinearLayout cardPowerControl;
    private TextView tvMasterPowerLbl;
    private Switch swMasterPower;
    private TextView tvAutoBtLbl;
    private Switch swAutoBt;
    private TextView btnPowerStopOrResume;

    // Device Hero Card
    private TextView tvModelName;
    private TextView tvDeviceMeta;
    private TextView tvLeftBattLbl, tvRightBattLbl, tvCaseBattLbl;
    private TextView tvLeftBatt, tvRightBatt, tvCaseBatt;
    private ProgressBar pbLeftBatt, pbRightBatt, pbCaseBatt;

    // Sound Mode Controls
    private TextView tvAncSecTitle;
    private TextView btnModeAnc, btnModeTrans, btnModeNormal;
    private LinearLayout llAncSubOptions, llTransSubOptions;
    private TextView btnAncWeak, btnAncMod, btnAncStrong, btnAncAdaptive, btnWindNoise;
    private TextView btnTransFull, btnTransVocal;

    // Autonomous Section
    private TextView tvAutoHeader;
    private TextView tv1aTitle, tv1aSub, tvNowPlayingInfo, tvAutoEqRuleStatus, btnSimYt, btnSimYtm;
    private Switch swAutoEq;

    private TextView tv1bTitle, tv1bSub, tvActivityTelemetry, btnActStationary, btnActWalking, btnActTransit;
    private Switch swSmartActivity;

    private TextView tv1cTitle, tv1cSub, tvSpeakStatus, tvThresholdVal;
    private Switch swSpeakToChat;
    private ProgressBar pbMicLevel;

    private TextView tv1dTitle, tv1dSub, tvWhisperStatus, tvWhisperGainVal, btnWhisperTest;
    private Switch swWhisperBoost;
    private ProgressBar pbWhisperOut;

    // Equalizer Studio
    private TextView tvEqTitle, tvBassLbl;
    private Switch swBassUp;
    private final TextView[] presetButtons = new TextView[6];
    private final SeekBar[] eqSeekBars = new SeekBar[8];
    private final TextView[] eqValueLabels = new TextView[8];
    private boolean updatingUiProgrammatically = false;

    // Telemetry Console
    private TextView tvTelemetryTitle, tvTxRxHex, tvTelemetryLogs;

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
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
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
        header.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        tvAppTitle = new TextView(this);
        tvAppTitle.setText(I18n.t("app_title"));
        tvAppTitle.setTextColor(Color.parseColor("#00E5FF"));
        tvAppTitle.setTextSize(18);
        tvAppTitle.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(tvAppTitle);

        tvConnStatus = new TextView(this);
        tvConnStatus.setTextColor(Color.parseColor("#94A3B8"));
        tvConnStatus.setTextSize(11);
        titleCol.addView(tvConnStatus);

        btnReconnect = new TextView(this);
        btnReconnect.setTextColor(Color.parseColor("#00E5FF"));
        btnReconnect.setTextSize(11);
        btnReconnect.setTypeface(Typeface.DEFAULT_BOLD);
        btnReconnect.setPadding(dp(10), dp(8), dp(10), dp(8));
        btnReconnect.setBackground(roundedBox("#112233", "#00E5FF", 12, 1));
        btnReconnect.setOnClickListener(v -> {
            if (!engine.isMasterAppEnabled()) {
                engine.setMasterAppEnabled(true);
            } else {
                engine.connectToEarbuds();
            }
        });
        header.addView(btnReconnect);

        btnHeaderPower = new TextView(this);
        btnHeaderPower.setText("⏻");
        btnHeaderPower.setTextColor(Color.parseColor("#FFFFFF"));
        btnHeaderPower.setTextSize(12);
        btnHeaderPower.setTypeface(Typeface.DEFAULT_BOLD);
        btnHeaderPower.setPadding(dp(10), dp(8), dp(10), dp(8));
        btnHeaderPower.setBackground(roundedBox("#7F1D1D", "#F87171", 12, 1));
        LinearLayout.LayoutParams hpLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hpLp.leftMargin = dp(6);
        btnHeaderPower.setOnClickListener(v -> engine.setMasterAppEnabled(!engine.isMasterAppEnabled()));
        header.addView(btnHeaderPower, hpLp);

        // 1.5 MULTI-LANGUAGE BAR (Auto / TR / EN / DE / ES / FR / RU / AR)
        HorizontalScrollView langScroll = new HorizontalScrollView(this);
        langScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams lsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lsLp.topMargin = dp(10);
        root.addView(langScroll, lsLp);

        LinearLayout langRow = new LinearLayout(this);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        langScroll.addView(langRow);

        for (int i = 0; i < I18n.SUPPORTED_CODES.length; i++) {
            final String code = I18n.SUPPORTED_CODES[i];
            TextView pill = createSmallSubBtn(I18n.SUPPORTED_LABELS[i]);
            pill.setPadding(dp(10), dp(5), dp(10), dp(5));
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pLp.rightMargin = dp(6);
            pill.setOnClickListener(v -> {
                I18n.setLanguageOverride(code);
                onStateUpdated();
            });
            langButtons[i] = pill;
            langRow.addView(pill, pLp);
        }

        // 1.8 MASTER POWER & AUTO-CONNECT CONTROL CARD
        root.addView(buildPowerControlCard());

        // Self-service Notification Listener permission banner
        btnGrantNotifAccess = new TextView(this);
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
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            } catch (Throwable ignored) {}
        });
        root.addView(btnGrantNotifAccess, gnLp);

        // 2. HERO DEVICE CARD
        root.addView(buildHeroDeviceCard());

        // 3. SOUND MODES CARD
        root.addView(buildSoundModesCard());

        // 4. AUTONOMOUS SECTION HEADER
        tvAutoHeader = new TextView(this);
        tvAutoHeader.setTextColor(Color.parseColor("#E2E8F0"));
        tvAutoHeader.setTextSize(13);
        tvAutoHeader.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams secLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        secLp.topMargin = dp(18);
        secLp.bottomMargin = dp(8);
        root.addView(tvAutoHeader, secLp);

        root.addView(buildAutoEqCard());
        root.addView(buildSmartActivityCard());
        root.addView(buildSpeakToChatCard());
        root.addView(buildWhisperCallBoostCard());

        // 5. EQUALIZER STUDIO
        root.addView(buildEqualizerStudioCard());

        // 6. TELEMETRY CONSOLE
        root.addView(buildTelemetryConsoleCard());

        return scroll;
    }

    private View buildPowerControlCard() {
        cardPowerControl = new LinearLayout(this);
        cardPowerControl.setOrientation(LinearLayout.VERTICAL);
        cardPowerControl.setPadding(dp(14), dp(12), dp(14), dp(12));
        cardPowerControl.setBackground(roundedBox("#11172A", "#1E293B", 16, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        cardPowerControl.setLayoutParams(lp);

        // Row 1: Master App Power Switch (ON/OFF)
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        cardPowerControl.addView(row1);

        tvMasterPowerLbl = new TextView(this);
        tvMasterPowerLbl.setTextColor(Color.parseColor("#34D399"));
        tvMasterPowerLbl.setTextSize(12);
        tvMasterPowerLbl.setTypeface(Typeface.DEFAULT_BOLD);
        row1.addView(tvMasterPowerLbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        swMasterPower = new Switch(this);
        swMasterPower.setChecked(engine.isMasterAppEnabled());
        swMasterPower.setOnCheckedChangeListener((btn, checked) -> {
            if (!updatingUiProgrammatically) {
                engine.setMasterAppEnabled(checked);
            }
        });
        row1.addView(swMasterPower);

        // Row 2: Auto-Connect on Bluetooth Switch
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams r2Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        r2Lp.topMargin = dp(6);
        cardPowerControl.addView(row2, r2Lp);

        tvAutoBtLbl = new TextView(this);
        tvAutoBtLbl.setTextColor(Color.parseColor("#94A3B8"));
        tvAutoBtLbl.setTextSize(11);
        row2.addView(tvAutoBtLbl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        swAutoBt = new Switch(this);
        swAutoBt.setChecked(engine.isAutoConnectOnBt());
        swAutoBt.setOnCheckedChangeListener((btn, checked) -> {
            if (!updatingUiProgrammatically) {
                engine.setAutoConnectOnBt(checked);
            }
        });
        row2.addView(swAutoBt);

        // Row 3: Quick "Disconnect & Exit App" or "Re-enable" button
        btnPowerStopOrResume = createSmallSubBtn("");
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.topMargin = dp(8);
        btnPowerStopOrResume.setOnClickListener(v -> {
            if (engine.isMasterAppEnabled()) {
                engine.setMasterAppEnabled(false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAndRemoveTask();
                } else {
                    finish();
                }
            } else {
                engine.setMasterAppEnabled(true);
            }
        });
        cardPowerControl.addView(btnPowerStopOrResume, btnLp);

        return cardPowerControl;
    }

    private View buildHeroDeviceCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(roundedBox("#11172A", "#1E293B", 20, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        card.setLayoutParams(lp);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(topRow);

        ImageView imgLeft = new ImageView(this);
        imgLeft.setImageResource(R.drawable.a3968_device_line_l);
        imgLeft.setScaleType(ImageView.ScaleType.FIT_CENTER);
        topRow.addView(imgLeft, new LinearLayout.LayoutParams(dp(56), dp(56)));

        ImageView imgRight = new ImageView(this);
        imgRight.setImageResource(R.drawable.a3968_device_line_r);
        imgRight.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(dp(56), dp(56));
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
        tvDeviceMeta.setTextColor(Color.parseColor("#94A3B8"));
        tvDeviceMeta.setTextSize(11);
        metaCol.addView(tvDeviceMeta);

        LinearLayout battRow = new LinearLayout(this);
        battRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bRowLp.topMargin = dp(14);
        card.addView(battRow, bRowLp);

        tvLeftBattLbl = new TextView(this);
        tvLeftBatt = new TextView(this);
        pbLeftBatt = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        battRow.addView(buildBatteryBox(tvLeftBattLbl, tvLeftBatt, pbLeftBatt, "#00E5FF"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        battRow.addView(new View(this), new LinearLayout.LayoutParams(dp(8), 1));

        tvRightBattLbl = new TextView(this);
        tvRightBatt = new TextView(this);
        pbRightBatt = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        battRow.addView(buildBatteryBox(tvRightBattLbl, tvRightBatt, pbRightBatt, "#00E5FF"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        battRow.addView(new View(this), new LinearLayout.LayoutParams(dp(8), 1));

        tvCaseBattLbl = new TextView(this);
        tvCaseBatt = new TextView(this);
        pbCaseBatt = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        battRow.addView(buildBatteryBox(tvCaseBattLbl, tvCaseBatt, pbCaseBatt, "#A855F7"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return card;
    }

    private LinearLayout buildBatteryBox(TextView tvLbl, TextView tvVal, ProgressBar pb, String accentHex) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(8), dp(10), dp(8));
        box.setBackground(roundedBox("#182238", "#263554", 12, 1));

        tvLbl.setTextColor(Color.parseColor("#94A3B8"));
        tvLbl.setTextSize(9);
        tvLbl.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(tvLbl);

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

        tvAncSecTitle = new TextView(this);
        tvAncSecTitle.setTextColor(Color.WHITE);
        tvAncSecTitle.setTextSize(12);
        tvAncSecTitle.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(tvAncSecTitle);

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams mrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mrLp.topMargin = dp(10);
        card.addView(modeRow, mrLp);

        btnModeAnc = createPillButton("");
        btnModeTrans = createPillButton("");
        btnModeNormal = createPillButton("");

        btnModeAnc.setOnClickListener(v -> engine.setAmbientMode(SoundcoreProtocol.AMBIENT_ANC, "ANC"));
        btnModeTrans.setOnClickListener(v -> engine.setAmbientMode(SoundcoreProtocol.AMBIENT_TRANSPARENCY, "Transparency"));
        btnModeNormal.setOnClickListener(v -> engine.setAmbientMode(SoundcoreProtocol.AMBIENT_NORMAL, "Normal"));

        modeRow.addView(btnModeAnc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        modeRow.addView(new View(this), new LinearLayout.LayoutParams(dp(8), 1));
        modeRow.addView(btnModeTrans, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        modeRow.addView(new View(this), new LinearLayout.LayoutParams(dp(8), 1));
        modeRow.addView(btnModeNormal, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        llAncSubOptions = new LinearLayout(this);
        llAncSubOptions.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(10);
        card.addView(llAncSubOptions, subLp);

        LinearLayout strengthRow = new LinearLayout(this);
        strengthRow.setOrientation(LinearLayout.HORIZONTAL);
        llAncSubOptions.addView(strengthRow);

        btnAncWeak = createSmallSubBtn("");
        btnAncMod = createSmallSubBtn("");
        btnAncStrong = createSmallSubBtn("");
        btnAncAdaptive = createSmallSubBtn("");

        btnAncWeak.setOnClickListener(v -> engine.setAncDetails(
                SoundcoreProtocol.NC_MODE_MANUAL, SoundcoreProtocol.ANC_STRENGTH_WEAK,
                engine.getState().windNoiseSuppression, "ANC Level 1"));
        btnAncMod.setOnClickListener(v -> engine.setAncDetails(
                SoundcoreProtocol.NC_MODE_MANUAL, SoundcoreProtocol.ANC_STRENGTH_MODERATE,
                engine.getState().windNoiseSuppression, "ANC Level 2"));
        btnAncStrong.setOnClickListener(v -> engine.setAncDetails(
                SoundcoreProtocol.NC_MODE_MANUAL, SoundcoreProtocol.ANC_STRENGTH_STRONG,
                engine.getState().windNoiseSuppression, "ANC Level 3"));
        btnAncAdaptive.setOnClickListener(v -> engine.setAncDetails(
                SoundcoreProtocol.NC_MODE_ADAPTIVE, engine.getState().manualAncStrength,
                engine.getState().windNoiseSuppression, "Adaptive ANC"));

        strengthRow.addView(btnAncWeak, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        strengthRow.addView(new View(this), new LinearLayout.LayoutParams(dp(6), 1));
        strengthRow.addView(btnAncMod, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        strengthRow.addView(new View(this), new LinearLayout.LayoutParams(dp(6), 1));
        strengthRow.addView(btnAncStrong, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        strengthRow.addView(new View(this), new LinearLayout.LayoutParams(dp(6), 1));
        strengthRow.addView(btnAncAdaptive, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnWindNoise = createSmallSubBtn("");
        LinearLayout.LayoutParams windLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        windLp.topMargin = dp(8);
        btnWindNoise.setOnClickListener(v -> {
            boolean next = !engine.getState().windNoiseSuppression;
            engine.setAncDetails(engine.getState().ncMode, engine.getState().manualAncStrength, next, "Wind Suppression");
        });
        llAncSubOptions.addView(btnWindNoise, windLp);

        llTransSubOptions = new LinearLayout(this);
        llTransSubOptions.setOrientation(LinearLayout.HORIZONTAL);
        llTransSubOptions.setVisibility(View.GONE);
        card.addView(llTransSubOptions, subLp);

        btnTransFull = createSmallSubBtn("");
        btnTransVocal = createSmallSubBtn("");
        btnTransFull.setOnClickListener(v -> engine.setTransparencyDetails(
                SoundcoreProtocol.TRANS_FULLY_TRANSPARENT, "Full Transparency"));
        btnTransVocal.setOnClickListener(v -> engine.setTransparencyDetails(
                SoundcoreProtocol.TRANS_VOCAL_MODE, "Vocal Mode"));

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

        tv1aTitle = new TextView(this);
        tv1aTitle.setTextColor(Color.parseColor("#00E5FF"));
        tv1aTitle.setTextSize(14);
        tv1aTitle.setTypeface(Typeface.DEFAULT_BOLD);
        txtCol.addView(tv1aTitle);

        tv1aSub = new TextView(this);
        tv1aSub.setTextColor(Color.parseColor("#94A3B8"));
        tv1aSub.setTextSize(11);
        txtCol.addView(tv1aSub);

        swAutoEq = new Switch(this);
        swAutoEq.setChecked(true);
        swAutoEq.setOnCheckedChangeListener((b, checked) -> {
            if (!updatingUiProgrammatically) engine.setAutoEqByAppEnabled(checked);
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

        LinearLayout simRow = new LinearLayout(this);
        simRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srLp.topMargin = dp(8);
        card.addView(simRow, srLp);

        btnSimYt = createSmallSubBtn("🎬 Test: YouTube (Cinema)");
        styleSelectableButton(btnSimYt, false, "#00E5FF");
        btnSimYt.setOnClickListener(v -> engine.simulateMediaAppSwitch(
                "com.google.android.youtube", "YouTube Cinema Video", "Dolby/Cinema Stage"
        ));
        btnSimYtm = createSmallSubBtn("🎵 Test: YT Music (Bass)");
        styleSelectableButton(btnSimYtm, false, "#00E5FF");
        btnSimYtm.setOnClickListener(v -> engine.simulateMediaAppSwitch(
                "com.google.android.apps.youtube.music", "Şar", "Koma Azad"
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

        tv1bTitle = new TextView(this);
        tv1bTitle.setTextColor(Color.parseColor("#C084FC"));
        tv1bTitle.setTextSize(14);
        tv1bTitle.setTypeface(Typeface.DEFAULT_BOLD);
        txtCol.addView(tv1bTitle);

        tv1bSub = new TextView(this);
        tv1bSub.setTextColor(Color.parseColor("#94A3B8"));
        tv1bSub.setTextSize(11);
        txtCol.addView(tv1bSub);

        swSmartActivity = new Switch(this);
        swSmartActivity.setChecked(true);
        swSmartActivity.setOnCheckedChangeListener((b, checked) -> {
            if (!updatingUiProgrammatically) engine.setSmartActivityAncEnabled(checked);
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

        btnActStationary = createSmallSubBtn("");
        btnActWalking = createSmallSubBtn("");
        btnActTransit = createSmallSubBtn("");

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

        tv1cTitle = new TextView(this);
        tv1cTitle.setTextColor(Color.parseColor("#34D399"));
        tv1cTitle.setTextSize(14);
        tv1cTitle.setTypeface(Typeface.DEFAULT_BOLD);
        txtCol.addView(tv1cTitle);

        tv1cSub = new TextView(this);
        tv1cSub.setTextColor(Color.parseColor("#94A3B8"));
        tv1cSub.setTextSize(11);
        txtCol.addView(tv1cSub);

        swSpeakToChat = new Switch(this);
        swSpeakToChat.setChecked(false);
        swSpeakToChat.setOnCheckedChangeListener((b, checked) -> {
            if (!updatingUiProgrammatically) engine.setSpeakToChatEnabled(checked);
        });
        top.addView(swSpeakToChat);

        tvSpeakStatus = new TextView(this);
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
        tvThresholdVal.setTextColor(Color.parseColor("#94A3B8"));
        tvThresholdVal.setTextSize(11);
        threshRow.addView(tvThresholdVal);

        SeekBar sbThresh = new SeekBar(this);
        sbThresh.setMax(65);
        sbThresh.setProgress(58 - 25);
        sbThresh.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) engine.setSpeakThresholdLevel(25 + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        threshRow.addView(sbThresh, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return card;
    }

    private View buildWhisperCallBoostCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundedBox("#11172A", "#F59E0B", 18, 1));
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

        tv1dTitle = new TextView(this);
        tv1dTitle.setTextColor(Color.parseColor("#FBBF24"));
        tv1dTitle.setTextSize(14);
        tv1dTitle.setTypeface(Typeface.DEFAULT_BOLD);
        txtCol.addView(tv1dTitle);

        tv1dSub = new TextView(this);
        tv1dSub.setTextColor(Color.parseColor("#94A3B8"));
        tv1dSub.setTextSize(11);
        txtCol.addView(tv1dSub);

        swWhisperBoost = new Switch(this);
        swWhisperBoost.setChecked(true);
        swWhisperBoost.setOnCheckedChangeListener((b, checked) -> {
            if (!updatingUiProgrammatically) engine.setWhisperCallBoostEnabled(checked);
        });
        top.addView(swWhisperBoost);

        tvWhisperStatus = new TextView(this);
        tvWhisperStatus.setPadding(dp(10), dp(8), dp(10), dp(8));
        tvWhisperStatus.setBackground(roundedBox("#182238", "#263554", 10, 1));
        tvWhisperStatus.setTextColor(Color.parseColor("#FDE68A"));
        tvWhisperStatus.setTextSize(11);
        LinearLayout.LayoutParams wsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wsLp.topMargin = dp(8);
        card.addView(tvWhisperStatus, wsLp);

        pbWhisperOut = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pbWhisperOut.setMax(100);
        pbWhisperOut.setProgress(0);
        pbWhisperOut.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B")));
        LinearLayout.LayoutParams pwLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        pwLp.topMargin = dp(6);
        card.addView(pbWhisperOut, pwLp);

        LinearLayout gainRow = new LinearLayout(this);
        gainRow.setOrientation(LinearLayout.HORIZONTAL);
        gainRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams grLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        grLp.topMargin = dp(8);
        card.addView(gainRow, grLp);

        tvWhisperGainVal = new TextView(this);
        tvWhisperGainVal.setTextColor(Color.parseColor("#94A3B8"));
        tvWhisperGainVal.setTextSize(11);
        gainRow.addView(tvWhisperGainVal);

        SeekBar sbWhisper = new SeekBar(this);
        sbWhisper.setMax(40); // 10..50 tenths (1.0x..5.0x)
        sbWhisper.setProgress(32 - 10);
        sbWhisper.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) engine.setWhisperGainMultiplierTenths(10 + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        gainRow.addView(sbWhisper, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnWhisperTest = createSmallSubBtn("");
        LinearLayout.LayoutParams btLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btLp.topMargin = dp(8);
        btnWhisperTest.setOnClickListener(v -> engine.toggleWhisperTestLoopback());
        card.addView(btnWhisperTest, btLp);

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

        tvEqTitle = new TextView(this);
        tvEqTitle.setTextColor(Color.WHITE);
        tvEqTitle.setTextSize(12);
        tvEqTitle.setTypeface(Typeface.DEFAULT_BOLD);
        topRow.addView(tvEqTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        tvBassLbl = new TextView(this);
        tvBassLbl.setTextColor(Color.parseColor("#F59E0B"));
        tvBassLbl.setTextSize(11);
        tvBassLbl.setTypeface(Typeface.DEFAULT_BOLD);
        topRow.addView(tvBassLbl);

        swBassUp = new Switch(this);
        swBassUp.setChecked(true);
        swBassUp.setOnCheckedChangeListener((b, checked) -> {
            if (!updatingUiProgrammatically) engine.setBassUpEnabled(checked);
        });
        topRow.addView(swBassUp);

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

        for (int i = 0; i < 6; i++) {
            final int presetIdx = i;
            TextView chip = createSmallSubBtn("");
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cLp.rightMargin = dp(8);
            chip.setOnClickListener(v -> engine.applyEqPreset(presetIdx, engine.isBassUpEnabled(), "EQ Selected"));
            presetButtons[i] = chip;
            presetRow.addView(chip, cLp);
        }

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
            sb.setMax(240);
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
                        engine.setCustomEqBand(bandIdx, seekBar.getProgress() - 120);
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

        tvTelemetryTitle = new TextView(this);
        tvTelemetryTitle.setTextColor(Color.parseColor("#38BDF8"));
        tvTelemetryTitle.setTextSize(12);
        tvTelemetryTitle.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(tvTelemetryTitle);

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
            String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
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
            String lang = I18n.getActiveLang();

            // Update Multi-Language Pills
            for (int i = 0; i < I18n.SUPPORTED_CODES.length; i++) {
                boolean selected = I18n.SUPPORTED_CODES[i].equals(I18n.getLanguageOverride());
                styleSelectableButton(langButtons[i], selected, "#00E5FF");
            }

            // Localized static texts
            tvAppTitle.setText(I18n.t("app_title"));
            btnReconnect.setText(I18n.t("sync_btn"));
            btnGrantNotifAccess.setText(I18n.t("grant_notif_btn"));
            btnGrantNotifAccess.setVisibility(isNotificationListenerEnabled() ? View.GONE : View.VISIBLE);

            tvLeftBattLbl.setText(I18n.t("left_earbud"));
            tvRightBattLbl.setText(I18n.t("right_earbud"));
            tvCaseBattLbl.setText(I18n.t("charging_case"));

            tvAncSecTitle.setText(I18n.t("anc_section_title"));
            btnModeAnc.setText(I18n.t("mode_anc"));
            btnModeTrans.setText(I18n.t("mode_trans"));
            btnModeNormal.setText(I18n.t("mode_normal"));

            btnAncWeak.setText(I18n.t("anc_weak"));
            btnAncMod.setText(I18n.t("anc_mod"));
            btnAncStrong.setText(I18n.t("anc_strong"));
            btnAncAdaptive.setText(I18n.t("anc_adaptive"));

            btnTransFull.setText(I18n.t("trans_full"));
            btnTransVocal.setText(I18n.t("trans_vocal"));

            tvAutoHeader.setText(I18n.t("autonomous_header"));
            tv1aTitle.setText(I18n.t("card_1a_title"));
            tv1aSub.setText(I18n.t("card_1a_sub"));
            tv1bTitle.setText(I18n.t("card_1b_title"));
            tv1bSub.setText(I18n.t("card_1b_sub"));
            tv1cTitle.setText(I18n.t("card_1c_title"));
            tv1cSub.setText(I18n.t("card_1c_sub"));
            tv1dTitle.setText(I18n.t("card_1d_title"));
            tv1dSub.setText(I18n.t("card_1d_sub"));

            btnActStationary.setText(I18n.t("btn_stationary"));
            btnActWalking.setText(I18n.t("btn_walking"));
            btnActTransit.setText(I18n.t("btn_transit"));

            tvEqTitle.setText(I18n.t("eq_section_title"));
            tvBassLbl.setText(I18n.t("cinema_boost_label"));
            tvTelemetryTitle.setText(I18n.t("telemetry_title"));

            // Dynamic state
            SoundcoreProtocol.DeviceState st = engine.getState();
            boolean masterOn = engine.isMasterAppEnabled();
            swMasterPower.setChecked(masterOn);
            swAutoBt.setChecked(engine.isAutoConnectOnBt());
            tvAutoBtLbl.setText(I18n.t("auto_bt_lbl"));

            if (masterOn) {
                tvMasterPowerLbl.setText(I18n.t("power_active_lbl"));
                tvMasterPowerLbl.setTextColor(Color.parseColor("#34D399"));
                cardPowerControl.setBackground(roundedBox("#11172A", "#1E293B", 16, 1));
                btnHeaderPower.setBackground(roundedBox("#7F1D1D", "#F87171", 12, 1));
                btnPowerStopOrResume.setText(I18n.t("btn_power_stop"));
                btnPowerStopOrResume.setBackground(roundedBox("#450A0A", "#F87171", 12, 1));
                btnPowerStopOrResume.setTextColor(Color.parseColor("#FCA5A5"));
            } else {
                tvMasterPowerLbl.setText(I18n.t("power_off_lbl"));
                tvMasterPowerLbl.setTextColor(Color.parseColor("#F87171"));
                cardPowerControl.setBackground(roundedBox("#281117", "#F87171", 16, 1));
                btnHeaderPower.setBackground(roundedBox("#064E3B", "#34D399", 12, 1));
                btnPowerStopOrResume.setText(I18n.t("btn_power_resume"));
                btnPowerStopOrResume.setBackground(roundedBox("#064E3B", "#34D399", 12, 1));
                btnPowerStopOrResume.setTextColor(Color.parseColor("#6EE7B7"));
            }

            tvConnStatus.setText(engine.getConnectionStatusText());
            tvConnStatus.setTextColor(!masterOn
                    ? Color.parseColor("#F87171")
                    : (st.connected ? Color.parseColor("#34D399") : Color.parseColor("#FBBF24")));

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

            btnWindNoise.setText(st.windNoiseSuppression ? I18n.t("wind_on") : I18n.t("wind_off"));
            styleSelectableButton(btnWindNoise, st.windNoiseSuppression, "#38BDF8");

            styleSelectableButton(btnTransFull, st.transparencyMode == SoundcoreProtocol.TRANS_FULLY_TRANSPARENT, "#34D399");
            styleSelectableButton(btnTransVocal, st.transparencyMode == SoundcoreProtocol.TRANS_VOCAL_MODE, "#34D399");

            // #1A Auto-EQ
            swAutoEq.setChecked(engine.isAutoEqByAppEnabled());
            String pkg = engine.getCurrentMediaPackage();
            String friendlyApp = pkg.contains("youtube.music") ? "YouTube Music"
                    : pkg.contains("youtube") ? "YouTube"
                    : pkg.contains("spotify") ? "Spotify" : pkg;
            String[] presetNames = I18n.getPresetNames();
            tvNowPlayingInfo.setText("🎶 " + friendlyApp + "\n"
                    + engine.getCurrentMediaTitle() + " — " + engine.getCurrentMediaArtist());
            tvAutoEqRuleStatus.setText("⚡ " + presetNames[engine.getCurrentPresetIndex()]
                    + (engine.isBassUpEnabled() ? " + 🎬 Cinema Depth" : ""));

            // #1B Smart Step-Counter & Speed
            swSmartActivity.setChecked(engine.isSmartActivityAncEnabled());
            int actState = engine.getCurrentActivityState();
            String stepLabel = I18n.pick(lang,
                    String.format(Locale.US, "🚶 Toplam Adım: %d • Aktif Pencere: %d/8 adım • Hız: %.1f km/s • İvme: %.2f m/s²",
                            engine.getTotalSessionSteps(), engine.getRecentStepsCount(), engine.getCurrentSpeedKmh(), engine.getCurrentMovementRms()),
                    String.format(Locale.US, "🚶 Total Steps: %d • Active Window: %d/8 steps • Speed: %.1f km/h • Accel: %.2f m/s²",
                            engine.getTotalSessionSteps(), engine.getRecentStepsCount(), engine.getCurrentSpeedKmh(), engine.getCurrentMovementRms()),
                    String.format(Locale.US, "🚶 Schritte: %d • Aktiv: %d/8 Schritte • Geschw.: %.1f km/h",
                            engine.getTotalSessionSteps(), engine.getRecentStepsCount(), engine.getCurrentSpeedKmh()),
                    String.format(Locale.US, "🚶 Pasos: %d • Ventana: %d/8 pasos • Vel.: %.1f km/h",
                            engine.getTotalSessionSteps(), engine.getRecentStepsCount(), engine.getCurrentSpeedKmh()),
                    String.format(Locale.US, "🚶 Pas: %d • Fenêtre: %d/8 pas • Vitesse: %.1f km/h",
                            engine.getTotalSessionSteps(), engine.getRecentStepsCount(), engine.getCurrentSpeedKmh()),
                    String.format(Locale.US, "🚶 Шаги: %d • Окно: %d/8 шагов • Скор.: %.1f км/ч",
                            engine.getTotalSessionSteps(), engine.getRecentStepsCount(), engine.getCurrentSpeedKmh()),
                    String.format(Locale.US, "🚶 الخطوات: %d • النافذة النشطة: %d/8 • السرعة: %.1f كم/س",
                            engine.getTotalSessionSteps(), engine.getRecentStepsCount(), engine.getCurrentSpeedKmh())
            );
            tvActivityTelemetry.setText(stepLabel);
            styleSelectableButton(btnActStationary, actState == SmartAutomationEngine.ACTIVITY_STATIONARY, "#C084FC");
            styleSelectableButton(btnActWalking, actState == SmartAutomationEngine.ACTIVITY_WALKING_RUNNING, "#C084FC");
            styleSelectableButton(btnActTransit, actState == SmartAutomationEngine.ACTIVITY_TRANSIT_FAST, "#C084FC");

            // #1C Speak-to-Chat
            swSpeakToChat.setChecked(engine.isSpeakToChatEnabled());
            pbMicLevel.setProgress(engine.getCurrentMicDbLevel());
            tvThresholdVal.setText(I18n.pick(lang,
                    "Eşik: " + engine.getSpeakThresholdLevel() + " dB",
                    "Threshold: " + engine.getSpeakThresholdLevel() + " dB",
                    "Schwelle: " + engine.getSpeakThresholdLevel() + " dB",
                    "Umbral: " + engine.getSpeakThresholdLevel() + " dB",
                    "Seuil: " + engine.getSpeakThresholdLevel() + " dB",
                    "Порог: " + engine.getSpeakThresholdLevel() + " дБ",
                    "الحد: " + engine.getSpeakThresholdLevel() + " dB"));
            if (engine.isSpeakTriggeredActive()) {
                tvSpeakStatus.setText("🗣️ " + I18n.pick(lang,
                        "KONUŞMA ALGILANDI! Vokal Şeffaf Mod • ANC'ye dönüş: " + engine.getSpeakCountdownSec() + " sn",
                        "SPEECH DETECTED! Vocal Transparency • Restoring ANC in " + engine.getSpeakCountdownSec() + "s",
                        "SPRACHE ERKANNT! Transparenz • ANC in " + engine.getSpeakCountdownSec() + "s",
                        "¡VOZ DETECTADA! Transparencia • ANC en " + engine.getSpeakCountdownSec() + "s",
                        "VOIX DÉTECTÉE ! Transparence • ANC dans " + engine.getSpeakCountdownSec() + "s",
                        "РЕЧЬ ОБНАРУЖЕНА! Прозрачность • ANC через " + engine.getSpeakCountdownSec() + " сек",
                        "تم اكتشاف التحدث! وضع الشفافية • العودة خلال " + engine.getSpeakCountdownSec() + " ث"));
                tvSpeakStatus.setTextColor(Color.parseColor("#34D399"));
            } else if (engine.isSpeakToChatEnabled()) {
                tvSpeakStatus.setText("🎙️ " + engine.getCurrentMicDbLevel() + " dB");
                tvSpeakStatus.setTextColor(Color.parseColor("#E2E8F0"));
            } else {
                tvSpeakStatus.setText(I18n.pick(lang,
                        "Mikrofon Dinleme: Kapalı",
                        "Voice Monitoring: Off",
                        "Spracherkennung: Aus",
                        "Monitoreo de Voz: Apagado",
                        "Écoute Vocale : Désactivée",
                        "Мониторинг речи: Выкл",
                        "مراقبة الصوت: متوقفة"));
                tvSpeakStatus.setTextColor(Color.parseColor("#94A3B8"));
            }

            // #1D Whisper & Call Mic Booster
            swWhisperBoost.setChecked(engine.isWhisperCallBoostEnabled());
            float mult = engine.getWhisperGainMultiplierTenths() / 10.0f;
            tvWhisperGainVal.setText(String.format(Locale.US, "Gain: %.1fx (+%.0f dB)", mult, 20.0 * Math.log10(mult)));
            boolean wActive = engine.isWhisperTestLoopbackActive();
            btnWhisperTest.setText(wActive ? I18n.t("whisper_test_on") : I18n.t("whisper_test_off"));
            styleSelectableButton(btnWhisperTest, wActive, "#F59E0B");
            if (wActive) {
                pbWhisperOut.setProgress(engine.getCurrentWhisperOutputDb());
                tvWhisperStatus.setText(String.format(Locale.US,
                        "🎙️ Fısıltı Girişi: %d dB ➡️ Güçlendirilmiş Çıkış: %d dB (AGC + Gürültü Filtresi Aktif)",
                        engine.getCurrentWhisperInputDb(), engine.getCurrentWhisperOutputDb()));
            } else {
                pbWhisperOut.setProgress(engine.isWhisperCallBoostEnabled() ? 100 : 0);
                tvWhisperStatus.setText(I18n.pick(lang,
                        "⚡ HFP Mikrofon Kazancı: 15/15 (Maks) • HD Ses (16kHz mSBC) • Donanım AGC + Rüzgar Filtresi: HAZIR",
                        "⚡ HFP Mic Gain: 15/15 (Max) • HD Voice (16kHz mSBC) • Hardware AGC + Wind Filter: READY",
                        "⚡ HFP-Mikrofon: 15/15 (Max) • HD-Voice (16kHz) • Hardware-AGC: BEREIT",
                        "⚡ Ganancia Mic HFP: 15/15 (Máx) • Voz HD (16kHz) • AGC Hardware: LISTO",
                        "⚡ Gain Micro HFP: 15/15 (Max) • Voix HD (16kHz) • AGC Matériel: PRÊT",
                        "⚡ Усиление микрофона HFP: 15/15 (Макс) • HD Voice • Аппаратный AGC: ГОТОВ",
                        "⚡ حساسية ميكروفون HFP: 15/15 (الأقصى) • صوت HD • AGC المادي: جاهز"));
            }

            // Equalizer Studio
            swBassUp.setChecked(engine.isBassUpEnabled());
            for (int i = 0; i < presetButtons.length; i++) {
                if (presetButtons[i] != null) {
                    presetButtons[i].setText(presetNames[i]);
                    styleSelectableButton(presetButtons[i], i == engine.getCurrentPresetIndex(), "#00E5FF");
                }
            }
            for (int b = 0; b < 8; b++) {
                int tenthsDb = st.eqBandsTenthsDb[b];
                eqSeekBars[b].setProgress(tenthsDb + 120);
                eqValueLabels[b].setText(String.format(Locale.US, "%+.1f dB", tenthsDb / 10.0f));
            }

            // Telemetry Console
            tvTxRxHex.setText("TX : " + engine.getLastTxHex() + "\nRX : " + engine.getLastRxHex());
            List<String> logs = engine.getTelemetryLogs();
            StringBuilder sb = new StringBuilder();
            int count = Math.min(8, logs.size());
            for (int i = 0; i < count; i++) {
                sb.append(logs.get(i)).append("\n");
            }
            tvTelemetryLogs.setText(sb.toString().trim());
        } finally {
            updatingUiProgrammatically = false;
        }
    }
}
