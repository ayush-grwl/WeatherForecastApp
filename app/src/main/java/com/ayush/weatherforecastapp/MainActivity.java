package com.ayush.weatherforecastapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView homeTextView,weatherTextView,tempTextView,windTextView,visibilityTextView,timeTextView;
    ImageView weatherImageView;
    FusedLocationProviderClient fusedLocationProviderClient;
    double latitude;
    double longitude;
    String address;
    SharedPreferences sharedPreferences;

    public class DownloadTask extends AsyncTask<String,Void,String>{

        String result="";
        URL url;
        HttpURLConnection httpURLConnection;

        @Override
        protected String doInBackground(String... strings) {

            try {
                url=new URL(strings[0]);
                httpURLConnection=(HttpURLConnection)url.openConnection();
                InputStream is=httpURLConnection.getInputStream();
                InputStreamReader isr=new InputStreamReader(is);
                while(true){
                    int n=isr.read();
                    if(n==-1)
                        break;
                    else{
                        char ch=(char)n;
                        result=result+ch;
                    }
                }
                return result;
            } catch (Exception e) {
                Log.i("Error","Error="+e.getMessage());
                return null;
            }

        }

        @Override
        protected void onPostExecute(String s) {

            super.onPostExecute(s);
            String finalResult="";
            try{
                JSONObject object=new JSONObject(s);
                homeTextView.setText(address);
                String weather=object.getString("weather");
                JSONArray weatherArr=new JSONArray(weather);
                JSONObject weatherObject=weatherArr.getJSONObject(0);
                String weatherData=weatherObject.getString("main")+",\n"+weatherObject.getString("description");
                weatherTextView.setText(weatherData);
                try {
                    String url1="https://openweathermap.org/img/wn/"+weatherObject.getString("icon")+"@2x.png";
                    URL url = new URL(url1);
                    Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    weatherImageView.setImageBitmap(bmp);
                } catch (Exception e) {
                    Log.i("Error","Error="+e.getMessage());
                }

                JSONObject tempObject=object.getJSONObject("main");
                String tempData="Temperature:"+tempObject.getString("temp")+"\nPressure:"+tempObject.getString("pressure")+"\nHumidity:"+tempObject.getString("humidity");
                tempTextView.setText(tempData);

                String visibilityData="Visibility:"+object.getString("visibility");
                visibilityTextView.setText(visibilityData);

                JSONObject windObject=object.getJSONObject("wind");
                String windData="Speed:"+windObject.getString("speed");
                windTextView.setText(windData);

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy_HH:mm", Locale.getDefault());
                String currentDateandTime = sdf.format(new Date());
                timeTextView.setText(currentDateandTime);

                sharedPreferences.edit().putBoolean("hasData",true).apply();
                sharedPreferences.edit().putString("address",address).apply();
                sharedPreferences.edit().putString("weather",weatherData).apply();
                sharedPreferences.edit().putString("weatherIcon",weatherObject.getString("icon")).apply();
                sharedPreferences.edit().putString("temperature",tempData).apply();
                sharedPreferences.edit().putString("visibility",visibilityData).apply();
                sharedPreferences.edit().putString("wind",windData).apply();
                sharedPreferences.edit().putString("date",currentDateandTime).apply();
            }catch (Exception e){
                Log.i("Error","Error="+e.getMessage());
            }

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new
        StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        sharedPreferences=getSharedPreferences(getString(R.string.file),MODE_PRIVATE);
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this);
        homeTextView=(TextView)findViewById(R.id.homeTextView);
        weatherTextView=(TextView)findViewById(R.id.weatherTextView);
        weatherImageView=(ImageView)findViewById(R.id.weatherImageView);
        tempTextView=(TextView)findViewById(R.id.tempTextView);
        visibilityTextView=(TextView)findViewById(R.id.visibilityTextView);
        windTextView=(TextView)findViewById(R.id.windTextView);
        timeTextView=(TextView)findViewById(R.id.timeTextView);
        setData();
        currentWeather();

    }

    private void setData() {

        if(sharedPreferences.getBoolean("hasData",false)){
            homeTextView.setText(sharedPreferences.getString("address","Location"));
            weatherTextView.setText(sharedPreferences.getString("weather","Weather"));
            tempTextView.setText(sharedPreferences.getString("temperature","Temperature"));
            visibilityTextView.setText(sharedPreferences.getString("visibility","Visibility"));
            windTextView.setText(sharedPreferences.getString("wind","Wind"));
            timeTextView.setText(sharedPreferences.getString("date","Time"));
        }

    }

    @SuppressLint("NewApi")
    public void currentWeather(){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            final LocationManager locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
            if(locationManager.isProviderEnabled(locationManager.GPS_PROVIDER)){
                getData();
            }
            else{
                AlertDialog.Builder builder=new AlertDialog.Builder(this);
                builder.setMessage("Device location is turned off, Turn on the device location. Do you want to turn on location. Restart the app after turning on location.");
                builder.setCancelable(false);
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                AlertDialog dialog=builder.create();
                dialog.show();
            }

        }
        else{
            askPermission();
        }

    }

    private void getData() {

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        try {
                            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            latitude = addresses.get(0).getLatitude();
                            longitude = addresses.get(0).getLongitude();
                            address = addresses.get(0).getAddressLine(0);
                            DownloadTask task = new DownloadTask();
                            String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=0917117e3e000e954a145026d416306f";
                            task.execute(url);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.i("Error", "Error=" + e.getMessage());
                        }
                    } else {
                        Log.i("Location", "Null");
                    }
                }
            });
        }

    }

    private void askPermission() {

        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},100);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==100){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                currentWeather();
            }
        }

    }
}