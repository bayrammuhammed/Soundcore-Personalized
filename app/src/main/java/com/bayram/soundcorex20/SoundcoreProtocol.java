package com.bayram.soundcorex20;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * Universal Anker Soundcore RFCOMM Protocol Engine.
 * Supports Soundcore Sport X20 (A3968) and all other Soundcore TWS & Headphone models
 * (Liberty 4 NC, Space A40, Life Q30/Q35/Q45, P40i, R50i, Life P3, Space One, etc.).
 */
public class SoundcoreProtocol {

    public static final String ANKER_UUID_PREFIX = "0cf12d31-";
    public static final UUID X20_RFCOMM_UUID = UUID.fromString("0cf12d31-fac3-4553-bd80-d6832e7b3968");
    public static final UUID SPP_FALLBACK_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public static final String TARGET_MAC = "B0:38:E2:FC:55:7E";

    // Ambient Sound Modes
    public static final int AMBIENT_ANC = 0;
    public static final int AMBIENT_TRANSPARENCY = 1;
    public static final int AMBIENT_NORMAL = 2;

    // Manual ANC Strength (1..3)
    public static final int ANC_STRENGTH_WEAK = 1;
    public static final int ANC_STRENGTH_MODERATE = 2;
    public static final int ANC_STRENGTH_STRONG = 3;

    // Adaptive ANC Level (0..2)
    public static final int ADAPTIVE_LOW = 0;
    public static final int ADAPTIVE_MEDIUM = 1;
    public static final int ADAPTIVE_HIGH = 2;

    // Transparency Modes (0..1)
    public static final int TRANS_FULLY_TRANSPARENT = 0;
    public static final int TRANS_VOCAL_MODE = 1;

    // Noise Canceling Sub-mode (0..1)
    public static final int NC_MODE_MANUAL = 0;
    public static final int NC_MODE_ADAPTIVE = 1;

    public static class DeviceState {
        public boolean connected = false;
        public boolean twsConnected = true;
        public String deviceName = "soundcore Sport X20";
        public String deviceMac = TARGET_MAC;
        public String modelCode = "A3968";
        public boolean uses4ByteSoundMode = false;

        public int hostDevice = 0;
        public int leftBattery = 5; // 0..5
        public int rightBattery = 5; // 0..5
        public int caseBattery = 5; // 0..5
        public boolean leftCharging = false;
        public boolean rightCharging = false;
        public String firmwareLeft = "01.65";
        public String firmwareRight = "01.65";
        public String serialNumber = "3968B038E2FC557E";

        public int ambientMode = AMBIENT_ANC;
        public int manualAncStrength = ANC_STRENGTH_STRONG;
        public int adaptiveAncLevel = ADAPTIVE_HIGH;
        public int transparencyMode = TRANS_VOCAL_MODE;
        public int ncMode = NC_MODE_MANUAL;
        public boolean windNoiseSuppression = false;
        public boolean windNoiseDetected = false;
        public boolean surroundSound = false;
        public boolean dualConnections = true;

        public int eqPresetId = 0;
        public int[] eqBandsTenthsDb = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
    }

