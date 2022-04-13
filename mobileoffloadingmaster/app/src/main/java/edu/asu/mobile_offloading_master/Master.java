package edu.asu.mobile_offloading_master;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;
import edu.asu.mobile_offloading_master.constants.Constants;

public class Master extends AppCompatActivity implements LocationListener {
    
    private ConnectionsClient ClientConnections;
    private HashMap<String, SecondaryDataModel> slavesMap;
    private static final int REQUEST_CODE = 500;
    private static Double minBatteryLevel = 30.0;
    public static final int maxDistance = 100;
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

    public static final int Endit = 0;
    Location LocationOnDevice;
    private LinkedHashMap<String, String> DistanceStats;
    private LinkedHashMap<String, String> StandloneStats;

    private int NoOfDisconnections = 0;
    LocationRequest LocatnReq;
    LocationCallback LocatnCBck;
    private FusedLocationProviderClient ClientLocatn;


    public static ArrayList<SecondaryDataModel> SlaveListRecycler = new ArrayList<>();
    private ItemList ApdapterOfSlaveList = new ItemList(SlaveListRecycler);

    @Override
    public void onLocationChanged(Location locatn) {
        if(locatn!=null){
            LocationOnDevice = locatn;
        }
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

    private enum REQUEST_CODES {
        DEVICE_STATUS(201),
        ERROR(400),
        RESULT(200),
        MONITOR(202),
        STATUS(203),
        CALCULATE(204);
        int request_code;

        REQUEST_CODES(int i) {
            request_code = i;
        }
    }

    private LinkedList<int[]> PartitionsOfMatrix;
    Button Monitor;
    Button MultiplicationButton;
    public static final int Dimension = 1000;

    private static volatile int[][] A = new int[Dimension][Dimension];
    private static volatile int[][] B = new int[Dimension][Dimension];
    private static volatile int[][] B_T = new int[Dimension][Dimension];


    private static volatile int[][] result = new int[Dimension][Dimension];

    private int SizeOfPartitn = 50;

    private int numberOfRequests;
    private int numberOfRequestsServed;
    Button SalesFinding, Disconnect;

    private HashMap<String, int[]> CurrRequests;

    private HashMap<int[], Boolean> Req;

    private HashMap<String, Long> ResponseTimeMostRecent;

    private long StartTimeOfDistributedComp;

    Handler StatusCheckerHandler;
    Runnable StatusCheckRunnable;
    public static final String TagNameLog = "Master";
    boolean isDiscovering = false;
    ProgressBar PeperProgress;

    private final PayloadCallback CallBackPayRoll =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(final String endpointId, Payload payload) {
                    try {

                        InputStream inputStream = payload.asStream().asInputStream();
                        BufferedReader bR = new BufferedReader(new InputStreamReader(inputStream));
                        String line;

                        StringBuilder responseStrBuilder = new StringBuilder();
                        while ((line = bR.readLine()) != null) {

                            responseStrBuilder.append(line);
                        }
                        inputStream.close();

                        JSONObject response = new JSONObject(responseStrBuilder.toString());
                        int request_code = response.getInt("request_code");
                        switch (request_code) {
                            case 201:
                                updateSlaveStatus(response, endpointId);
                                break;
                            case 200:
                                ResultsMerge(response, endpointId);
                                sendComputationRequestToSlave(endpointId);
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + request_code);
                        }

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }


                }


                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {

                }
            };

    private int BatteryLevelInfo() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent StatusOfBattery = registerReceiver(null, ifilter);

        int level = StatusOfBattery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = StatusOfBattery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float BatteryPercentage = level / (float) scale;

        return (int) (BatteryPercentage * 100);
    }

    private void ResultsMerge(final JSONObject response, String endpointId) {
        try {
            Gson Gson = new Gson();
            String resultString = response.getString("result");
            final int[][] computationResult = Gson.fromJson(resultString, int[][].class);
            final int[] indices = CurrRequests.get(endpointId);
            if(Req.get(indices)){
                CurrRequests.remove(endpointId);
                return;
            }
            Req.put(indices, true);
            numberOfRequestsServed++;



            if(Req.get(indices)){
                CurrRequests.remove(endpointId);
                return;
            }
            Req.put(indices, true);
            numberOfRequestsServed++;

            int CompletedPercentage = (int)(numberOfRequestsServed*100/numberOfRequests);
            if(CompletedPercentage%10 == 0){
                Log.d(TagNameLog,String.format("%d completed", CompletedPercentage ));
                Toast.makeText(getApplicationContext(), String.format("%d%% completed", CompletedPercentage), Toast.LENGTH_SHORT).show();
            }

            Log.d(TagNameLog, "Number of requests served: "+ numberOfRequestsServed);
//            Toast.makeText(getApplicationContext(), "Number of requests served: "+ numberOfRequestsServed, Toast.LENGTH_SHORT).show();

            CurrRequests.remove(endpointId);
            new Thread() {
                public void run() {
                    for (int i = indices[0]; i < indices[0] + SizeOfPartitn; i++) {
                        for (int j = indices[1]; j < indices[1] + SizeOfPartitn; j++) {
                            result[i][j] = computationResult[i - indices[0]][j - indices[1]];
                        }
                    }
                }
            }.start();

            if(numberOfRequestsServed == numberOfRequests){
                long distributedCompEndTime = System.currentTimeMillis();
                float distributedCompTime = (float) ((distributedCompEndTime - StartTimeOfDistributedComp)/1000.0);

                int LevelOfEndBattery = BatteryLevelInfo();
                DistanceStats.put("End Master battery level", String.valueOf(LevelOfEndBattery));
                double StartBatteryLevelSlaveAVG = 0;
                for (Map.Entry<String, SecondaryDataModel> slave : slavesMap.entrySet()) {
                    int BatteryLevelOfSlave = slave.getValue().remainingPower;
                    StartBatteryLevelSlaveAVG+=BatteryLevelOfSlave;
                    DistanceStats.put("End battery level "+slave.getKey(), String.valueOf(BatteryLevelOfSlave));
                }
                if(slavesMap.size()!=0)
                    StartBatteryLevelSlaveAVG/=slavesMap.size();

                DistanceStats.put("Average Slave Battery level at end", String.valueOf(StartBatteryLevelSlaveAVG));
                DistanceStats.put("Distributed computation Start Time", String.valueOf(StartTimeOfDistributedComp));
                DistanceStats.put("Distributed Computation End Time", String.valueOf(distributedCompEndTime));
                DistanceStats.put("Total time for Distributed Computation in seconds", String.valueOf(distributedCompTime));
                DistanceStats.put("Number of disconnections", String.valueOf(NoOfDisconnections));
                DistanceStats.put("Power Consumption Average (Slave)", String.valueOf(Double.parseDouble(DistanceStats.get("Average Slave Battery level at start"))-StartBatteryLevelSlaveAVG));
                DistanceStats.put("Power consumption Master", String.valueOf(Integer.parseInt(DistanceStats.get("Starting Master Battery Level")) -LevelOfEndBattery));
                for (String slaveEndpoint :
                        slavesMap.keySet()) {
                    sendStatus(slaveEndpoint, Endit);
                }
                MatrixToFile(result, "Result.txt");
                Log.d(TagNameLog, "Total time for distributed computation: "+ distributedCompTime);
                Toast.makeText(getApplicationContext(), "Total time for distributed computation: "+distributedCompTime, Toast.LENGTH_SHORT).show();
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        computeMatrixMultiplication(A, B);
                    }
                }.start();
                PeperProgress.setVisibility(View.INVISIBLE);
            }
            CompletedPercentage = (int)(numberOfRequestsServed*100/numberOfRequests);
            if(CompletedPercentage%10 == 0){
                Log.d(TagNameLog,String.format("%d completed", CompletedPercentage ));
                Toast.makeText(getApplicationContext(), String.format("%d%% completed", CompletedPercentage), Toast.LENGTH_SHORT).show();
            }

            Log.d(TagNameLog, "Number of requests served: "+ numberOfRequestsServed);
