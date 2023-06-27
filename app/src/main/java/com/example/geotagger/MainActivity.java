package com.example.geotagger;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;

public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    EditText MasterID, ChannelID, ListenerID, SensorID, Latitude, Longitude, Accuracy, BarcodeID, UID;
    Button update, Scanbutton, Getlocation, Export, Share;

    private static final String URL = "jdbc:mysql://114.143.243.102:3306/pids_data";
    private static final String USER = "master1";
    private static final String PASSWORD = "master1@1Fence";

    private static final int NUM_LOCATION_SAMPLES = 20;
    private static final long LOCATION_COLLECTION_TIME = 10000; // 15 seconds
    private static final long LOCATION_UPDATE_INTERVAL = 300; // 1 second
    private static final long LOCATION_UPDATE_FASTEST_INTERVAL = 100; // 0.1 second

    private boolean collectingLocations = false;

    protected boolean shouldAskPermissions() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @TargetApi(30)
    protected void askPermissions() {
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
        int requestCode = 200;
        requestPermissions(permissions, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (shouldAskPermissions()) {
            askPermissions();
        }

        MasterID = (EditText) findViewById(R.id.editmasterid);
        ChannelID = (EditText) findViewById(R.id.editchannelid);
        ListenerID = (EditText) findViewById(R.id.editlistenerid);
        SensorID = (EditText) findViewById(R.id.editTextNumberDecimal5);
        BarcodeID = (EditText) findViewById(R.id.editTextNumberDecimal6);
        Latitude = (EditText) findViewById(R.id.editTextNumberSigned);
        Longitude = (EditText) findViewById(R.id.editTextNumberSigned2);
        Export = (Button) findViewById(R.id.exportbutton);
        Getlocation = (Button) findViewById(R.id.button3);
        Accuracy = (EditText) findViewById(R.id.editTextTextPersonName);
        Scanbutton = (Button) findViewById(R.id.scanbutton);
        Share = (Button) findViewById(R.id.share);
        UID = (EditText) findViewById(R.id.editTextNumberDecimal4);
        update = (Button) findViewById(R.id.button4);

        Latitude.setText(null);
        Longitude.setText(null);
        Accuracy.setText(null);
        BarcodeID.setText(null);
        UID.setText(null);
        SensorID.setText(null);


        Getlocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Latitude.setText(null);
                Longitude.setText(null);
                Accuracy.setText(null);

                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
                } else {
//                    getCurrentLocation();
                    startLocationCollection();
                }
            }
        });

        Scanbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BarcodeID.setText(null);
                scanCode();

            }
        });

        Export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (SensorID.getText().toString().trim().length() <= 0){
                    SensorID.setError("Vigil ID is required");
                    SensorID.requestFocus();
                    return;
                }else{
                    int sens_id = new Integer(SensorID.getText().toString()).intValue();
                    SensorID.setError(null);
                    if(sens_id<1 || sens_id>100){
                        SensorID.setError("Vigil ID should be in range 1-100");
                        SensorID.requestFocus();
                        return;
                    }
                }

                if (UID.getText().toString().trim().length() <= 0) {
                    UID.setError("Please Scan the barcode to recieve the UID");
                    UID.requestFocus();
                    return;
                }else{
                    UID.setError(null);
                }

                if (BarcodeID.getText().toString().trim().length() <= 0) {
                    BarcodeID.setError("Please Scan the barcode");
                    BarcodeID.requestFocus();
                    return;
                }else{
                    BarcodeID.setError(null);
                }

                if (Latitude.getText().toString().trim().length() <= 0) {
                    Latitude.setError("Please Click on Locate to Get Latitude");
                    Latitude.requestFocus();
                    return;
                }else{
                    Latitude.setError(null);
                }

                if (Longitude.getText().toString().trim().length() <= 0) {
                    Longitude.setError("Please Click on Locate to Get Longitude");
                    Longitude.requestFocus();
                    return;
                }else{
                    Longitude.setError(null);
                }
                if (MasterID.getText().toString().trim().length() <= 0){
                    MasterID.setError("Master ID is required");
                    MasterID.requestFocus();
                    return;
                }else{
                    MasterID.setError(null);
                    int master_id = new Integer(MasterID.getText().toString()).intValue();
                    if(master_id<1 || master_id>15){
                        MasterID.setError("Master ID should be in range 1-15");
                        MasterID.requestFocus();
                        return;
                    }
                }


                if (ChannelID.getText().toString().trim().length() <= 0) {
                    ChannelID.setError("Channel ID is required");
                    ChannelID.requestFocus();
                    return;
                }else{
                    ChannelID.setError(null);
                    int channel_id = new Integer(ChannelID.getText().toString()).intValue();
                    if(channel_id<1 || channel_id>2){
                        ChannelID.setError("Channel ID should be in range either 1 or 2");
                        ChannelID.requestFocus();
                        return;
                    }

                }

                if (ListenerID.getText().toString().trim().length() <= 0){
                    ListenerID.setError("Listener Master ID is required");
                    ListenerID.requestFocus();
                    return;
                }else{
                    ListenerID.setError("null");
                    int listner_id = new Integer(ListenerID.getText().toString()).intValue();
                    if(listner_id>15){
                        ListenerID.setError("Listener ID should be in range 1-15");
                        ListenerID.requestFocus();
                        return;
                    }
                }

                try {

                    File sdCardDir = new File(Environment.getExternalStorageDirectory() + "/A1Fence");

                    if (!sdCardDir.exists()) {
                        sdCardDir.mkdir();
                        String filename = "Sensor.csv";
                        File f = new File(sdCardDir, filename);
                        FileWriter fw = new FileWriter(f);
                        BufferedWriter bw = new BufferedWriter(fw);
                        bw.write("Sensor ID" + ",");
                        bw.write("Barcode" + ",");
                        bw.write("UID" + ",");
                        bw.write("Master ID" + ",");
                        bw.write("Channel ID" + ",");
                        bw.write("Listener Master ID" + ",");
                        bw.write("Latitude" + ",");
                        bw.write("Longitude" + ",");
                        bw.newLine();
                        bw.flush();
                        Toast.makeText(MainActivity.this, "CSV file Generated!", Toast.LENGTH_SHORT).show();
                    }

                    String filename = "Sensor.csv";
                    File f = new File(sdCardDir, filename);
                    FileWriter fw = new FileWriter(f, true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.append(SensorID.getText() + ",");
                    bw.append(BarcodeID.getText() + ",");
                    bw.append(UID.getText() + ",");
                    bw.append(MasterID.getText() + ",");
                    bw.append(ChannelID.getText() + ",");
                    bw.append(ListenerID.getText() + ",");
                    bw.append(Latitude.getText() + ",");
                    bw.append(Longitude.getText() + ",");
                    bw.newLine();
                    bw.flush();
                    Toast.makeText(MainActivity.this, "Sensor data saved to file", Toast.LENGTH_SHORT).show();

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        });
        Share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Context context = getApplicationContext();
                    File filelocation = new File(Environment.getExternalStorageDirectory() + "/A1Fence/", "Sensor.csv");
                    Uri path = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", filelocation);
                    Intent fileIntent = new Intent(Intent.ACTION_SEND);
                    fileIntent.setType("text/csv");
                    fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Data");
                    fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    fileIntent.putExtra(Intent.EXTRA_STREAM, path);
                    startActivity(Intent.createChooser(fileIntent, "Send mail"));
                } catch (Exception e) {
                    System.out.println(e);
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }

        });
        update.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                  if (SensorID.getText().toString().trim().length() <= 0){
                      SensorID.setError("Vigil ID is required");
                      SensorID.requestFocus();
                      return;
                  }else{
                      int sens_id = new Integer(SensorID.getText().toString()).intValue();
                      SensorID.setError(null);
                      if(sens_id<1 || sens_id>100){
                          SensorID.setError("Vigil ID should be in range 1-100");
                          SensorID.requestFocus();
                          return;
                      }
                  }


                  if (UID.getText().toString().trim().length() <= 0) {
                      UID.setError("Please Scan the barcode to recieve the UID");
                      UID.requestFocus();
                      return;
                  }else{
                      UID.setError(null);
                  }

                  if (BarcodeID.getText().toString().trim().length() <= 0) {
                      BarcodeID.setError("Please Scan the barcode");
                      BarcodeID.requestFocus();
                      return;
                  }else{
                      BarcodeID.setError(null);
                  }

                  if (Latitude.getText().toString().trim().length() <= 0) {
                      Latitude.setError("Please Click on Locate to Get Latitude");
                      Latitude.requestFocus();
                      return;
                  }else{
                      Latitude.setError(null);
                  }

                  if (Longitude.getText().toString().trim().length() <= 0) {
                      Longitude.setError("Please Click on Locate to Get Longitude");
                      Longitude.requestFocus();
                      return;
                  }else{
                      Longitude.setError(null);
                  }
                  if (MasterID.getText().toString().trim().length() <= 0){
                      MasterID.setError("Master ID is required");
                      MasterID.requestFocus();
                      return;
                  }else{
                      MasterID.setError(null);
                      int master_id = new Integer(MasterID.getText().toString()).intValue();
                      if(master_id<1 || master_id>15){
                          MasterID.setError("Master ID should be in range 1-15");
                          MasterID.requestFocus();
                          return;
                      }
                  }


                  if (ChannelID.getText().toString().trim().length() <= 0) {
                      ChannelID.setError("Channel ID is required");
                      ChannelID.requestFocus();
                      return;
                  }else{
                      ChannelID.setError(null);
                      int channel_id = new Integer(ChannelID.getText().toString()).intValue();
                      if(channel_id<1 || channel_id>2){
                          ChannelID.setError("Channel ID should be in range either 1 or 2");
                          ChannelID.requestFocus();
                          return;
                      }

                  }

                  if (ListenerID.getText().toString().trim().length() <= 0){
                      ListenerID.setError("Listener Master ID is required");
                      ListenerID.requestFocus();
                      return;
                  }else{
                      ListenerID.setError(null);
                      int listner_id = new Integer(ListenerID.getText().toString()).intValue();
                      if(listner_id>15){
                          ListenerID.setError("Listener ID should be in range 1-15");
                          ListenerID.requestFocus();
                          return;
                      }
                  }

                  new DBupdateAsyncTask().execute();
              }

        });