    /**
     * Checks if a BluetoothDevice is a Soundcore / Anker audio device by name, UUID, or MAC.
     */
    public static boolean isSoundcoreDevice(BluetoothDevice device) {
        if (device == null) return false;
        try {
            String mac = device.getAddress();
            if (TARGET_MAC.equalsIgnoreCase(mac)) return true;

            String name = device.getName();
            if (name != null) {
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("soundcore") || lower.contains("sport x")
                        || lower.contains("liberty") || lower.contains("space ")
                        || lower.contains("life q") || lower.contains("life p")
                        || lower.contains("life a") || lower.contains("aerofit")
                        || lower.contains("r50i") || lower.contains("p20i")
                        || lower.contains("p30i") || lower.contains("p40i")
                        || lower.contains("v30i") || lower.contains("anker")) {
                    return true;
                }
            }

            ParcelUuid[] uuids = device.getUuids();
            if (uuids != null) {
                for (ParcelUuid pu : uuids) {
                    if (pu != null && pu.getUuid() != null) {
                        String u = pu.getUuid().toString().toLowerCase(Locale.ROOT);
                        if (u.startsWith(ANKER_UUID_PREFIX) || u.startsWith("66666666-")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Dynamically finds the model-specific 0cf12d31-... RFCOMM UUID advertised by any Soundcore device.
     */
    public static UUID findDeviceRfcommUuid(BluetoothDevice device) {
        if (device != null) {
            try {
                ParcelUuid[] uuids = device.getUuids();
                if (uuids != null) {
                    for (ParcelUuid pu : uuids) {
                        if (pu != null && pu.getUuid() != null) {
                            String u = pu.getUuid().toString().toLowerCase(Locale.ROOT);
                            if (u.startsWith(ANKER_UUID_PREFIX)) {
                                return pu.getUuid();
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return X20_RFCOMM_UUID;
    }

    public static byte calculateChecksum(byte[] data, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum = (sum + (data[i] & 0xFF)) & 0xFF;
        }
        return (byte) sum;
    }

    public static byte[] buildOutboundPacket(int cmdHi, int cmdLo, byte[] body) {
        int bodyLen = (body != null) ? body.length : 0;
        int totalLen = 5 + 2 + 2 + bodyLen + 1;
        byte[] packet = new byte[totalLen];
        packet[0] = (byte) 0x08;
        packet[1] = (byte) 0xEE;
        packet[2] = (byte) 0x00;
        packet[3] = (byte) 0x00;
        packet[4] = (byte) 0x00;
        packet[5] = (byte) (cmdHi & 0xFF);
        packet[6] = (byte) (cmdLo & 0xFF);
        packet[7] = (byte) (totalLen & 0xFF);
        packet[8] = (byte) ((totalLen >> 8) & 0xFF);
        if (bodyLen > 0) {
            System.arraycopy(body, 0, packet, 9, bodyLen);
        }
        packet[totalLen - 1] = calculateChecksum(packet, totalLen - 1);
        return packet;
    }

    public static byte[] buildRequestStatePacket() {
        return buildOutboundPacket(0x01, 0x01, new byte[0]);
    }

    /**
     * Command [0x06, 0x81]: Set Sound Modes (Gen 2 6-byte format for Sport X20, Liberty 4 NC, P40i, etc.)
     */
    public static byte[] buildSetSoundModesPacket(
            int ambientMode,
            int manualStrength,
            int adaptiveLevel,
            int transparencyMode,
            int ncMode,
            boolean windSuppression
    ) {
        int clampedManual = Math.max(1, Math.min(3, manualStrength));
        int clampedAdaptive = Math.max(0, Math.min(2, adaptiveLevel));
        byte[] body = new byte[]{
                (byte) (ambientMode & 0xFF),
                (byte) (((clampedManual & 0x0F) << 4) | (clampedAdaptive & 0x0F)),
                (byte) (transparencyMode & 0xFF),
                (byte) (ncMode & 0xFF),
                (byte) (windSuppression ? 0x01 : 0x00),
                (byte) 0xFF
        };
        return buildOutboundPacket(0x06, 0x81, body);
    }

    /**
     * Command [0x06, 0x81]: 4-byte Sound Modes format for Gen 1 models (Life Q30, Q35, Life P3, Space A40).
     */
    public static byte[] buildSetSoundModes4BytePacket(
            int ambientMode,
            int manualStrength,
            int transparencyMode
    ) {
        // Map manualStrength (1=Weak, 2=Mod, 3=Strong) to Gen 1 Indoor(2)/Outdoor(1)/Transport(0)
        int gen1NcMode = (manualStrength == ANC_STRENGTH_WEAK) ? 2 : (manualStrength == ANC_STRENGTH_MODERATE) ? 1 : 0;
        byte[] body = new byte[]{
                (byte) (ambientMode & 0xFF),
                (byte) (gen1NcMode & 0xFF),
                (byte) (transparencyMode & 0xFF),
                (byte) 0x00
        };
        return buildOutboundPacket(0x06, 0x81, body);
    }

    public static int[] applyDrc10Bands(int[] adjustments8TenthsDb) {
        double[] adj = new double[8];
        for (int i = 0; i < 8; i++) {
            adj[i] = adjustments8TenthsDb[i] / 10.0;
        }
        final double SMALLER = 0.85;
        final double LARGER = 0.95;
        double lowsSub = adj[2] * 0.81 * SMALLER;
        double highsSub = adj[5] * 0.81 * SMALLER;

        double[][] coeff = new double[][]{
                {1.26, -0.71 * SMALLER, 0.177, -0.0494, 0.0345, -0.0197, 0.0075, -0.00217},
                {-0.71 * SMALLER, 1.73 * LARGER, 0.0, 0.204, -0.068, 0.045, -0.0235, 0.0075},
                {0.177, -0.81 * SMALLER, 1.73 * LARGER, -0.81 * SMALLER, 0.208, -0.07, 0.045, -0.0197},
                {-0.0494, 0.204, 0.0, 1.73 * LARGER, -0.82 * SMALLER, 0.208, -0.068, 0.0345},
                {0.0345, -0.068, 0.208, -0.82 * SMALLER, 1.73 * LARGER, 0.0, 0.204, -0.0494},
                {-0.0197, 0.045, -0.07, 0.208, -0.81 * SMALLER, 1.73 * LARGER, -0.81 * SMALLER, 0.177},
                {0.0075, -0.0235, 0.045, -0.068, 0.204, 0.0, 1.83 * LARGER, -0.71 * SMALLER},
                {-0.00217, 0.0075, -0.0197, 0.0345, -0.0494, 0.177, -0.71 * SMALLER, 1.5}
        };

        int[] result = new int[10];
        for (int b = 0; b < 8; b++) {
            double acc = 0.0;
            for (int i = 0; i < 8; i++) {
                if ((b == 1 || b == 3) && i == 2) {
                    acc -= lowsSub;
                } else if ((b == 4 || b == 6) && i == 5) {
                    acc -= highsSub;
                } else {
                    acc += adj[i] * coeff[b][i];
                }
            }
            int v = (int) Math.round((acc / 10.0) * 10.0);
            result[b] = Math.max(-120, Math.min(134, v));
        }
        result[8] = 0;
        result[9] = -120;
        return result;
    }

    private static byte adjustmentToByte(int tenthsDb) {
        int clamped = Math.max(-120, Math.min(134, tenthsDb));
        return (byte) ((clamped + 120) & 0xFF);
    }

    /**
     * Command [0x03, 0x86]: Set Equalizer & DRC for stereo 10-band TWS (Sport X20, Liberty, P40i, etc.)
     */
    public static byte[] buildSetEqualizerPacket(int presetId, int[] bands8TenthsDb) {
        int[] raw10 = new int[10];
        for (int i = 0; i < 8; i++) {
            raw10[i] = (i < bands8TenthsDb.length) ? bands8TenthsDb[i] : 0;
        }
        raw10[8] = 0;
        raw10[9] = -120;
        int[] drc10 = applyDrc10Bands(raw10);

        byte[] body = new byte[90];
        int idx = 0;
        body[idx++] = (byte) (presetId & 0xFF);
        body[idx++] = (byte) ((presetId >> 8) & 0xFF);

        for (int ch = 0; ch < 2; ch++) {
            for (int b = 0; b < 10; b++) {
                body[idx++] = adjustmentToByte(raw10[b]);
            }
        }
        body[idx++] = (byte) 0xFF;
        body[idx++] = (byte) 0xFF;
        body[idx++] = (byte) 0x00;

        for (int i = 0; i < 20; i++) body[idx++] = (byte) 0xFF;
        for (int i = 0; i < 4; i++) body[idx++] = (byte) 0x00;
        body[idx++] = (byte) 0x00;
        for (int i = 0; i < 20; i++) body[idx++] = (byte) 0xFF;

        for (int ch = 0; ch < 2; ch++) {
            for (int b = 0; b < 10; b++) {
                body[idx++] = adjustmentToByte(drc10[b]);
            }
        }
        return buildOutboundPacket(0x03, 0x86, body);
    }

    /**
     * Command [0x02, 0x81]: Set Equalizer for simpler 8-band Soundcore headphones (Life Q30, Q20i, etc.)
     */
    public static byte[] buildSimpleEqualizerPacket(int presetId, int[] bands8TenthsDb) {
        byte[] body = new byte[10];
        body[0] = (byte) (presetId & 0xFF);
        body[1] = (byte) ((presetId >> 8) & 0xFF);
        for (int i = 0; i < 8; i++) {
            body[2 + i] = adjustmentToByte(i < bands8TenthsDb.length ? bands8TenthsDb[i] : 0);
        }
        return buildOutboundPacket(0x02, 0x81, body);
    }

    public static boolean parseInboundPacket(byte[] buffer, int length, DeviceState state) {
        if (length < 10) return false;
        boolean anyParsed = false;
        int offset = 0;
        while (offset + 10 <= length) {
            if ((buffer[offset] & 0xFF) != 0x09 || (buffer[offset + 1] & 0xFF) != 0xFF) {
                offset++;
                continue;
            }
            int pktLen = (buffer[offset + 7] & 0xFF) | ((buffer[offset + 8] & 0xFF) << 8);
            if (pktLen < 10 || offset + pktLen > length) {
                pktLen = length - offset;
            }
            byte[] single = Arrays.copyOfRange(buffer, offset, offset + pktLen);
            if (parseSingleFrame(single, pktLen, state)) {
                anyParsed = true;
            }
            offset += Math.max(10, pktLen);
        }
        return anyParsed;
    }

    private static boolean parseSingleFrame(byte[] packet, int length, DeviceState state) {
        if (length < 10) return false;
        if ((packet[0] & 0xFF) != 0x09 || (packet[1] & 0xFF) != 0xFF) return false;

        int cmdHi = packet[5] & 0xFF;
        int cmdLo = packet[6] & 0xFF;
        int pktLen = (packet[7] & 0xFF) | ((packet[8] & 0xFF) << 8);
        if (pktLen > length || pktLen < 10) {
            pktLen = length;
        }
        int bodyOffset = 9;
        int bodyLen = pktLen - 10;
        if (bodyLen <= 0) {
            return false;
        }

        // Command [0x01, 0x01]: Full State Update
        if (cmdHi == 0x01 && cmdLo == 0x01) {
            // Case A: TWS Earbuds with ~125-143+ byte state packet (Sport X20 A3968, Liberty 4 NC, P40i, etc.)
            if (bodyLen >= 120) {
                state.uses4ByteSoundMode = false;
                state.hostDevice = packet[bodyOffset] & 0xFF;
                state.twsConnected = (packet[bodyOffset + 1] & 0xFF) != 0;
                state.leftBattery = Math.min(5, packet[bodyOffset + 2] & 0xFF);
                state.rightBattery = Math.min(5, packet[bodyOffset + 3] & 0xFF);
                state.leftCharging = (packet[bodyOffset + 4] & 0xFF) != 0;
                state.rightCharging = (packet[bodyOffset + 5] & 0xFF) != 0;

                state.firmwareLeft = new String(packet, bodyOffset + 6, 5, StandardCharsets.US_ASCII);
                state.firmwareRight = new String(packet, bodyOffset + 11, 5, StandardCharsets.US_ASCII);
                state.serialNumber = new String(packet, bodyOffset + 16, 16, StandardCharsets.US_ASCII);
                if (state.serialNumber.length() >= 4) {
                    state.modelCode = "A" + state.serialNumber.substring(0, 4);
                }

                int caseB = packet[bodyOffset + 37] & 0xFF;
                if (caseB <= 5) state.caseBattery = caseB;

                state.eqPresetId = (packet[bodyOffset + 38] & 0xFF) | ((packet[bodyOffset + 39] & 0xFF) << 8);
                for (int i = 0; i < 8; i++) {
                    int rawByte = packet[bodyOffset + 40 + i] & 0xFF;
                    if (rawByte != 0xFF) {
                        state.eqBandsTenthsDb[i] = rawByte - 120;
                    }
                }

                parseSoundModesBlock(packet, bodyOffset + 117, state);
                if (bodyLen >= 127) {
                    state.surroundSound = (packet[bodyOffset + 124] & 0xFF) != 0;
                    state.dualConnections = (packet[bodyOffset + 126] & 0xFF) != 0;
                }
                return true;
            }
            // Case B: Smaller state packets (Headphones like Life Q30/Q35/Q45 or compact TWS)
            else if (bodyLen >= 25) {
                state.uses4ByteSoundMode = true;
                int b0 = packet[bodyOffset] & 0xFF;
                int b1 = packet[bodyOffset + 1] & 0xFF;
                if (b0 <= 5 && b1 <= 1) {
                    // Single battery headphone (level, charging)
                    state.leftBattery = b0;
                    state.rightBattery = b0;
                    state.leftCharging = (b1 != 0);
                    state.rightCharging = (b1 != 0);
                } else if (bodyLen >= 6) {
                    state.leftBattery = Math.min(5, packet[bodyOffset + 2] & 0xFF);
                    state.rightBattery = Math.min(5, packet[bodyOffset + 3] & 0xFF);
                }
                return true;
            }
        }

        // Command [0x06, 0x01]: Sound mode changed from physical button press
        if (cmdHi == 0x06 && cmdLo == 0x01 && bodyLen >= 4) {
            if (bodyLen >= 5 && !state.uses4ByteSoundMode) {
                parseSoundModesBlock(packet, bodyOffset, state);
            } else {
                int amb = packet[bodyOffset] & 0xFF;
                if (amb <= 2) state.ambientMode = amb;
                int trans = packet[bodyOffset + 2] & 0xFF;
                if (trans <= 1) state.transparencyMode = trans;
            }
            return true;
        }

        // Command [0x01, 0x03]: Battery level update
        if (cmdHi == 0x01 && cmdLo == 0x03 && bodyLen >= 2) {
            state.leftBattery = Math.min(5, packet[bodyOffset] & 0xFF);
            state.rightBattery = Math.min(5, packet[bodyOffset + 1] & 0xFF);
            if (bodyLen >= 3) {
                int c = packet[bodyOffset + 2] & 0xFF;
                if (c <= 5) state.caseBattery = c;
            }
            return true;
        }

        // Command [0x01, 0x04]: Charging status update
        if (cmdHi == 0x01 && cmdLo == 0x04 && bodyLen >= 2) {
            state.leftCharging = (packet[bodyOffset] & 0xFF) != 0;
            state.rightCharging = (packet[bodyOffset + 1] & 0xFF) != 0;
            return true;
        }

        return false;
    }

    private static void parseSoundModesBlock(byte[] buf, int offset, DeviceState state) {
        if (offset + 5 > buf.length) return;
        int amb = buf[offset] & 0xFF;
        if (amb <= 2) {
            state.ambientMode = amb;
        }
        int manAdap = buf[offset + 1] & 0xFF;
        int man = (manAdap >> 4) & 0x0F;
        int adap = manAdap & 0x0F;
        if (man >= 1 && man <= 3) state.manualAncStrength = man;
        if (adap <= 2) state.adaptiveAncLevel = adap;

        int trans = buf[offset + 2] & 0xFF;
        if (trans <= 1) state.transparencyMode = trans;

        int nc = buf[offset + 3] & 0xFF;
        if (nc <= 1) state.ncMode = nc;

        int wind = buf[offset + 4] & 0xFF;
        state.windNoiseSuppression = (wind & 0x01) != 0;
        state.windNoiseDetected = (wind & 0x02) != 0;
    }

    public static String toHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        int max = Math.min(bytes.length, Math.min(length, 24));
        for (int i = 0; i < max; i++) {
            sb.append(String.format("%02X ", bytes[i] & 0xFF));
        }
        if (length > max) {
            sb.append("... (").append(length).append("B)");
        }
        return sb.toString().trim();
    }
}
