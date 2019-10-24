package com.capi.funshime;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.capi.funshime.model.DailyWeatherReport;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WeatherActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener , GoogleApiClient.ConnectionCallbacks, LocationListener {

    private final int PERMISSION_LOCATION = 111;

    final String URL_BASE = "http://api.openweathermap.org/data/2.5/forecast";
    final String URL_COORD = "?lat="; //"?lat=-71.956871&lon=24.607752";
    final String URL_UNITS = "&units=metric";
    final String URL_API_KEY = "&APPID=91e522f8cacadd63899e03f084f34447";

    private GoogleApiClient googleApiClient;

    private ArrayList<DailyWeatherReport> weatherReportList = new ArrayList<>();

    private ImageView weatherIcon;
    private ImageView weatherIconMini;
    private TextView weatherDate;
    private TextView currentTemp;
    private TextView lowTemp;
    private TextView cityCountry;
    private TextView weatherDesc;

    // 38.805890, -9.381185 sintra
    // 65.423210, -52.905789 gronolandia
    // -71.956871, 24.607752 antartida

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        weatherIcon = (ImageView) findViewById(R.id.weatherIcon);
        weatherIconMini = (ImageView) findViewById(R.id.weatherIconMini);
        weatherDate = (TextView) findViewById(R.id.weatherDate);
        currentTemp = (TextView) findViewById(R.id.currentTemp);
        lowTemp = (TextView) findViewById(R.id.lowTemp);
        cityCountry = (TextView) findViewById(R.id.cityCountry);
        weatherDesc = (TextView) findViewById(R.id.weatherDesc);

        googleApiClient = new GoogleApiClient.Builder(this)
        .addApi(LocationServices.API)
        .enableAutoManage(this, this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .build();

        //pretend to have a location
        downloadWeatherData(null);


    }

    public void downloadWeatherData(Location location) {

//        final String fullCoordinates = URL_COORD + location.getLatitude() + "&lon=" + location.getLongitude();
        //hardcode so it works on simulator
        final String fullCoordinates = "?lat=-65.423210&lon=-52.905789";

        final String url = URL_BASE + fullCoordinates + URL_UNITS + URL_API_KEY;

        final JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        String cityName;
                        String country;

                        Log.v("SHINE ", "GOOD STUF: " + response);

                        try {
                            //grabbing the top level information
                            JSONObject city = response.getJSONObject("city");
                            if (city.length() > 0) {
                                cityName = city.getString("name");
                                country = city.getString("country");
                            } else {
                                cityName = "Batatas";
                                country = "Cebolas";
                            }

                            JSONArray list = response.getJSONArray("list");

                            for (int x =0; x<5; x++)  {
                                JSONObject object = list.getJSONObject(x);
                                JSONObject main = object.getJSONObject("main");
                                Double currentTemp = main.getDouble("temp");
                                Double temp_min = main.getDouble("temp_min");
                                Double temp_max = main.getDouble("temp_max");

                                JSONArray weatherArray = object.getJSONArray("weather");
                                JSONObject weatherDesc = weatherArray.getJSONObject(0);
                                String weatherType = weatherDesc.getString("main");

                                String rawDate = object.getString("dt_txt");

                                DailyWeatherReport report = new DailyWeatherReport(cityName, country, currentTemp.intValue(),
                                        temp_max.intValue(), temp_min.intValue(), weatherType, rawDate);

                                weatherReportList.add(report);
                            }

                            Log.v("JSON", "Name " + cityName + " Country " + country);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        updateUi();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.v("SHINE ", "ERR: " + error.getLocalizedMessage());

            }
        });

        Volley.newRequestQueue(this).add(jsonRequest);

    }

    public void updateUi() {

        if (weatherReportList.size() > 0) {
            DailyWeatherReport report = weatherReportList.get(0);

            switch (report.getWeather()) {
                case DailyWeatherReport.WEATHER_TYPE_CLEAR:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_SNOW:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.snow));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.snow));
                    break;
                default:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.thunder_lightning));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.thunder_lightning));

            }


            weatherDate.setText("Today, March 20th");
            currentTemp.setText(Integer.toString(report.getCurrentTemp()));
            lowTemp.setText(Integer.toString(report.getMinTemp()));
            cityCountry.setText(report.getCityName() + ", " + report.getCountry());
            weatherDesc.setText(report.getWeather());

        }


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
        } else {
            startLocationServices();
        }

    }

    private void startLocationServices() {

        try {
            LocationRequest request = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, this);
        } catch (SecurityException exception) {
            // do something here to handle exception
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        downloadWeatherData(location);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationServices();
                } else {

                    Toast.makeText(this, "Can not determine location", Toast.LENGTH_LONG).show();

                }
            }
        }
    }
}
