package com.bayram.soundcorex20;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class X20QuickTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        SmartAutomationEngine engine = SmartAutomationEngine.getInstance(getApplicationContext());
        int current = engine.getState().ambientMode;
        int next;
        if (current == SoundcoreProtocol.AMBIENT_ANC) {
            next = SoundcoreProtocol.AMBIENT_TRANSPARENCY;
        } else if (current == SoundcoreProtocol.AMBIENT_TRANSPARENCY) {
            next = SoundcoreProtocol.AMBIENT_NORMAL;
        } else {
            next = SoundcoreProtocol.AMBIENT_ANC;
        }
        engine.setAmbientMode(next, "Hızlı Ayarlar (Quick Tile)");
        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;
        SmartAutomationEngine engine = SmartAutomationEngine.getInstance(getApplicationContext());
        int mode = engine.getState().ambientMode;
        tile.setState(Tile.STATE_ACTIVE);
        if (mode == SoundcoreProtocol.AMBIENT_ANC) {
            tile.setLabel("X20: ANC Aktif");
            tile.setSubtitle("Gürültü Engelleme");
        } else if (mode == SoundcoreProtocol.AMBIENT_TRANSPARENCY) {
            tile.setLabel("X20: Şeffaf Mod");
            tile.setSubtitle("Dış Ses Açık");
        } else {
            tile.setLabel("X20: Normal Mod");
            tile.setSubtitle("Pasif İzolasyon");
        }
        tile.updateTile();
    }
}