//            Toast.makeText(getApplicationContext(), "Number of requests served: "+ numberOfRequestsServed, Toast.LENGTH_SHORT).show();

            CurrRequests.remove(endpointId);
            new Thread() {
                public void run() {
                    for (int i = indices[0]; i < indices[0] + SizeOfPartitn; i++) {
                        for (int j = indices[1]; j < indices[1] + SizeOfPartitn; j++) {
                            result[i][j] = computationResult[i - indices[0]][j - indices[1]];
                        }
                    }
                }
            }.start();

            if(numberOfRequestsServed == numberOfRequests){
                long distributedCompEndTime = System.currentTimeMillis();
                float distributedCompTime = (float) ((distributedCompEndTime - StartTimeOfDistributedComp)/1000.0);

                int EndBatteryLevel = BatteryLevelInfo();
                DistanceStats.put("End Master battery level", String.valueOf(EndBatteryLevel));
                double StartBatteryLevelSlaveAVG = 0;
                for (Map.Entry<String, SecondaryDataModel> slave : slavesMap.entrySet()) {
                    int BatteryLevelslave = slave.getValue().remainingPower;
                    StartBatteryLevelSlaveAVG+=BatteryLevelslave;
                    DistanceStats.put("End battery level "+slave.getKey(), String.valueOf(BatteryLevelslave));
                }
                if(slavesMap.size()!=0)
                    StartBatteryLevelSlaveAVG/=slavesMap.size();

                DistanceStats.put("Average Slave Battery level at end", String.valueOf(StartBatteryLevelSlaveAVG));

                DistanceStats.put("Distributed computation Start Time", String.valueOf(StartTimeOfDistributedComp));
                DistanceStats.put("Distributed Computation End Time", String.valueOf(distributedCompEndTime));
                DistanceStats.put("Total time for Distributed Computation in seconds", String.valueOf(distributedCompTime));

                DistanceStats.put("Number of disconnections", String.valueOf(NoOfDisconnections));
                DistanceStats.put("Power Consumption Average (Slave)", String.valueOf(Double.parseDouble(DistanceStats.get("Average Slave Battery level at start"))-StartBatteryLevelSlaveAVG));
                DistanceStats.put("Power consumption Master", String.valueOf(Integer.parseInt(DistanceStats.get("Starting Master Battery Level")) -EndBatteryLevel));
                for (String slaveEndpoint :
                        slavesMap.keySet()) {
                    sendStatus(slaveEndpoint, Endit);
                }
                MatrixToFile(result, "Result.txt");
                Log.d(TagNameLog, "Total time for distributed computation: "+ distributedCompTime);
                Toast.makeText(getApplicationContext(), "Total time for distributed computation: "+distributedCompTime, Toast.LENGTH_SHORT).show();
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        computeMatrixMultiplication(A, B);
                    }
                }.start();
                PeperProgress.setVisibility(View.INVISIBLE);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateSlaveStatus(JSONObject result, String endpointId) {

        SecondaryDataModel slave = slavesMap.get(endpointId);
        try {
            slave.setSecondaryId(endpointId);

            double slaveLat = result.getDouble("lat");
            double slaveLong = result.getDouble("long");
            slave.setLatitudeCoord(slaveLat);
            slave.setLongitudeCoord(slaveLong);
            if (result.getInt("battery_level") != slave.getRemainingPower()) {
                int batteryLevel = result.getInt("battery_level");
                slave.setRemainingPower(batteryLevel);
                String status = "Time: " + System.currentTimeMillis() +" SlaveId: " + endpointId + ", BatteryLevel: " + batteryLevel + ", Location: (" + slaveLat + ", " + slaveLong + " )" + "\n";
                WriteBatteryStatusToFile(status);
                if(batteryLevel<minBatteryLevel){
                    Toast.makeText(getApplicationContext(), String.format("Slave %s battery is low. Disconnected.", endpointId), Toast.LENGTH_SHORT).show();
                    Log.i(TagNameLog, String.format("Slave %s battery is low. Disconnected.", endpointId));
                    handleSlaveError(endpointId);
                    return;
                }
            }
            double DistFromSlave = getDistance(slaveLat, slaveLong, LocationOnDevice.getLatitude(), LocationOnDevice.getLongitude());
            Log.d(TagNameLog, LocationOnDevice.toString());
            Log.d(TagNameLog, "Slave "+ slaveLat + " " + slaveLong);
            Log.d(TagNameLog, "Slave: "+endpointId+". Distance = " + DistFromSlave);


            // If slave is farther than 100 meters, Disconnect
            if(DistFromSlave > maxDistance){
                Toast.makeText(getApplicationContext(), String.format("Slave %s is too far. Disconnecting", endpointId), Toast.LENGTH_SHORT).show();
                handleSlaveError(endpointId);
                return;
            }
            slavesMap.put(endpointId, slave);
            updateReviewerSlaveList();
            if(ResponseTimeMostRecent == null){
                ResponseTimeMostRecent = new HashMap<>();
            }
            ResponseTimeMostRecent.put(endpointId, System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void WriteBatteryStatusToFile(String battery) {
        File file = new File(getExternalFilesDir(null),
                "batteryStatus.txt");
        FileOutputStream outputStream = null;
        try {
            file.createNewFile();
            outputStream = new FileOutputStream(file, true);
            outputStream.write(battery.getBytes());
            outputStream.flush();
            outputStream.close();
//            Toast.makeText(getApplicationContext(), "Battery status written to file successfully", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteStatsToFile(){
        File file = new File(getExternalFilesDir(null),
                "Benchmarks.txt");
        BufferedWriter bf = null;;

        try{
            //create new BufferedWriter for the output file
            bf = new BufferedWriter( new FileWriter(file) );

            bf.write("****************Distributed Computation*****************");
            bf.newLine();

            for(Map.Entry<String, String> entry : DistanceStats.entrySet()){
                bf.write( entry.getKey() + ":" + entry.getValue() );
                bf.newLine();
            }

            bf.newLine();
            bf.newLine();
            bf.newLine();

            bf.write("****************Standalone Computation******************");
            bf.newLine();

            for(Map.Entry<String, String> entry : StandloneStats.entrySet()){
                bf.write( entry.getKey() + ":" + entry.getValue() );
                bf.newLine();
            }

            bf.flush();

        }catch(IOException e){
            e.printStackTrace();
        }finally{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "All operations Completed. Stats written to file.", Toast.LENGTH_LONG).show();
                }
            });
            try{
                bf.close();
            }catch(Exception e){}
        }
    }


    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endpointId, CallBackPayRoll);

                }


                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        SecondaryDataModel slave = new SecondaryDataModel();
                        slavesMap.put(endpointId, slave);
                    } else {

                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    ResponseTimeMostRecent.remove(endpointId);
                    slavesMap.remove(endpointId);
                    updateReviewerSlaveList();
                    Toast.makeText(getApplicationContext(), String.format("Slave %s disconnected",endpointId), Toast.LENGTH_SHORT).show();
                    handleSlaveError(endpointId);
                }
            };

    private int[][] computeMatrixMultiplication(int[][] matrixA, int[][] matrixB){
        StandloneStats = new LinkedHashMap<>();
        int startBattery = BatteryLevelInfo();
        long startTime = System.currentTimeMillis();
        int i, j, k;
        int [][]res = new int[matrixA.length][matrixA.length];

        for (i = 0; i < matrixA.length; i++) {
            for (j = 0; j < matrixB.length; j++) {
                res[i][j] = 0;
                for (k = 0; k < matrixA.length; k++)
                    res[i][j] += matrixA[i][k]
                            * matrixB[k][j];
            }
        }

        long endTime = System.currentTimeMillis();
        final float computationTimeOnMaster = (float) ((endTime - startTime)/1000.0);
        StandloneStats.put("Start Time", String.valueOf(startTime));
        StandloneStats.put("End Time", String.valueOf(endTime));
        StandloneStats.put("Total Time for computation on Master in seconds", String.valueOf(computationTimeOnMaster));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Computation time on Master alone:" + (computationTimeOnMaster), Toast.LENGTH_SHORT).show();
            }
        });

        int endBattery = BatteryLevelInfo();
        StandloneStats.put("Starting Master Battery Level", String.valueOf(startBattery));
        StandloneStats.put("Ending Master Battery Level", String.valueOf(endBattery));

        StandloneStats.put("Power Consumption", String.valueOf(startBattery - endBattery));
        WriteStatsToFile();
        return res;
    }

    @Override
    protected void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_primary);
        slavesMap = new HashMap<>();
        Monitor = findViewById(R.id.btrack);
        Monitor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMonitorRequest();
            }
        });

        MultiplicationButton = findViewById(R.id.bProd);
        MultiplicationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Nearby.getConnectionsClient(getApplicationContext()).stopDiscovery();
                sendMonitorRequest();
                startDistributedMultiplication();
            }
        });
        SalesFinding = findViewById(R.id.bSearch);
        SalesFinding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDiscovery();
            }
        });

        Disconnect = findViewById(R.id.bShutdown);
        Disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reset();
            }
        });

        createDummyMatrices();

        ClientConnections = Nearby.getConnectionsClient(this);

        RecyclerView SlaveList = findViewById(R.id.recyclerlist);
        SlaveList.setLayoutManager(new LinearLayoutManager(this));
        SlaveList.setAdapter(ApdapterOfSlaveList);
        PeperProgress = findViewById(R.id.pbar);
        PeperProgress.setVisibility(View.INVISIBLE);

        ClientLocatn = LocationServices.getFusedLocationProviderClient(this);
        LocatnReq = LocationRequest.create();

        LocatnReq.setInterval(1000);
        LocatnReq.setSmallestDisplacement(1);

        LocatnCBck = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                Location location = locationResult.getLastLocation();
                if(location != null){
                    LocationOnDevice = location;
                }
            }
        };

        UpdateLocation();


    }

    public void UpdateLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE);
        } else {
            ClientLocatn.requestLocationUpdates(LocatnReq, LocatnCBck, null);
            ClientLocatn.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        LocationOnDevice = location;
                        Log.d(TagNameLog, LocationOnDevice.toString());
                    } else {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        ClientLocatn.requestLocationUpdates(LocatnReq, LocatnCBck, null);
                    }
                }
            });
        }
    }

    private List<SecondaryDataModel> updateReviewerSlaveList() {
        SlaveListRecycler.clear();
        SlaveListRecycler.addAll(this.getSlaveList());
        ApdapterOfSlaveList.notifyDataSetChanged();
        return SlaveListRecycler;
    }

    private ArrayList<SecondaryDataModel> getSlaveList() {

        ArrayList<SecondaryDataModel> list = new ArrayList<>();

        for (Map.Entry<String,SecondaryDataModel> entry : slavesMap.entrySet()) {
            SecondaryDataModel slaveDetails  = entry.getValue();
            list.add(slaveDetails);
        }

        return list;
    }

    private void createDummyMatrices() {
        Random random = new Random();

        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A.length; j++) {
                A[i][j] = random.nextInt();
            }
        }

        MatrixToFile(A, "MatrixA.txt");

        for (int i = 0; i < B.length; i++) {
            for (int j = 0; j < B.length; j++) {
                B[i][j] = random.nextInt();
            }
        }
        MatrixToFile(A, "MatrixB.txt");

        B_T = transpose(B);
    }

    private void MatrixToFile(int[][] matrix, String filename){
        StringBuilder stringBuilder = new StringBuilder();

        for (int[] row :
                matrix) {
            for (int element :
                    row) {
                stringBuilder.append(element).append("\t");
            }
            stringBuilder.append("\r\n");
        }

        Log.d(TagNameLog,"Matrix:\n" + stringBuilder);
        File file = new File(getExternalFilesDir(null), filename);
        FileOutputStream outputStream;
        try {
            file.createNewFile();
            outputStream = new FileOutputStream(file, false);
            outputStream.write(stringBuilder.toString().getBytes());
            outputStream.flush();
            outputStream.close();
//            Toast.makeText(getApplicationContext(), "Battery status written to file successfully", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private int[][] transpose(int[][] matrix){
        for(int i =0; i < matrix.length;i++){
            for(int j = 0; j< matrix[i].length; j ++){
                int x = matrix[i][j];
                matrix[i][j] = matrix[j][i];
                matrix[j][i] = x;
            }
        }
        return matrix;
    }

    private void startDistributedMultiplication() {
        if(slavesMap.size()==0){
            Toast.makeText(getApplicationContext(), "No slaves connected! Cancelling operation", Toast.LENGTH_SHORT).show();
            return;
        }
        NoOfDisconnections = 0;
        DistanceStats = new LinkedHashMap<>();
        PeperProgress.setVisibility(View.VISIBLE);
        StartTimeOfDistributedComp = System.currentTimeMillis();
        Toast.makeText(this, slavesMap.toString(), Toast.LENGTH_SHORT).show();
        CurrRequests = new HashMap<>();
        numberOfRequests = createMatrixPartitions();

        DistanceStats.put("Slaves count", String.valueOf(slavesMap.size()));
        DistanceStats.put("Total requests", String.valueOf(numberOfRequests));

        DistanceStats.put("Starting Master Battery Level", String.valueOf(BatteryLevelInfo()));
        double averageSlaveStartBatteryLevel = 0;
        for (Map.Entry<String, SecondaryDataModel> slave : slavesMap.entrySet()) {
            int slaveBatteryLevel = slave.getValue().remainingPower;
            averageSlaveStartBatteryLevel+=slaveBatteryLevel;
            DistanceStats.put("Start battery level of slave"+slave.getKey(), String.valueOf(slaveBatteryLevel));
        }
        averageSlaveStartBatteryLevel/=slavesMap.size();
        DistanceStats.put("Average Slave Battery level at start", String.valueOf(averageSlaveStartBatteryLevel));

        numberOfRequestsServed = 0;
        distributeComputation();
    }



    private void distributeComputation() {
        for (String slaveEndpoint : slavesMap.keySet()) {
            sendComputationRequestToSlave(slaveEndpoint);
        }
    }

    private void sendComputationRequestToSlave(String slaveEndpoint) {
        JSONObject request = createComputationRequest(slaveEndpoint);
        if (request != null) {
            sendRequestToSlave(slaveEndpoint, request);
        }
    }

    private JSONObject createComputationRequest(String slaveEndpoint) {
        if (!PartitionsOfMatrix.isEmpty()) {
            int[] indices = PartitionsOfMatrix.removeFirst();
            int[][] A_sub = Arrays.copyOfRange(A, indices[0], indices[0] + SizeOfPartitn);
            int[][] B_sub = Arrays.copyOfRange(B_T, indices[1], indices[1] + SizeOfPartitn);

            JSONObject request = new JSONObject();
            try {
                Gson gson = new Gson();
                request.put("request_code", REQUEST_CODES.CALCULATE.request_code);
                String A = gson.toJson(A_sub);
                String B = gson.toJson(B_sub);

                request.put("matrixA", A);
                request.put("matrixB", B);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            addToCurrentRequests(slaveEndpoint, indices);
            return request;
        }
        return null;
    }

    private void addToCurrentRequests(String slaveEndpoint, int[] indices) {
        CurrRequests.put(slaveEndpoint, indices);
    }

    private void sendRequestToSlave(String slaveEndpoint, JSONObject request) {
        Payload payload = Payload.fromStream(new ByteArrayInputStream(request.toString().getBytes(UTF_8)));
        Nearby.getConnectionsClient(getApplicationContext()).sendPayload(slaveEndpoint, payload);
    }

    private int createMatrixPartitions() {
//        partitionSize = A.length/(2*slavesMap.size());
        PartitionsOfMatrix = new LinkedList<>();
        Req = new HashMap<>();
        for (int i = 0; i < A.length; i += SizeOfPartitn) {
            for (int j = 0; j < B_T.length; j += SizeOfPartitn) {
                int[] partition = new int[]{i, j};
                PartitionsOfMatrix.addLast(partition);
                Req.put(partition, false);
            }
        }
        Toast.makeText(getApplicationContext(), "Total number of requests: "+ Req.size(), Toast.LENGTH_SHORT).show();
        return Req.size();
    }

    private void sendMonitorRequest() {
        ResponseTimeMostRecent = new HashMap<>();
        for (Map.Entry<String, SecondaryDataModel> slave : slavesMap.entrySet()) {
            REQUEST_CODES request_code = REQUEST_CODES.MONITOR;
            JSONObject message = new JSONObject();
            try {
                message.put("request_code", request_code.request_code);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendRequestToSlave(slave.getKey(), message);
            ResponseTimeMostRecent.put(slave.getKey(), System.currentTimeMillis());
        }
        enableStatusChecker();
    }

    private void  enableStatusChecker(){
        if(StatusCheckerHandler !=null && StatusCheckRunnable !=null){
            StatusCheckerHandler.removeCallbacks(StatusCheckRunnable);
        }
        StatusCheckerHandler = new Handler();
        StatusCheckRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TagNameLog, "Checking status...");
                if(slavesMap == null){
                    return;
                }
                for (final String slaveEndpoint :
                        slavesMap.keySet()) {
                    if(ResponseTimeMostRecent !=null && (System.currentTimeMillis() - ResponseTimeMostRecent.get(slaveEndpoint))>12000){
                        Log.e(TagNameLog, String.format("Error: Response from slave %s is delayed. Failure Recovery Initialized", slaveEndpoint));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), String.format("Error: Response from slave %s is delayed. Failure Recovery Initialized",slaveEndpoint), Toast.LENGTH_SHORT).show();
                            }
                        });
                        handleSlaveError(slaveEndpoint);
                    }
                }
                StatusCheckerHandler.postDelayed(StatusCheckRunnable, 10000);
            }
        };
        StatusCheckRunnable.run();
    }

    private void handleSlaveError(String slaveEndpoint){
        if(CurrRequests !=null && CurrRequests.containsKey(slaveEndpoint)){
            PartitionsOfMatrix.addLast(CurrRequests.get(slaveEndpoint));
            NoOfDisconnections++;
        }
        slavesMap.remove(slaveEndpoint);
        updateReviewerSlaveList();
        Nearby.getConnectionsClient(getApplicationContext()).disconnectFromEndpoint(slaveEndpoint);
    }

    private void sendStatus(String slaveEndpoint, int statusCode){
        REQUEST_CODES request_code = REQUEST_CODES.STATUS;
        JSONObject message = new JSONObject();
        try {
            message.put("request_code", request_code.request_code);
            message.put("status_code", statusCode);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendRequestToSlave(slaveEndpoint, message);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onStart() {
        super.onStart();

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE);
        }
    }

    @Override
    protected void onStop() {
        Nearby.getConnectionsClient(getApplicationContext()).stopAllEndpoints();
        super.onStop();
    }


    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(getApplicationContext(), "Permission Denied. Closing App!!", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            ClientLocatn.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        LocationOnDevice = location;
                        Log.d(TagNameLog, LocationOnDevice.toString());
                    }
                    else{
                        ClientLocatn.requestLocationUpdates(LocatnReq, LocatnCBck, null);
                    }
                }
            });
        }
        recreate();
    }

    double getDistance(double lat1, double long1, double lat2, double long2) {
        int R = 6371*1000;
        double latD = toRad(lat2-lat1);
        double longD = toRad(long2-long1);
        double x = Math.sin(latD / 2) * Math.sin(latD / 2) +
                Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                        Math.sin(longD / 2) * Math.sin(longD / 2);
        double c = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1-x));
        double distance = R * c;

        return distance;

    }

    double toRad(Double value) {
        return value * Math.PI / 180;
    }

    private void startDiscovery() {
        if(isDiscovering && slavesMap.size() > 1){
            Toast.makeText(getApplicationContext(), "Already Discovering", Toast.LENGTH_SHORT).show();
            return;
        }
      Toast.makeText(getApplicationContext(),"Searching for slaves...", Toast.LENGTH_LONG).show();
        Nearby.getConnectionsClient(getApplicationContext()).startDiscovery(
                Constants.SERVICE_ID, endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(Constants.STRATEGY).build()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                isDiscovering = true;
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Discovery Failed" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Toast.makeText(getApplicationContext(), "Endpoint found", Toast.LENGTH_LONG).show();
                    Nearby.getConnectionsClient(getApplicationContext()).requestConnection("Master", endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                }
            };


    private void reset() {
        for(Map.Entry<String, SecondaryDataModel> entry: slavesMap.entrySet()) {
            Nearby.getConnectionsClient(getApplicationContext()).disconnectFromEndpoint(entry.getKey());
        }
        slavesMap.clear();
        updateReviewerSlaveList();
        Nearby.getConnectionsClient(getApplicationContext()).stopDiscovery();
        isDiscovering = false;

    }


}