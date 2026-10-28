package com.bayram.soundcorex20;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.util.Locale;

/**
 * Automatically detects when soundcore Sport X20 connects/disconnects over Bluetooth
 * (e.g. taken out of the charging case) and establishes the RFCOMM control channel automatically.
 */
public class BluetoothAutoConnectReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        SmartAutomationEngine engine = SmartAutomationEngine.getInstance(context.getApplicationContext());

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (isTargetX20(device)) {
                engine.addLog("📡 Sport X20 kutudan çıkarıldı (ACL Bağlandı) -> Otomatik bağlanılıyor...");
                scheduleReconnectAttempts(engine);
            }
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (isTargetX20(device)) {
                engine.onEarbudsDisconnected("🔌 Kulaklık kutuya konuldu / Bluetooth bağlantısı kesildi.");
            }
        } else if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (isTargetX20(device)) {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    engine.addLog("🎧 A2DP Ses Kanalı aktif -> RFCOMM otomatik bağlanıyor...");
                    scheduleReconnectAttempts(engine);
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    engine.onEarbudsDisconnected("🔌 A2DP bağlantısı kesildi.");
                }
            }
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            if (state == BluetoothAdapter.STATE_ON) {
                engine.addLog("📶 Bluetooth açıldı -> Kulaklık aranıyor...");
                scheduleReconnectAttempts(engine);
            } else if (state == BluetoothAdapter.STATE_OFF) {
                engine.onEarbudsDisconnected("📶 Bluetooth kapatıldı.");
            }
        }
    }

    public static boolean isTargetX20(BluetoothDevice device) {
        if (device == null) return true;
        return SoundcoreProtocol.isSoundcoreDevice(device);
    }

    private void scheduleReconnectAttempts(SmartAutomationEngine engine) {
        Handler h = new Handler(Looper.getMainLooper());
        // Try at 1.2s, 3.0s, and 6.0s after ACL connect (gives earbuds firmware time to initialize SPP/RFCOMM)
        h.postDelayed(engine::connectToEarbuds, 1200L);
        h.postDelayed(engine::connectToEarbuds, 3000L);
        h.postDelayed(engine::connectToEarbuds, 6000L);
    }
}
