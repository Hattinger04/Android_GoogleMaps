package com.example.googlemaps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SimpleMapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private TextView feedback;
    private int markerCnt = 0;
    private String url = "";
    private String url2 = "";
    private String appKey = "";
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private double latitude = 47.259659;
    private double longitude = 11.400375;
    @SuppressLint("MissingPermission")   //ignore the warning
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appKey = getString(R.string.app_key);
        url = getString(R.string.url);
        url2 = getString(R.string.url2);
        setContentView(R.layout.activity_simple_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        feedback = findViewById(R.id.feedback);
        getLocation();
    }

    // Sollte Map auf Reallife Location setzen
    public void getLocation() {
        //Settings how frequently updates should be fetched
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000); //this is the lowest rate at which update is called
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //this callback is periodically called
        LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                TextView locationText = findViewById(R.id.feedback);
                Log.d("LOCATION", "calling callback");
                if (locationResult == null) {
                    Log.d("LOCATION", "locationResult is null");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        String locationStr = String.format("lat: %.2f long: %.2f alt: %.2f", location.getLatitude(), location.getLongitude(), location.getAltitude());
                        locationText.setText(locationStr);
                    }
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper());
    }

    public void getRequest(View view) {
        /*
            Wäre sinnvoll, wenn man eine ID zum GetRequest mitgeben könnte
            String id = ((EditText)findViewById(R.id.IDText)).getText().toString();
            if(id == null)
                return;
        */
        GetServerAsyncTask task = new GetServerAsyncTask();

        task.execute(url2);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng ibk = new LatLng(47.259659,11.400375);
        mMap.addMarker(new MarkerOptions().position(ibk).title("Marker in Innsbruck"));

        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ibk,10)); //Values from 2 to 21 possible
        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerClickListener(this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        feedback.setText(marker.getTitle());
        return true;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng).title(String.format("Marker %d", ++markerCnt)));
        longitude = latLng.longitude;
        latitude = latLng.latitude;
        PutServerAsyncTask task = new PutServerAsyncTask();
        task.execute(url + "0");

        feedback.setText("New Marker created!");
    }

    private class PutServerAsyncTask extends AsyncTask<String, Void, String> {
        @lombok.SneakyThrows  //to avoid exception handling
        @Override
        protected String doInBackground(String... urls) {

            OkHttpClient client = new OkHttpClient();
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("info", new JSONObject()
                .put("lat", latitude)
                .put("long", longitude)
                .put("message", "test")
                .put("name", "SG"));
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(jsonObject);
                Log.d("JSON", json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
            Request request =
                    new Request.Builder()
                            .url(urls[0])
                            .addHeader("Accept", "application/json")
                            .addHeader("appkey", appKey)
                            .put(body)
                            .build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            }
            return "Upload failed";
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            feedback.setText(result);
            //we already know Gson for JSON Processing
            Gson gson = new Gson();
            Todo todo = gson.fromJson(result, Todo.class);
            feedback.append("\n");
            feedback.append(String.format("TITLE: %s\n",todo.getName()));
        }
    }

    private class GetServerAsyncTask extends AsyncTask<String, Void, String> {
        @lombok.SneakyThrows  //to avoid exception handling
        @Override
        protected String doInBackground(String... urls) {
            OkHttpClient client = new OkHttpClient();
            Request request =
                    new Request.Builder()
                            .url(urls[0])
                            .addHeader("Accept", "application/json")
                            .addHeader("appkey", appKey)
                            .get()
                            .build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            }
            return "Download failed";
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            //we already know Gson for JSON Processing
            result = result.replaceAll("long", "loong");
            Log.d("result", result);
            Gson gson = new Gson();
            Todo[] todo = gson.fromJson(result, Todo[].class);
            for(int i = 0; i < todo.length; i++) {

                mMap.addMarker(new MarkerOptions().position(new LatLng(todo[i].getLat(), todo[i].getLoong())).title(String.format("Marker %d", ++markerCnt)));
            }
        }
    }
    class Todo
    {
        private int id;
        @Getter
        private String name;
        @Getter
        private float loong;
        @Getter
        private float lat;
        private String message;
        private String when;

    }
}
