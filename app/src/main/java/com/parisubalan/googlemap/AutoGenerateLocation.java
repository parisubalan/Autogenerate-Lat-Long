package com.parisubalan.googlemap;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AutoGenerateLocation extends AppCompatActivity implements View.OnClickListener {

    private TextView latLang;
    private double latitude, longitude;
    private FusedLocationProviderClient fpc;
    private LocationManager locationManager;
    private LocationRequest locationRequest;
    private LocationSettingsRequest settingsRequest;
    private SettingsClient settingsClient;
    private LocationCallback locationCallback;
    private StringBuilder stringBuilder;
    private List<Address> address;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button currentLocation = findViewById(R.id.captureLocation);
        latLang = findViewById(R.id.location);
        currentLocation.setOnClickListener(this);

// To initialize a components
        fpc = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        settingsClient = LocationServices.getSettingsClient(this);

        locationRequest = LocationRequest.create();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        settingsRequest = builder.build();
        builder.setAlwaysShow(true);

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(3 * 1000);
        locationRequest.setFastestInterval(2 * 1000); // 2 seconds

        locationCallback = new LocationCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        try {
                            address = geocoder.getFromLocation(latitude,longitude,1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String subLocality = address.get(0).getSubLocality();
                        stringBuilder.append(latitude).append(" - ").append(longitude).append(" - ").append(subLocality);
                        stringBuilder.append("\n\n");
                        latLang.setText(stringBuilder.toString());
                        if (fpc !=null) {
                            fpc.removeLocationUpdates(locationCallback);
                        }

                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Location Not Found", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
    }
    @SuppressLint("MissingPermission")
    public void getLocation() {
        stringBuilder = new StringBuilder();
        fpc.requestLocationUpdates(locationRequest, locationCallback, null);

    }

    public void getLocationPermission() {
        // Checking a permission grant or not
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {

            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    10);
        } else {
            getGps();
        }
    }

    private void getGps() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLocation();
        } else {
            settingsClient.checkLocationSettings(settingsRequest)
                    .addOnSuccessListener(this, locationSettingsResponse -> getLocation())
                    .addOnFailureListener(this, e -> {
                        int statusCode = ((ApiException) e).getStatusCode();
                        if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                            try {
                                ResolvableApiException rae = (ResolvableApiException) e;
                                rae.startResolutionForResult(this, 10);
                            } catch (IntentSender.SendIntentException sie) {
                                Log.i(TAG, "PendingIntent unable to execute request.");
                            }
                        }
                    });
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getGps();
            } else {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)&&
                        ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION))
                {
                    Toast.makeText(this, "Please Turn On Location", Toast.LENGTH_SHORT).show();
                }
                else {
                    settingPermission();
                }
            }
        }
    }

    public void settingPermission() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("please Enable Location Permission").setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> startActivity(new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)))
                .setNegativeButton("No", (dialog, which) -> dialog.cancel());
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onClick(View v) {
        getLocationPermission();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode  == RESULT_OK)
        {
            if (requestCode == 10)
            {
                getLocationPermission();
            }
        }
    }
}