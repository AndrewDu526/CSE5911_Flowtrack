package parser;

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.util.SparseArray;

import java.util.Arrays;

import model.BeaconId;

public class BeaconParser {
    public static BeaconId parse(ScanResult r) {
        ScanRecord record = r.getScanRecord();
        if (record == null) return null;

        SparseArray<byte[]> msd = record.getManufacturerSpecificData();
        if (msd != null) {
            byte[] p = msd.get(0x004C); // apple
            if (p != null && p.length >= 23 && (p[0]==0x02) && (p[1]==0x15)) {
                String uuid = bytesToUuid(Arrays.copyOfRange(p,2,18)).toString().toLowerCase();
                int major = u16(p,18), minor = u16(p,20);
                return new BeaconId("ibeacon", uuid, major, minor);
            }
        }
        return null;
    }

    private static int u16(byte[] b, int offset) {
        return ((b[offset] & 0xFF) << 8) | (b[offset + 1] & 0xFF);
    }

    private static java.util.UUID bytesToUuid(byte[] b) {
        long msb = 0, lsb = 0;
        for (int i = 0; i < 8; i++) msb = (msb << 8) | (b[i] & 0xff);
        for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (b[i] & 0xff);
        return new java.util.UUID(msb, lsb);
    }
}