//        request for permission from the user if not yet granted
        try {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // this will request for permission from the user if not yet granted
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            } else {
                System.out.println("Not OK");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            Toast.makeText(this, "Please turn on GPS on your phone!", Toast.LENGTH_LONG).show();
            startActivity(intent);
        } else {
            Toast.makeText(this, "Fetching Location please wait", Toast.LENGTH_LONG).show();
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(1000)
                    .setFastestInterval(100)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setNumUpdates(5)
                    .setMaxWaitTime(100);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                    .requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            super.onLocationResult(locationResult);
                            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                    .removeLocationUpdates(this);
                            if (locationResult != null && locationResult.getLocations().size() > 0) {
                                int latestLocationIndex = locationResult.getLocations().size() - 1;
                                double latitude = locationResult.getLocations().get(latestLocationIndex).getLatitude();
                                double longitude = locationResult.getLocations().get(latestLocationIndex).getLongitude();
                                double lol = locationResult.getLocations().get(latestLocationIndex).getAltitude();
                                double accuracy = locationResult.getLocations().get(latestLocationIndex).getAccuracy();
                                Latitude.setText(String.valueOf(latitude));
                                Latitude.setEnabled(false);
                                Longitude.setText(String.valueOf(longitude));
                                Longitude.setEnabled(false);
                                Accuracy.setText(String.format("%.2f", accuracy) + " meters");
                                Accuracy.setEnabled(false);
                                Latitude.setError(null);
                                Longitude.setError(null);
                                Accuracy.setError(null);

                            }
                        }
                    }, Looper.getMainLooper());

        }
    }

    private void scanCode() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(CaptureAct.class);
        integrator.setOrientationLocked(false);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.UPC_A);
        integrator.setPrompt("Scanning Barcode");
        integrator.initiateScan();
    }

    private void startLocationCollection() {
        collectingLocations = true;
        Getlocation.setText("Loading...");

        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(LOCATION_UPDATE_FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(NUM_LOCATION_SAMPLES)
                .setMaxWaitTime(LOCATION_COLLECTION_TIME);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                .requestLocationUpdates(locationRequest, new LocationCallback() {
                    private List<Location> locationSamples = new ArrayList<>();

                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        locationSamples.addAll(locationResult.getLocations());

                        if (locationSamples.size() >= NUM_LOCATION_SAMPLES) {
                            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                    .removeLocationUpdates(this);
                            collectingLocations = false;
                            Getlocation.setText("Get Location");

                            Location weightedCentroid = calculateWeightedCentroid(locationSamples);
                            updateLocationUI(weightedCentroid);
                        }
                    }
                }, Looper.getMainLooper());
    }

    private Location calculateWeightedCentroid(List<Location> locations) {
        double totalWeight = 0;
        double weightedLatitude = 0;
        double weightedLongitude = 0;
        double maxAccuracy = Double.MAX_VALUE;

        for (Location location : locations) {
            double accuracy = location.getAccuracy();
            double weight = 1.0 / accuracy;
            totalWeight += weight;
            weightedLatitude += weight * location.getLatitude();
            weightedLongitude += weight * location.getLongitude();

            if (accuracy < maxAccuracy) {
                maxAccuracy = accuracy;
            }
        }

        double centroidLatitude = weightedLatitude / totalWeight;
        double centroidLongitude = weightedLongitude / totalWeight;

        Location centroidLocation = new Location("");
        centroidLocation.setLatitude(centroidLatitude);
        centroidLocation.setLongitude(centroidLongitude);
        centroidLocation.setAccuracy((float) maxAccuracy);

        return centroidLocation;
    }

    private void updateLocationUI(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        double accuracy = location.getAccuracy();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Latitude.setText(String.valueOf(latitude));
                Latitude.setEnabled(false);
                Longitude.setText(String.valueOf(longitude));
                Longitude.setEnabled(false);
                Accuracy.setText(String.format("%.2f", accuracy) + " meters");
                Accuracy.setEnabled(false);
                Latitude.setError(null);
                Longitude.setError(null);
                Accuracy.setError(null);
                Getlocation.setText("LOCATE");
                collectingLocations = false;
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                BarcodeID.setText(result.getContents().substring(1, result.getContents().length() - 1));
                BarcodeID.setEnabled(false);
                new DBFetchAsyncTask().execute();


            } else {
                Toast.makeText(this, "Could not scan the barcode!", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class DBFetchAsyncTask extends AsyncTask<Void, Void, Map<String, String>> {
        @Override
        protected Map<String, String> doInBackground(Void... voids) {
            Map<String, String> info = new HashMap<>();

            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "SELECT UID, SID FROM pids_data.pids_details WHERE Serial_Number = " + BarcodeID.getText();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    info.put("Details",  " UID : " + resultSet.getString("UID") + " SID : " + resultSet.getString("SID"));
                    info.put("UID", resultSet.getString("UID"));
                    info.put("SID", resultSet.getString("SID"));

                }
            } catch (Exception e) {
                Log.e("InfoAsyncTask", "Error reading Sensor Information", e);
                info.put("Details", "Error reading Sensor Information");
            }

            return info;
        }

        @Override
        protected void onPostExecute(Map<String, String> result) {
            if (!result.isEmpty()) {

                Toast.makeText(getApplicationContext().getApplicationContext(), result.get("Details"), Toast.LENGTH_LONG).show();
                UID.setEnabled(false);
                UID.setText(String.valueOf(result.get("UID")));
                SensorID.setText(String.valueOf(result.get("SID")));

            }
        }
    }


    @SuppressLint("StaticFieldLeak")
    public class DBupdateAsyncTask extends AsyncTask<Void, Void, Map<String, String>> {
        @Override
        protected Map<String, String> doInBackground(Void... voids) {
            Map<String, String> info = new HashMap<>();


            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql1 = "Update pids_data.pids_details set latitude = " + Latitude.getText() +
                        ",longitude = " + Longitude.getText() +
                        ",SensorMaster_id = " + MasterID.getText() +
                        ",Listener_id = " + ListenerID.getText() +
                        ",Channel_id = " + ChannelID.getText() +
                        ",SID = " + SensorID.getText() +
                        " where Serial_Number = " + BarcodeID.getText();
                PreparedStatement statement = connection.prepareStatement(sql1);
                statement.executeUpdate(sql1);
                System.out.println("Records updated successfully");
                info.put("updatelog", "Records updated successfully");


            } catch (Exception e) {
                Log.e("InfoAsyncTask", "Error updating Sensor Information", e);
                info.put("updatelog" ,"Error updating Sensor Information ! Please Check the input fields");
            }

            return info;
        }

        @Override
        protected void onPostExecute(Map<String, String> result) {
            if (!result.isEmpty()) {
                Toast.makeText(getApplicationContext().getApplicationContext(), result.get("updatelog"), Toast.LENGTH_LONG).show();
                Latitude.setText(null);
                Longitude.setText(null);
                Accuracy.setText(null);
                BarcodeID.setText(null);
                UID.setText(null);
                SensorID.setText(null);

            }
        }
    }

}










