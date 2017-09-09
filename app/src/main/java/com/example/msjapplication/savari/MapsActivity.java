package com.example.msjapplication.savari;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    LocationManager locationManager;
    LocationListener locationListener;
    Button button;
    Boolean requestActive = false;
    Boolean driverActive = false;
    private GoogleMap mMap;
    android.os.Handler handler = new android.os.Handler();
    TextView info;

    public void checkForUpdates(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
        query.whereEqualTo("username" , ParseUser.getCurrentUser().getUsername());
        query.whereExists("driverUsername");

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null && objects.size() > 0) {

                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.whereEqualTo("username" , objects.get(0).getString("driverUsername"));
                    query.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> objects, ParseException e) {
                            if (e == null && objects.size() > 0) {
                                ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("location");
                                if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                                    Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                                    if (last != null){

                                        driverActive = true;

                                        ParseGeoPoint userLocation = new ParseGeoPoint(last.getLatitude() , last.getLongitude());

                                        Double distance = driverLocation.distanceInKilometersTo(userLocation);

                                        if (distance < 0.1){
                                            info.setText("SAVARI HAS ARRIVED");

                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    button.setVisibility(View.VISIBLE);
                                                    button.setText("BOOK A SAVARI");
                                                    requestActive = false;
                                                    driverActive = false;
                                                    info.setText("");


                                                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
                                                    query.whereEqualTo("username" , ParseUser.getCurrentUser().getUsername());
                                                    query.findInBackground(new FindCallback<ParseObject>() {
                                                        @Override
                                                        public void done(List<ParseObject> objects, ParseException e) {
                                                            if (e == null){
                                                                for (ParseObject object: objects){
                                                                    object.deleteInBackground();
                                                                }
                                                            }
                                                        }
                                                    });

                                                }
                                            } , 5000);

                                        }else {

                                            Double km = (double) Math.round(distance * 10) / 10;

                                            info.setText("Savari is " + km.toString() + " km away");

                                            LatLng driverLocationLatLng = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());
                                            LatLng requestLocationLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

                                            ArrayList<Marker> markers = new ArrayList<>();

                                            mMap.clear();

                                            markers.add(mMap.addMarker(new MarkerOptions()
                                                    .position(driverLocationLatLng)
                                                    .title("DRIVER LOCATION")
                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));

                                            markers.add(mMap.addMarker(new MarkerOptions()
                                                    .position(requestLocationLatLng)
                                                    .title("YOUR LOCATION")));

                                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                            for (Marker marker : markers) {
                                                builder.include(marker.getPosition());
                                            }
                                            LatLngBounds bounds = builder.build();

                                            int padding = 100; // offset from edges of the map in pixels
                                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                                            mMap.animateCamera(cu);

                                            button.setVisibility(View.INVISIBLE);

                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    checkForUpdates();
                                                }
                                            } , 2000);
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    public void logout(View view){
        ParseUser.logOut();
        Intent intent = new Intent(getApplicationContext() , MainActivity.class);
        startActivity(intent);
    }

    public void callSavari(View view) {
        if (requestActive) {
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {

                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if (objects.size() > 0) {
                            for (ParseObject object : objects) {
                                object.deleteInBackground();
                            }
                            requestActive = false;
                            button.setText("CALL SAVARI");
                            Toast.makeText(MapsActivity.this, "SAVARI CANCELED", Toast.LENGTH_SHORT).show();

                        }
                    }

                }
            });
        } else {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (last != null) {
                    ParseObject request = new ParseObject(("Request"));
                    request.put("username", ParseUser.getCurrentUser().getUsername());
                    ParseGeoPoint parseGeo = new ParseGeoPoint(last.getLatitude(), last.getLongitude());
                    request.put("location", parseGeo);
                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                requestActive = true;
                                button.setText("CANCEL RIDE");
                                Toast.makeText(MapsActivity.this, "CALLING SAVARI " , Toast.LENGTH_SHORT).show();

                                checkForUpdates();

                            }
                        }
                    });
                } else {
                    Toast.makeText(this, "Last location not found", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        button = (Button) findViewById(R.id.button);
        info = (TextView)findViewById(R.id.infoTextView);

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if (objects.size() > 0) {
                        requestActive = true;
                        button.setText("CANCEL RIDE");
                        Toast.makeText(MapsActivity.this, "CALLING SAVARI ", Toast.LENGTH_SHORT).show();

                        checkForUpdates();
                    }
                }
            }
        });
    }

    public void updateMap(Location location) {

        if (driverActive != false) {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            mMap.addMarker(new MarkerOptions().position(userLocation).title("YOUR LOCATION"));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateMap(last);
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateMap(location);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (last != null) {
                    updateMap(last);
                }
            }
        }

    }
}
