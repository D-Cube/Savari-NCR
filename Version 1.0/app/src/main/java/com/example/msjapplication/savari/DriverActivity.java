package com.example.msjapplication.savari;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.List;

public class DriverActivity extends AppCompatActivity{
    ListView listView;
    ArrayList<String> request = new ArrayList<String>();
    ArrayList<Double> reqLat = new ArrayList<Double>();
    ArrayList<Double> reqLong = new ArrayList<Double>();
    ArrayList<String> usernames = new ArrayList<String>();

    ArrayAdapter arrayAdapter ;
    LocationManager locationManager;
    LocationListener locationListener;

    public void updateListView(Location location){
        if (location != null) {

            ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
            final ParseGeoPoint geoPointLocation = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

            query.whereNear("location" ,geoPointLocation);
            query.whereDoesNotExist("driverUsername");
            query.setLimit(5);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e == null){
                        request.clear();
                        reqLat.clear();
                        reqLong.clear();
                        if (objects.size()>0){
                            for(ParseObject object : objects){
                                ParseGeoPoint reqLocation = (ParseGeoPoint) object.get("location");
                                if (reqLocation !=null) {

                                    Double distance = geoPointLocation.distanceInKilometersTo(reqLocation);
                                    Double km = (double) Math.round(distance * 10) / 10;
                                    request.add(km.toString() + " km away");

                                    reqLat.add(reqLocation.getLatitude());
                                    reqLong.add(reqLocation.getLongitude());
                                    usernames.add(object.getString("username"));
                                }
                            }
                        }else{
                            request.add("No Savari found");
                        }
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            });
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
                    updateListView(last);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        listView = (ListView)findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, request);
        request.clear();
        request.add("Getting nearby Savari");
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (ContextCompat.checkSelfPermission(DriverActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(DriverActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                } else {

                        Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (reqLat.size()>i && reqLong.size() > i && last != null && usernames != null) {

                            Intent intent = new Intent(getApplicationContext() , Drivers_Map.class);
                            intent.putExtra("requestLatitude" , reqLat.get(i));
                            intent.putExtra("requestLongitude" , reqLong.get(i));
                            intent.putExtra("driverLatitude" , last.getLatitude());
                            intent.putExtra("driverLongitude" , last.getLongitude());
                            intent.putExtra("username" , usernames.get(i));
                            startActivity(intent);

                        }
                }

            }
        });

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateListView(location);
                ParseUser.getCurrentUser().put("location" , new ParseGeoPoint(location.getLatitude() , location.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();
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
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (last != null) {
                    updateListView(last);
                }
            }
        }
    }
}
