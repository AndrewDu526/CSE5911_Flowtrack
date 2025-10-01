package com.example.testappblt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import model.Beacon;
import model.BeaconId;
import model.TrackBatch;
import parser.BeaconParser;
import model.TrackPoint;

public class MainActivity extends AppCompatActivity {

    int APPLE_ID = 0x004C;
    Map<String, Beacon> myBeacons = new HashMap<>(); // sign up stable beacons


    private static final String TAG = "BEACON";
    private Button btnStart, btnStop;

    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner bleScanner;
    private TextView txtStatus, txtCount;
    private boolean scanning = false;
    final Handler timer = new Handler(Looper.getMainLooper());
    final int UPDATE_INTERVAL_MS = 2000; // per 5s cal current location
    private List<TrackPoint> pointBuffer = new ArrayList<>();
    private static final int BATCH_SIZE = 50;
    private int batchCounter = 0;
    int pointCount = 0;

    // 权限
    private final String[] permsApi31Plus = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private final String[] permsApi30Minus = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private ActivityResultLauncher<Intent> enableBtLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialBeacons();

        btnStart = findViewById(R.id.btnStart);
        btnStop  = findViewById(R.id.btnStop);
        txtStatus = findViewById(R.id.txtStatus);
        txtCount = findViewById(R.id.txtCount);

        // Manager is used to get Adapter, check and manage blt connections
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        // btAdapter == null means mobile not support blt
        // Adapter is used to check blt supportability, open blt, and get scanner...
        btAdapter = (bm != null) ? bm.getAdapter() : null;

        // launcher set up
        enableBtLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                r -> { // r: result of user choice in pop window
                    if (r.getResultCode() == RESULT_OK) {
                        tryStart();
                    } else {
                        Toast.makeText(this, "Blue tooth does not open", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                r -> {

                    for (Boolean granted : r.values()) {
                        if (granted == null || !granted) {
                            Toast.makeText(this, "Lack of permissions", Toast.LENGTH_LONG).show();
                            break;
                        }
                    }
                    tryStart();
                }
        );

        btnStart.setOnClickListener(v -> tryStart());
        btnStop.setOnClickListener(v -> stopScan());
    }

    private void initialBeacons() {
        String uuid = "426c7565-4368-6172-6d42-6561636f6e73";
        double tx = -59;
        double n = 2.0;

        BeaconId id1 = new BeaconId("ibeacon", uuid, 10001, 1);
        BeaconId id2 = new BeaconId("ibeacon", uuid, 10001, 2);
        BeaconId id3 = new BeaconId("ibeacon", uuid, 10001, 3);
        BeaconId id4 = new BeaconId("ibeacon", uuid, 10001, 4);

        myBeacons.put(id1.toString(), new Beacon(id1, 0.0, 0.0, tx, n));
        myBeacons.put(id2.toString(), new Beacon(id2, 0.0, 700, tx, n));
        myBeacons.put(id3.toString(), new Beacon(id3, 1200, 0, tx, n));
        myBeacons.put(id4.toString(), new Beacon(id4, 900, 700, tx, n));
    }

    private void tryStart() {
        if (btAdapter == null || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) return;

        // once user click start button, call tryStart()
        // if btAdapter is not enable, pop up permission window
        // system will listen to callback function: set up in enableBtLauncher

        if (!btAdapter.isEnabled()) {
            enableBtLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return; // after pop up the window, continue execute the rest code; Asy once user click
            // pop window, the selected result is caught by system then execute callback function
        }
        if (!hasAllRequiredPermissions()) {
            permissionLauncher.launch(permsApi31Plus);
            return;
        }
        txtStatus.setText("Scanning tried");
        startScan();
    }

    private boolean hasAllRequiredPermissions() {
        return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }


    @SuppressLint("MissingPermission")
    private void startScan() {
        if (scanning) return;
        if (bleScanner == null) bleScanner = btAdapter.getBluetoothLeScanner();
        if (bleScanner == null) return;

        // empty filter now, try listen signals as much as possible
        ScanFilter filter = new ScanFilter.Builder()
                .setManufacturerData(APPLE_ID, new byte[]{0x02, 0x15}) // Apple iBeacon
                .build();

        List<ScanFilter> filters = Collections.singletonList(filter); // filter setting on hardware level, like MAC address, UUID, service data...

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // LOW_LATENCY: high frequency, high power cost (LOW_POWER, BALANCE)
                .build();

        // start scanning: once system update/find a b
        bleScanner.startScan(filters, settings, scanCb);
        scanning = true;

        continuouslyCalculateLocation();
        txtStatus.setText("Scanning started");

    }

    @SuppressLint("MissingPermission")
    private void stopScan() {
        if (!scanning || bleScanner == null) return;
        timer.removeCallbacksAndMessages(null);
        bleScanner.stopScan(scanCb);
        scanning = false;
        Log.i(TAG, "Scan stopped");
        txtStatus.setText("Scanning stopped");
    }


    private final ScanCallback scanCb = new ScanCallback() {
        @Override public void onScanResult(int callbackType, @NonNull ScanResult r) { scanProcess(r); }
        @Override public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan failed: " + errorCode);
        }
    };

