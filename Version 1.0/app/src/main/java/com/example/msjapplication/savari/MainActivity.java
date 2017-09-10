package com.example.msjapplication.savari;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

public class MainActivity extends AppCompatActivity{
    Switch switchuser = null;

    public void redirect(){
        if (ParseUser.getCurrentUser().get("Usertype").equals("Rider")){
            Intent intent = new Intent(getApplicationContext() , MapsActivity.class);
            startActivity(intent);
        }else if (ParseUser.getCurrentUser().get("Usertype").equals("Driver")){
            Intent intent = new Intent(getApplicationContext() , DriverActivity.class);
            startActivity(intent);
        }
    }

    public void getStarted(View view) {
        String userType = "Rider";
        if (switchuser.isChecked()) {
            userType = "Driver";
        } else {
            userType = "Rider";
        }
        ParseUser.getCurrentUser().put("Usertype" ,userType );
        ParseUser.getCurrentUser().saveInBackground();
        redirect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Parse.initialize(new Parse.Configuration.Builder(getApplicationContext())
                .applicationId("88e1d60d64dadb2bbe6e38b5868b4deaa8e5e009")
                .clientKey("0d1be73d44ef33fb4553aab062aceacaff4b089e")
                .server("http://ec2-52-66-113-145.ap-south-1.compute.amazonaws.com:80/parse/")
                .build()
        );

        switchuser = (Switch) findViewById(R.id.switchuser);

        if(ParseUser.getCurrentUser() == null){
            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if(e == null){
                        Toast.makeText(MainActivity.this, "WELCOME", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(MainActivity.this, "LOGIN FAILED", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }else{
            if (ParseUser.getCurrentUser().get("Usertype") != null){
                ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        Toast.makeText(MainActivity.this,"Redirecting as " + ParseUser.getCurrentUser().get("Usertype"), Toast.LENGTH_SHORT).show();
                        redirect();
                    }
                });
            }
        }

        //ParseUser.enableAutomaticUser();
        ParseACL defaultACL = new ParseACL();
        defaultACL.setPublicReadAccess(true);
        defaultACL.setPublicWriteAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);
    }
}
