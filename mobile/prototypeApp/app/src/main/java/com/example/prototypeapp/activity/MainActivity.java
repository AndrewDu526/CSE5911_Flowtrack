package com.example.prototypeapp.activity;

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

import com.example.prototypeapp.R;
import com.example.prototypeapp.data.model.TrackBatch;
import com.example.prototypeapp.data.model.TrackPoint;
import com.example.prototypeapp.data.model.beacon.BeaconId;
import com.example.prototypeapp.data.model.beacon.BeaconRuntime;
import com.example.prototypeapp.data.model.beacon.StaticBeacon;
import com.example.prototypeapp.data.model.estimator.LocationEstimate;
import com.example.prototypeapp.data.parser.BeaconParser;
import com.example.prototypeapp.domain.distanceEstimator.DistanceEstimator;
import com.example.prototypeapp.domain.distanceEstimator.distanceEstimatorImpl.LogDistanceEstimator;
import com.example.prototypeapp.domain.locationEstimator.LocationEstimator;
import com.example.prototypeapp.domain.locationEstimator.locationEstimatorImpl.WeightedLeastSquareEstimator;
import com.example.prototypeapp.domain.locationFilter.KalmanAdaptiveFilter;
import com.example.prototypeapp.domain.rssiSmoother.RssiSmoother;
import com.example.prototypeapp.domain.rssiSmoother.rssiSmootherImpl.CombinedSmoother;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class MainActivity extends AppCompatActivity {

    int APPLE_ID = 0x004C;

    private static final String TAG = "BEACON";
    private Button btnStart, btnStop;

    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner bleScanner;
    private TextView txtStatus, txtCount;
    private boolean scanning = false;
    final Handler timer = new Handler(Looper.getMainLooper());
    final int UPDATE_INTERVAL_MS = 500;
    private List<TrackPoint> pointBuffer = new ArrayList<>();
    private static final int BATCH_SIZE = 100;
    private int batchCounter = 0;
    int pointCount = 0;

    Map<String, StaticBeacon> beaconRepository = new HashMap<>(); // sign up stable beacons
    Map<String, BeaconRuntime> beaconRuntimeMap = new HashMap<>();

    RssiSmoother rssiSmoother = new CombinedSmoother();
    DistanceEstimator distanceEstimator = new LogDistanceEstimator();
    LocationEstimator locationEstimator = new WeightedLeastSquareEstimator();
    KalmanAdaptiveFilter kalmanAdaptiveFilter = new KalmanAdaptiveFilter();



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

        StaticBeacon beacon1 = new StaticBeacon(new BeaconId("iBeacon",uuid,10001,1), "test_beacon_1_bedroom", -160*0.0254,280*0.0254);
        StaticBeacon beacon2 = new StaticBeacon(new BeaconId("iBeacon",uuid,10001,2), "test_beacon_1_bedroom", 155*0.0254,258*0.0254);
        StaticBeacon beacon3 = new StaticBeacon(new BeaconId("iBeacon",uuid,10001,3), "test_beacon_1_bedroom", 300*0.0254,280*0.0254);
        StaticBeacon beacon4 = new StaticBeacon(new BeaconId("iBeacon",uuid,10001,4), "test_beacon_1_bedroom", 60*0.0254,110*0.0254);
        StaticBeacon beacon5 = new StaticBeacon(new BeaconId("iBeacon",uuid,10001,5), "test_beacon_1_bedroom", 0,0);
        StaticBeacon beacon6 = new StaticBeacon(new BeaconId("iBeacon",uuid,10001,6), "test_beacon_1_bedroom", 240*0.0254,0);

        beaconRepository.put(beacon1.toString(), beacon1);
        beaconRepository.put(beacon2.toString(), beacon2);
        beaconRepository.put(beacon3.toString(), beacon3);
        beaconRepository.put(beacon4.toString(), beacon4);
        beaconRepository.put(beacon5.toString(), beacon5);
        beaconRepository.put(beacon6.toString(), beacon6);

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

        PositioningProcess();
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

        if (beaconRepository.containsKey(beaconId)) {

            StaticBeacon beacon = beaconRepository.get(beaconId);
            if(beacon!=null){

                BeaconRuntime beaconRuntime = new BeaconRuntime(beacon, 0, 0, new CombinedSmoother());
                beaconRuntime.smoother.addSingleRssi(rssi);
                double smoothedRssi = beaconRuntime.smoother.getSmoothedRssi();
                double distance = distanceEstimator.estimateDistance(smoothedRssi);

                beaconRuntime.rssi = smoothedRssi;
                beaconRuntime.distance = distance;

                beaconRuntimeMap.put(beaconId,beaconRuntime);
            }else{
                Log.w("BLE", "Unregistered beacon found: " + beaconId);
            }
        }
    }

    public void PositioningProcess() {
        timer.postDelayed(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> effectiveBeacons = new ArrayList<>();
                //filter beacons:
                for (String id : beaconRuntimeMap.keySet()) {
                    BeaconRuntime b = beaconRuntimeMap.get(id);

                    if (b.rssi < -90) continue;
                    // TODO: More Filters: time, stability...
                    effectiveBeacons.add(id);
                }
                effectiveBeacons.sort((b1, b2) -> Double.compare(beaconRuntimeMap.get(b2).rssi, beaconRuntimeMap.get(b1).rssi));

                final int N_MAX = 10, N_MIN = 4;

                if (effectiveBeacons.size() > N_MAX) {
                    effectiveBeacons = new ArrayList<>(effectiveBeacons.subList(0, N_MAX));
                }

                if (effectiveBeacons.size() >= N_MIN) {

                    ArrayList<BeaconRuntime> temp = new ArrayList<>();
                    for (String id : effectiveBeacons) {
                        BeaconRuntime b = beaconRuntimeMap.get(id);
                        if (b != null) temp.add(b);
                    }


                    LocationEstimate locationEstimate = locationEstimator.estimate(temp);
                    TrackPoint p = kalmanAdaptiveFilter.step(locationEstimate.x, locationEstimate.y, locationEstimate.timeStamp,locationEstimate.effectiveBeacons,locationEstimate.rms);

                    pointBuffer.add(p);
                    pointCount++;
                    txtCount.setText("Points: " + pointCount);

                    if (pointBuffer.size() >= BATCH_SIZE) {

                        TrackBatch batch = new TrackBatch("Room 2320", "3rd floor",
                                "Altitude Columbus", 50, 0,1,
                                pointBuffer, "test_local_map", "BLT+Kalman", "10001",
                                "OnePlusThreePJE110-15","batch_no.1_test");

                        saveBatchToFile(batch);
                        pointBuffer.clear();
                    }

                    timer.postDelayed(this, UPDATE_INTERVAL_MS); // recursive call
                }else {
                    // TODO: Back up functions: FLP, Centroid...
                }
            }
        }, UPDATE_INTERVAL_MS);}

    void saveBatchToFile(TrackBatch batch) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(batch);

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            File out = new File(dir, batch.batchId + ".json");
            if (!dir.exists()) dir.mkdirs();

            try (FileWriter writer = new FileWriter(out)) {
                writer.write(json);
                txtStatus.setText("batch saved: "+ batch.batchId);
            } catch (IOException e) {
                Log.e("TRACK", "Failed to write batch file", e);
            }
    }
}

