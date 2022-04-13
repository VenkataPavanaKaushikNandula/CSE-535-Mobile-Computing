package edu.asu.mobile_offloading_master;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import static java.nio.charset.StandardCharsets.UTF_8;
import edu.asu.mobile_offloading_master.constants.Constants;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import android.location.LocationListener;


public class Secondary extends AppCompatActivity implements LocationListener {

    private int requestProcessed = 0;

    private enum ACTION_CODES {
        ACTION_CODES_STATE(201),
        ACTION_CODES_ERR(400),
        ACTION_CODES_RES(200),
        ACTION_CODES_ST(203),
        ACTION_CODES_TRACK(202),
        ACTION_CODES_CALC(204);

        int actionCode;

        ACTION_CODES(int i) {
            actionCode = i;
        }
    }

    Button turnOff;
    private String primaryId;
    private Boolean isTracking = false;
    Handler trackingHandler;
    Runnable trackingRunnable;
    TextView primaryTV, stateTV, powerLevelTV;
    ProgressBar bar;
    boolean isBroadCasting = false;
    Location currentCoords;

    LocationRequest lcreq;
    LocationCallback lcHandler;



    private FusedLocationProviderClient fusedLocationProviderClient;

    private ConnectionsClient communicationClient;


    private final PayloadCallback dataHandler =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String id, Payload data) {
                    try {
                        InputStream is = data.asStream().asInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        String currentLine = "";

                        StringBuilder responseStrBuilder = new StringBuilder();
                        while (true) {

                            if (!((currentLine = reader.readLine()) != null)) break;


                            responseStrBuilder.append(currentLine);
                        }

                        is.close();
                        JSONObject jsonObject = new JSONObject(responseStrBuilder.toString());
                        int action_code = jsonObject.getInt(Constants.REQUEST_CODE_STR);
                        switch (action_code) {
                            case 202:
                                if (!isTracking) {
                                    Toast.makeText(getApplicationContext(), "Tracking...", Toast.LENGTH_SHORT).show();
                                    trackingHandler = new Handler();
                                    trackingRunnable = new Runnable() {
                                        @Override
                                        public void run() {
                                            updatePrimary();
                                            trackingHandler.postDelayed(trackingRunnable, Constants.DELAY_MILLIS);
                                        }
                                    };
                                    trackingRunnable.run();

                                    isTracking = true;
                                }
                                break;
                            case 203:
                                int st_code = jsonObject.getInt(Constants.STATUS_CODE);
                                if(st_code == 0){
                                    showResult();
                                }
                            case 204:
                                Gson gs = new Gson();
                                int[][] matrixA = gs.fromJson(jsonObject.getString(Constants.MATRIX_A), int[][].class);
                                int[][] matrixB = gs.fromJson(jsonObject.getString(Constants.MATRIX_B), int[][].class);

                                calculateProduct(matrixA, matrixB);
                                break;

                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }


                }

                @Override
                public void onPayloadTransferUpdate(String id, PayloadTransferUpdate up) {
                }
            };

    private void calculateProduct(int[][] matA, int[][] gridB) {
        bar.setVisibility(View.VISIBLE);
        int index_1, index_2, index_3;
        int[][] productMat = new int[matA.length][matA.length];
        for (index_1 = 0; index_1 < matA.length; index_1++) {
            for (index_2 = 0; index_2 < gridB.length; index_2++) {
                productMat[index_1][index_2] = 0;
                for (index_3 = 0; index_3 < matA.length; index_3++)
                    productMat[index_1][index_2] += matA[index_1][index_3]
                            * gridB[index_3][index_2];
            }
        }
        ACTION_CODES actionCodesRes = ACTION_CODES.ACTION_CODES_RES;
        JSONObject jsonObject = new JSONObject();
        try {
            Gson gs = new Gson();
            jsonObject.put(Constants.REQUEST_CODE1, actionCodesRes.actionCode);
            String result = gs.toJson(productMat);
            jsonObject.put(Constants.RESULT, result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Payload data = Payload.fromStream(new ByteArrayInputStream(jsonObject.toString().getBytes(UTF_8)));
        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(primaryId, data);
        requestProcessed++;
    }

    @Override
    public void onLocationChanged(Location lc) {
        if(lc!=null){
            currentCoords = lc;
        }
    }

    @Override
    public void onStatusChanged(String str, int a, Bundle b) {

    }

    @Override
    public void onProviderEnabled(String str) {

    }

    @Override
    public void onProviderDisabled(String str) {

    }



    private void showResult(){
        bar.setVisibility(View.INVISIBLE);
        Toast.makeText(getApplicationContext(), String.format("Processed operations: %s", requestProcessed),Toast.LENGTH_SHORT).show();
        requestProcessed = 0;
    }

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_secondary);
        turnOff = findViewById(R.id.bShutdown);
        primaryTV = findViewById(R.id.identifierTV);
        stateTV = findViewById(R.id.tvStatus);
        powerLevelTV = findViewById(R.id.powerLevelTV);
        bar = findViewById(R.id.pbar);
        bar.setVisibility(View.INVISIBLE);
        turnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (primaryId == null) {
                    return;
                }
                Nearby.getConnectionsClient(getApplicationContext()).disconnectFromEndpoint(primaryId);
                Toast.makeText(getApplicationContext(), "Connection terminated from master", Toast.LENGTH_LONG).show();
                primaryTV.setText("");
                stateTV.setText("None");
                if(isBroadCasting){
                    Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
                }
                showResult();
                isBroadCasting = false;
                beginBroadcast();
            }
        });

        communicationClient = Nearby.getConnectionsClient(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        lcreq = LocationRequest.create();
        lcreq.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        lcreq.setInterval(1000);
        lcreq.setSmallestDisplacement(1);

        lcHandler = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult lcRes) {
                if (lcRes == null) {
                    return;
                }

                Location lc = lcRes.getLastLocation();
                if(lc != null){
                    currentCoords = lc;
                }
            }
        };

        getNewLocation();
        beginBroadcast();

    }

    private final ConnectionLifecycleCallback transferCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(final String edID, final ConnectionInfo ci) {



                    AlertDialog.Builder popUpBuilder = new AlertDialog.Builder(Secondary.this);
                    popUpBuilder.setMessage(Constants.CONNECTION_FROM_MASTER);
                 popUpBuilder.setPositiveButton(Constants.YES,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface ag, int ag1) {
                                    Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(edID, dataHandler);
                                }
                            });

                    popUpBuilder.setNegativeButton(Constants.NO, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dg, int i) {
                            finish();
                        }
                    });

                    AlertDialog popUp = popUpBuilder.create();
                    popUp.show();
                }

                @Override
                public void onConnectionResult(String edPt, ConnectionResolution res) {
                    if (res.getStatus().isSuccess()) {
                        Toast.makeText(getApplicationContext(), Constants.APPLICATION_IS_CONNECTED, Toast.LENGTH_LONG).show();
                        primaryId = edPt;
                        primaryTV.setText(edPt);
                        stateTV.setText(Constants.CONNECTED);
                        Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
                        isBroadCasting = false;
                        updatePrimary();
                    } else {
                        Log.i(Constants.secondaryTagName, "Failed to connect");
                    }
                }

                @Override
                public void onDisconnected(String edPt) {
                    Toast.makeText(getApplicationContext(), "Can't connect to master", Toast.LENGTH_SHORT).show();
                    primaryTV.setText("");
                    stateTV.setText(Constants.NONE);
                    showResult();
                    Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
                    isBroadCasting = false;
                    Intent it = new Intent(Secondary.this, MainActivity.class);
                    startActivity(it);
                }
            };

    @SuppressLint("MissingPermission")
    private void updatePrimary() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                powerLevelTV.setText(fetchRemainingPowerValues() + " %");
            }
        });
        double xcoord = 0;
        double ycoord = 0;
        if (currentCoords != null) {
            xcoord = currentCoords.getLatitude();
            ycoord = currentCoords.getLongitude();
        }
        ACTION_CODES actionCodesState = ACTION_CODES.ACTION_CODES_STATE;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("request_code", actionCodesState.actionCode);
            jsonObject.put("battery_level", fetchRemainingPowerValues());
            jsonObject.put("long", ycoord);
            jsonObject.put("lat", xcoord);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Payload data = Payload.fromStream(new ByteArrayInputStream(jsonObject.toString().getBytes(UTF_8)));
        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(primaryId, data);

    }

    private int fetchRemainingPowerValues() {
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent registerReceiver = registerReceiver(null, batteryFilter);

        int extraL = registerReceiver.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int extraS = registerReceiver.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float percent = extraL / (float) extraS;

        return (int) (percent * 100);
    }




    private void beginBroadcast() {
        if (isBroadCasting) {
            Toast.makeText(getApplicationContext(), "Broadcast is already in progress", Toast.LENGTH_SHORT).show();
            return;
        }
        Nearby.getConnectionsClient(getApplicationContext()).startAdvertising(
                "slave", Constants.LogID, transferCallback,
                new AdvertisingOptions.Builder().setStrategy(Constants.STRATEGY).build()).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Operation Failed" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void b) {
                isBroadCasting = true;
            }
        });


    }

    public void getNewLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    Constants.ACTION_CODE);
        } else {
            fusedLocationProviderClient.requestLocationUpdates(lcreq, lcHandler, null);
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location lc) {
                    if (lc != null) {
                        currentCoords = lc;
                        Log.d(Constants.secondaryTagName, currentCoords.toString());
                    } else {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                            return;
                        }
                        fusedLocationProviderClient.requestLocationUpdates(lcreq, lcHandler, null);
                    }
                }
            });
        }
    }

    private static boolean checkPermissions(Context cxt, String... prms) {
        for (String p : prms) {
            if (ContextCompat.checkSelfPermission(cxt, p)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int operationResult, @NonNull String[] p, @NonNull int[] res) {
        super.onRequestPermissionsResult(operationResult, p, res);

        if (operationResult != Constants.ACTION_CODE) {
            return;
        }

        for (int i : res) {
            if (i == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(getApplicationContext(), "Failed to get required permission", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onSuccess(Location lc) {
                    fusedLocationProviderClient.requestLocationUpdates(lcreq, lcHandler, null);
                    if (lc != null) {
                        currentCoords = lc;
                        Log.d(Constants.secondaryTagName, currentCoords.toString());
                    }
                    else{
                        fusedLocationProviderClient.requestLocationUpdates(lcreq, lcHandler, null);
                    }
                }
            });
        }
        recreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onStart() {
        super.onStart();

        if (!checkPermissions(this, Constants.REQUIRED_PERMISSIONS)) {
            requestPermissions(Constants.REQUIRED_PERMISSIONS, Constants.ACTION_CODE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Nearby.getConnectionsClient(getApplicationContext()).stopAllEndpoints();
    }
}