    private void scanProcess(ScanResult r) {

        if (r == null || r.getScanRecord() == null) return;

        BeaconId id = BeaconParser.parse(r);
        if (id == null) return;

        String beaconId = id.toString();
        int rssi = r.getRssi();

        if (myBeacons.containsKey(beaconId)) {
            Beacon beacon = myBeacons.get(beaconId);
            if(beacon!=null){
                // RSSI smoothing
                beacon.setRssi(rssi);
                // Get Distance based on RSSI
                beacon.setDistance();
            }else{
                Log.w("BLE", "Unregistered beacon found: " + beaconId);
            }
        }
    }

    public void continuouslyCalculateLocation() {
        timer.postDelayed(new Runnable() {
            @Override
            public void run() {
                ArrayList<Beacon> beacons = new ArrayList<>(myBeacons.values());
                TrackPoint p = getTrackPoint(beacons);

                if (p != null) {
                    pointBuffer.add(p);

                    pointCount++;
                    txtCount.setText("Points: " + pointCount);

                    if (pointBuffer.size() >= BATCH_SIZE) {
                        TrackBatch batch = new TrackBatch();
                        batch.batchName = "batch_" + (++batchCounter);
                        batch.locations = new ArrayList<>(pointBuffer);
                        saveBatchToFile(batch);
                        pointBuffer.clear();
                    }
                }

                timer.postDelayed(this, UPDATE_INTERVAL_MS); // recursive call
            }
        }, UPDATE_INTERVAL_MS);
    }
    private void saveBatchToFile(TrackBatch batch) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(batch);

        // File dir = new File(getFilesDir(), "track_batches");
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);


        File out = new File(dir, batch.batchId + ".json");
        if (!dir.exists()) dir.mkdirs();

        File outFile = new File(dir, batch.batchName + ".json");
        try (FileWriter writer = new FileWriter(outFile)) {
            writer.write(json);
            txtStatus.setText("batch saved: "+ batch.batchName);
            Log.i("TRACK", "Saved " + batch.batchName + " to file");
        } catch (IOException e) {
            Log.e("TRACK", "Failed to write batch file", e);
        }
    }


    public static TrackPoint getTrackPoint(ArrayList<Beacon> beacons) {
        double sumWx = 0, sumWy = 0, sumW = 0;

        for (Beacon b : beacons) {
            double d = b.getDistance();
            if (d <= 0.01) continue; // ignore too small distance

            double w = 1.0 / (d * d); // weight = 1/d²
            sumWx += w * b.getX();
            sumWy += w * b.getY();
            sumW  += w;
        }

        if (sumW == 0) return null;

        double x = sumWx / sumW;
        double y = sumWy / sumW;


        TrackPoint p = new TrackPoint();
        p.tWall = System.currentTimeMillis();
        p.tElapsedNs = SystemClock.elapsedRealtimeNanos();
        p.lat = x;
        p.lon = y;
        p.provider = "BLT";
        p.source = "BEACON";

        return p;
    }

}
