package uk.ac.warwick.cim.unheardCity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;


/**
 * Main activity doesn't really do much, but start the service and then finish.
 * In oreo to run a background service when the app is not running it must
 * startForegroundService(Intent)  in the activity
 * in service, make a notification low or higher. persistent.
 * and startForeground (int id, Notification notification )
 */

public class MainActivity extends AppCompatActivity {
    public static String id1 = "test_channel_01";

    private static final String TAG = "BUTTON";

    private FusedLocationProviderClient fusedLocationClient;

    protected LocationCallback locationCallback;
    
    private LocationRequest locationRequest;

    private boolean requestingLocationUpdates;

    private File signalFile;

    private File locationFile;

    private File bluetoothFile;

    private File wifiFile;

    private BroadcastReceiver receiver;

    private int Bluetooth = 0;

    private int BLE = 0;

    private int wifi = 0;

    private WifiManager wifiManager;

    private BroadcastReceiver wifiScanReceiver;

    private BroadcastReceiver wifiRangeReceiver;

    private WifiRttManager wifiRttManager;

    protected WifiScan wifiScan;

    protected MediaRecorder mediaRecorder = new MediaRecorder();

    private BluetoothScan bluetoothScan;

    private BluetoothLEScan bleScanner;

    public MainActivity() {
        requestingLocationUpdates = true;
    }


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            Log.i("LOCATION", "No permissions");
        } else {
            Log.i("LOCATION", "Location permissions");
        }

        String[] permissions = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //@todo: refactor me into one permissions check
        checkPermissions(Manifest.permission.ACCESS_FINE_LOCATION, "Location Permissions error");

        //Get permissions to write data
        checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, "Write permissions error");

        //assumption that the session will be the time that the app runs.
        //Create log file for both WiFi and Bluetooth connections
        long currentTime = System.currentTimeMillis();
        signalFile = this.createDataFile("bluetoothle_" + currentTime + ".txt");
        locationFile = this.createDataFile("locations_" + currentTime + ".txt");
        bluetoothFile = this.createDataFile("bluetooth_" + currentTime + ".txt");
        wifiFile = this.createDataFile("wifi_" + currentTime + ".txt");

        bleScanner = new BluetoothLEScan(signalFile);
        bluetoothScan = new BluetoothScan(this, bluetoothFile);

        // set up location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            Log.i("LOCATION", location.toString());
                            String data = locationDetails(location);
                            new FileConnection(locationFile).writeFile(data);
                        }else {
                            Log.i("LOCATION", "No Location");
                            new BluetoothLEScan(signalFile);
                        }
                    }

                });

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.i("LOCATION", "Result");
                if (locationResult.getLocations().isEmpty()) {
                    Log.i("LOCATION", "No results");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.i("LOCATION", location.toString());
                    String data = locationDetails(location);
                    new FileConnection(locationFile).writeFile(data);
                }

            }

        };

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startLocationUpdates();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("Location", "Final");
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        int REQUEST_CHECK_SETTINGS = 105;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        Log.e("Location", sendEx.toString());
                    }
                }
            }
        });

        saveNotes();

        //setUpRecordAudio();

    }

    /**
     * Function to use the Microphone for simple audio recording
     * that writes to an MP4 files.
     */
    private void setUpRecordAudio () {


        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            //@todo: add location as well.
            File audioFile = new File(this.getExternalFilesDir(null),
                    System.currentTimeMillis() + ".mp4");

            mediaRecorder.setOutputFile(audioFile);

            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

            //let's stick to one channel for now. Maybe stereo later?
            mediaRecorder.setAudioChannels(1);
            mediaRecorder.prepare();

        } catch (IOException ioe) {
            Log.i("AudioRecorder", ioe.toString());
        } catch (IllegalStateException ise) {
            Log.i("AudioRecorder", ise.toString());
        }

    }

    private void startRecordAudio (View view) {
        mediaRecorder.start();
    }

    private void stopRecordAudio (View view) {
        mediaRecorder.stop();
    }

    /**
     * Listener to save any made notes made in the UI.
     * The text will be stored with the time and location.
     */
    private void saveNotes() {
        EditText editText = (EditText) findViewById(R.id.notes_field);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    //@todo: save the message with the time and location
                    handled = true;
                    Log.i(TAG, "Note ");
                }
                return handled;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void checkPermissions(String accessFineLocation, String s) {
        //Get permissions to find location
        if (ContextCompat.checkSelfPermission(MainActivity.this, accessFineLocation)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("PERMISSIONS", s);
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    accessFineLocation)) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{accessFineLocation}, 1);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{accessFineLocation}, 1);
            }
        }
    }

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        try {
            createLocationRequest();

            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        } catch (Exception e) {
            Log.i("LOCATION_ERROR", e.toString());
        }
    }

    /**
     * Function to wrap location as a string.
     * @param location
     * @return
     */
    private String locationDetails (Location location) {
        String details = "";
        details = System.currentTimeMillis()
                + "," + location.getLatitude()
                + "," + location.getLongitude()
                + "," + location.getAltitude()
                + "," + location.getBearing()
                + "," + location.getSpeed()
                + "," + location.getVerticalAccuracyMeters()
                + "," + location.getAccuracy()
                + "\n";

        return details;
    }

    /**
     * Set up the Bluetooth scanning
     */
    private void setUpBluetoothScan () {

        //bluetoothScan = new BluetoothScan(this, bluetoothFile);
        bluetoothScan.start();

        Log.i(TAG, "Bluetooth ON");
        //if (BLE == 1) {
            stopBluetoothLEscan();
        /*}

        if (BLE == 1) {
            BLE = 0;
        }
        Bluetooth = 1;*/
    }

    public void bScan (View view) {
        if (Bluetooth == 1) {
            stopBluetoothScan();
        } else {
            setUpBluetoothScan();
        }
    }

    public void bleScan (View view) {
        if (BLE == 1) {
            stopBluetoothLEscan();
        } else {
            setUpBluetoothLEscan();
        }
    }

    public void wifiScan (View view) {
        if (wifi == 1) {
            stopWiFiScan();
        } else {
            startWiFiScan();
        }
    }
    /**
     * Function to stop the scan if we change protocols
     * Set the Bluetooth scan flag to 0.
     */
    private void stopBluetoothScan() {
        bluetoothScan.stop();
        Log.i(TAG, "Bluetooth OFF");
        //unregisterReceiver(receiver);
        Bluetooth = 0;
    }

    /**
     * Start the Bluetooth LE Scan
     */
    private void setUpBluetoothLEscan() {
        Log.i(TAG, "BluetoothLE ON");
        //stop bluetooth scan if running.
        /*if (Bluetooth == 1) {
            stopBluetoothScan();
        }*/
        //@todo: set this up as a runnable for ever 5 seconds
        //@todo: set up a UI button to set scan time and put in warning.
        BLE = 1;
        bleScanner.start();
    }

    /**
     * Function to stop the runnable and to reset the BLE flag
     */
    private void stopBluetoothLEscan () {
        Log.i(TAG, "BluetoothLE OFF");
        BLE = 0;
        bleScanner.stop();
    }

    private void startWiFiScan() {
        Log.i(TAG, "WiFi ON");
        wifi = 1;
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        try {
            wifiScan = new WifiScan(this, wifiManager, wifiFile);
            wifiScan.start();
        } catch (SecurityException se) {
            Log.i("WIFI", se.toString());
        }
            catch (Exception e) {
            Log.i("WIFI", e.toString());
        }

    }

    private void stopWiFiScan() {
        Log.i(TAG, "WiFi OFF");
        wifi = 0;
        wifiScan.stop();

    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void startWiFiRanging() {
        Log.i(TAG, "WiFi Ranging ON");

        wifiRttManager = (WifiRttManager) this.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)) {
            //RTT available but is it enabled? User may have changed its state.
            IntentFilter filter =
                        new IntentFilter(wifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED);

            wifiRangeReceiver = new BroadcastReceiver() {
                    //Suppress the Fine Location permission because we request earlier to run the app.
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (wifiRttManager.isAvailable()) {
                            List<ScanResult> results = wifiManager.getScanResults();
                            RangingRequest.Builder builder = new RangingRequest.Builder();
                            builder.addAccessPoints(results);
                            RangingRequest req = builder.build();
                            Executor executor = context.getMainExecutor();

                             wifiRttManager.startRanging(req, executor, new RangingResultCallback() {

                                 @Override
                                 public void onRangingFailure(int code) {
                                     Log.i("WiFiRTT", "Ranging failure " + code);
                                 }

                                 @Override
                                 public void onRangingResults(List<RangingResult> results) {
                                     for (RangingResult result: results) {
                                         //We have data, let's get some details
                                         result.getDistanceMm();
                                     }
                                 }
                             });
                         } else {
                            Log.i("WiFiRTT", "Ranging not available");
                         }
                    }
            };
            registerReceiver(wifiRangeReceiver, filter);
            }
    }

    private void stopWiFiRanging () {
        Log.i(TAG, "WiFi Ranging OFF");
        unregisterReceiver(wifiRangeReceiver);
    }

    /**
     * Function to handle the file name creation
     * @param fileName
     * @return
     */
    private File createDataFile(String fileName) {
        File fName = new File(this.getExternalFilesDir(null), fileName);

        if (!fName.exists()) {
            try {
                final boolean newFile = fName.createNewFile();
                if (!newFile) Log.i("FILE", fileName + " not created");
            } catch (IOException e) {
                Log.i("FILE",e.toString());
            }
        }

        return fName;
    }


}
