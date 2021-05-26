package uk.ac.warwick.cim.signalCity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.fonts.FontFamily;

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
 * Main activity doesn't really do much, but start the service and then finish.
 * In oreo to run a background service when the app is not running it must
 * startForegroundService(Intent)  in the activity
 * in service, make a notification low or higher. persistent.
 * and startForground (int id, Notification notification )
 */

public class MainActivity extends AppCompatActivity {
    public static String id1 = "test_channel_01";

    private FusedLocationProviderClient fusedLocationClient;

    protected LocationCallback locationCallback;
    
    private LocationRequest locationRequest;

    public Location  mCurrentLocation;

    private boolean requestingLocationUpdates;

    private File signalFile;

    private File locationFile;

    private File wifiFile;

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

        //Get permissions to find location
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("PERMISSIONS", "Location Permissions error");
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }

        //Get permissions to write data
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("PERMISSIONS", "Write permissions error");
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        //@todo: Link both of these to a date
        //Create log file for both WiFi and Bluetooth connections
        signalFile = new File(this.getExternalFilesDir(null), "bluetooth.txt");

        if (!signalFile.exists()) {
            try {
                signalFile.createNewFile();
            } catch (IOException e) {
                Log.i("SIGNAL",e.toString());
            }
        }
        //Create log file for both WiFi and Bluetooth connections
        locationFile = new File(this.getExternalFilesDir(null), "locations.txt");

        if (!locationFile.exists()) {
            try {
                locationFile.createNewFile();
            } catch (IOException e) {
                Log.i("SIGNAL",e.toString());
            }
        }

        wifiFile = new File(this.getExternalFilesDir(null), "wifi.txt");

        if (!wifiFile.exists()) {
            try {
                wifiFile.createNewFile();
            } catch (IOException e) {
                Log.i("SIGNAL",e.toString());
            }
        }

        //createLocationRequest();
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
                            String data = System.currentTimeMillis()
                                    + "," + location.getLatitude()
                                    + "," + location.getLongitude()
                                    + "," + location.getAltitude()
                                    + "," + location.getBearing()
                                    + "," + location.getSpeed()
                                    + "," + location.getVerticalAccuracyMeters()
                                    + "," + location.getAccuracy()
                                    + "\n";
                            new FileConnection(locationFile).writeFile(data);
                        }else {
                            Log.i("LOCATION", "No Location");
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
                if (locationResult == null) {
                    Log.i("LOCATION", "No results");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.i("LOCATION", location.toString());
                    String data = System.currentTimeMillis()
                            + "," + location.getLatitude()
                            + "," + location.getLongitude()
                            + "," + location.getAltitude()
                            + "," + location.getBearing()
                            + "," + location.getSpeed()
                            + "," + location.getVerticalAccuracyMeters()
                            + "," + location.getAccuracy()
                            + "\n";
                    new FileConnection(locationFile).writeFile(data);
                }
                new BluetoothLEDetails(signalFile);

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
                Log.i("Location", "Fial");
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
                        // Ignore the error.
                    }
                }
            }
        });


    }

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (requestingLocationUpdates) {
            startLocationUpdates();
            new WifiDetails(this, wifiFile);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        try {

            /*if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i("LOCATION_PERMISSIONS", "Permissions error");
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)){
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }
            }*/

            createLocationRequest();
            Log.i("Location", "in loop");
            Log.i("Location", locationCallback.toString());
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        } catch (Exception e) {
            Log.i("LOCATION_ERROR", e.toString());
        }
    }


}
