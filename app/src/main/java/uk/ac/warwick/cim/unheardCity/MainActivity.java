package uk.ac.warwick.cim.unheardCity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.IOException;


/**
 * Main activity sets up the files for this session, and begins the location
 * collection.
 *
 * The interface allows the user to set up the scan function types.
 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BUTTON";

    private FusedLocationProviderClient fusedLocationClient;

    protected LocationCallback locationCallback;
    
    private LocationRequest locationRequest;

    private final boolean requestingLocationUpdates;

    private File signalFile;

    private File locationFile;

    private File bluetoothFile;

    private File wifiFile;

    private BroadcastReceiver receiver;

    private int Bluetooth = 0;

    private int BLE = 0;

    private int wifi = 0;

    private WifiManager wifiManager;

    protected WifiScan wifiScan;

    protected MediaRecorder mediaRecorder;

    private BluetoothScan bluetoothScan;

    private BluetoothLEScan bleScanner;

    private  FormatData formatData = new FormatData();

    private BaseStationScan baseStationScan;

    public MainActivity() {
        requestingLocationUpdates = true;
    }


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            };
            checkPermissions(permissions);
        } else {
            String[] permissions = {Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            checkPermissions(permissions);
        }



        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            Log.i("LOCATION", "No permissions");
        } else {
            Log.i("LOCATION", "Location permissions");
        }

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
                .addOnSuccessListener(this, new OnSuccessListener<>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            Log.i("LOCATION", location.toString());
                            //String data = locationDetails(location);
                            String data = formatData.formatLocation(location);
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
                }
                for (Location location : locationResult.getLocations()) {
                    Log.i("LOCATION", location.toString());
                    String data = formatData.formatLocation(location);
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
    }

    /**
     * Function to use the Microphone for simple audio recording
     * that writes to an MP4 files.
     */
    private void setUpRecordAudio () {

         mediaRecorder = new MediaRecorder();

        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

            mediaRecorder.setAudioSamplingRate(384000);

            //@todo: add location as well.
            File audioFile = new File(this.getExternalFilesDir(null),
                    System.currentTimeMillis() + ".mp4");

            mediaRecorder.setOutputFile(audioFile);

            //let's stick to one channel for now. Maybe stereo later?
            mediaRecorder.setAudioChannels(2);
            mediaRecorder.prepare();

        } catch (IOException ioe) {
            Log.i("AudioRecorder IO Exception", ioe.toString());
        } catch (IllegalStateException ise) {
            Log.i("AudioRecorder Illegal State", ise.toString());
        }

    }

    public void startRecordAudio (View view) {
        setUpRecordAudio();
        mediaRecorder.start();

        mediaRecorder.getMaxAmplitude();

    }

    public void stopRecordAudio (View view) {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
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

    private void checkPermissions(String[] permissions) {

        for (String permission: permissions) {
            //Get permissions to find location
            if (ContextCompat.checkSelfPermission(MainActivity.this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i("PERMISSIONS", "Granted " + permission);
                try {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            permission)) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{permission}, 1);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{permission}, 1);
                    }
                } catch (Exception e) {
                    Log.i("PERMISSIONS", e.toString());
                }
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
     * Set up the Bluetooth scanning
     */
    public void setUpBluetoothScan () {

        bluetoothScan.start();

        Log.i(TAG, "Bluetooth ON");
        if (BLE == 1) {
            stopBluetoothLEscan();
        }

        if (BLE == 1) {
            BLE = 0;
        }
        Bluetooth = 1;
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
        if (Bluetooth == 1) {
            stopBluetoothScan();
        }
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
            Log.i("WIFI Security", se.toString());
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

    /**
     * Function to handle the file name creation
     * @param fileName filename to write
     * @return file
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

    /**
     * Start the base station scan
     * @param view
     */
    public void baseScanStart (View view) {
        baseStationScan.stop();
    }

    /**
     * Stop the base station scan
     * @param view
     */
    public void baseScanStop (View view) {
        baseStationScan.stop();
    }

}
