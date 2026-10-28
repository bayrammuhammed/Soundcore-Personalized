package com.bayram.soundcorex20;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.List;

public class MediaNotificationListener extends NotificationListenerService {

    private static MediaNotificationListener instance;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
        // Ensure SmartAutomationEngine is initialized and monitoring Bluetooth in the background 24/7
        SmartAutomationEngine.getInstance(getApplicationContext());
        pollActiveMediaSessions(this);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        if (instance == this) {
            instance = null;
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        pollActiveMediaSessions(this);
    }

    public static void pollActiveMediaSessions(Context context) {
        try {
            MediaSessionManager msm = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (msm == null) return;
            ComponentName cn = new ComponentName(context, MediaNotificationListener.class);
            List<MediaController> controllers = msm.getActiveSessions(cn);
            if (controllers == null || controllers.isEmpty()) return;

            MediaController chosen = null;
            for (MediaController mc : controllers) {
                PlaybackState ps = mc.getPlaybackState();
                if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) {
                    chosen = mc;
                    break;
                }
            }
            if (chosen == null) {
                chosen = controllers.get(0);
            }

            String pkg = chosen.getPackageName();
            String title = "";
            String artist = "";
            boolean playing = false;
            PlaybackState ps = chosen.getPlaybackState();
            if (ps != null) {
                playing = (ps.getState() == PlaybackState.STATE_PLAYING);
            }
            MediaMetadata meta = chosen.getMetadata();
            if (meta != null) {
                CharSequence t = meta.getText(MediaMetadata.METADATA_KEY_TITLE);
                CharSequence a = meta.getText(MediaMetadata.METADATA_KEY_ARTIST);
                if (t != null) title = t.toString();
                if (a != null) artist = a.toString();
            }

            SmartAutomationEngine engine = SmartAutomationEngine.getInstance(context);
            if (engine != null) {
                engine.onMediaSessionDetected(pkg, title, artist, playing);
            }
        } catch (Throwable ignored) {
        }
    }
}
