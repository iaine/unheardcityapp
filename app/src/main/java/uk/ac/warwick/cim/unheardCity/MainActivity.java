package uk.ac.warwick.cim.unheardCity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;

import androidx.annotation.NonNull;
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
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
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
 * Main activity doesn't really do much, but start the service and then finish.
 * In oreo to run a background service when the app is not running it must
 * startForegroundService(Intent)  in the activity
 * in service, make a notification low or higher. persistent.
 * and startForeground (int id, Notification notification )
 */

public class MainActivity extends AppCompatActivity {
    public static String id1 = "test_channel_01";

    private FusedLocationProviderClient fusedLocationClient;

    protected LocationCallback locationCallback;
    
    private LocationRequest locationRequest;

    private boolean requestingLocationUpdates;

    private File signalFile;

    private File locationFile;

    private File bluetoothFile;

    private BroadcastReceiver receiver;

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
        checkPermissions(Manifest.permission.ACCESS_FINE_LOCATION, "Location Permissions error");

        //Get permissions to write data
        checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, "Write permissions error");

        //@todo: Link both of these to a date
        //Create log file for both WiFi and Bluetooth connections
        Long currentTime = System.currentTimeMillis();
        signalFile = this.createDataFile("bluetoothle_" + currentTime + ".txt");
        locationFile = this.createDataFile("locations_" + currentTime + ".txt");
        bluetoothFile = this.createDataFile("bluetooth_" + currentTime + ".txt");

        // set up location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if(fusedLocationClient == null) {
            Log.i("Location", "No provider");
        }

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
                            new BluetoothLEDetails(signalFile);
                        }
                    }

                });


        /*createchannel();  //needed for the persistent notification created in service.

        //IntentService start with 5 random number toasts
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent number5 = new Intent(getBaseContext(), MyForeGroundService.class);
                number5.putExtra("times", 50);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(number5);
                } else {
                    //lower then Oreo, just start the service.
                    startService(number5);
                }
                finish();  //make sure this activity has exited. f
            }
        });*/
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

                setUpBluetoothLEscan(signalFile);
                //new BluetoothLEDetails(signalFile);

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

        String details = System.currentTimeMillis()
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
        receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    System.out.println("Found device " + deviceName + " with addy " + deviceHardwareAddress);
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    /**
     * Function to stop the scan if we change protocols
     */
    private void stopBluetoothScan() {
        unregisterReceiver(receiver);
    }

    /**
     * Start the Bluetooth LE Scan
     * @param file
     */
    private void setUpBluetoothLEscan(File file) {
        //@todo: set this up as a runnable for ever 5 seconds
        //@todo: set up a UI button to set scan time and put in warning.
        new BluetoothLEDetails(signalFile);
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
