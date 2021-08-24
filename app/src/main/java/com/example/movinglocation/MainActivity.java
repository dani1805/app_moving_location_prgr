package com.example.movinglocation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.Activity;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private FusedLocationProviderClient locationClient;
    private LocationRequest locationRequest;
    private TextView tvLocation;
    private static final int REQUEST_PERMISSION = 2;
    private static final int CHECK_SETTINGS = 1;
    PolylineOptions options = new PolylineOptions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocation = findViewById(R.id.tvLocation);
        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.myMap);
        fragment.getMapAsync(this);

        locationClient = LocationServices.getFusedLocationProviderClient(this);
        // Comprobar que el dispositivo tenga permisos o no
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            configLocation();// Para obtener ubicacion actual no se necesita implementar los permisos aun
        } else { // Solicitar permisos
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        UiSettings uiSettings = googleMap.getUiSettings();

        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);

        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                configLocation();
            } else {
                Toast.makeText(this, "No se han aceptado los permisos de ubicación", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void showMessage(Location location) {

        float accuracy = location.getAccuracy();
        float bearing = location.getBearing();
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String provider = location.getProvider();
        double altitude = location.getAltitude();

        String message = "Latitud: " + latitude + ", Longitud: " + longitude + "\nAltitud: " + altitude + "\nAccuracy: " + accuracy +
                "\nProveedor: " + provider + "\nOrientación: " + bearing;

        tvLocation.setText(message);
        // Imprimir en un textView la precision, longitud,latitud... de la ubicacion del cliente

    }

    public void drawMarker(Location location) {
        if (map != null) {

            float accuracy = location.getAccuracy();
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude) , 25.0f) );

            map.clear();
            map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)));

            options.add(new LatLng(latitude, longitude));
            map.addPolyline(options);
        }
    }

    public void configLocation() {

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        // Listener de exito
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);

                locationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                locationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        });
        // Listener de fallo
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    try {
                        resolvable.startResolutionForResult(MainActivity.this, CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
//            super.onLocationResult(locationResult);
            if (locationResult != null) {
                for (Location location : locationResult.getLocations()) {
                    showMessage(location);
                    drawMarker(location);
                    //imprimirUbicacion(location);
                }
            }
        }

    };

    // Pausar las actualizaciones de las ubicaciones
    @Override
    protected void onPause() {
        super.onPause();
        locationClient.removeLocationUpdates(locationCallback);
    }

    // Despertar la actualizacion de las ubicaciones
    @Override
    protected void onResume() {
        super.onResume();
        if (locationClient != null) {
            configLocation();
        }
    }
    }





