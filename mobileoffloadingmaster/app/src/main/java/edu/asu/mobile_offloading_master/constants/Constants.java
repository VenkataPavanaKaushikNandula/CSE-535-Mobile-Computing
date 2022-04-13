package edu.asu.mobile_offloading_master.constants;

import android.Manifest;

import com.google.android.gms.nearby.connection.Strategy;

public class Constants {
    public static final String LogID = "CSE535";
    public static final String secondaryTagName = "Slave";
    public static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
            };
    public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    public static final String SERVICE_ID = "CSE535";
    public static final int REQUEST_CODE = 500;
    public static final int maxDistance = 100;
    public static final int TERMINATE = 0;
    public static final int DELAY_MILLIS = 5000;
    public static final String STATUS_CODE = "status_code";
    public static final String MATRIX_A = "matrixA";
    public static final String MATRIX_B = "matrixB";
    public static final String REQUEST_CODE_STR = "request_code";
    public static final String REQUEST_CODE1 = "request_code";
    public static final String RESULT = "result";
    public static final String CONNECTION_FROM_MASTER = "Press yes to accept incoming connection from Master";
    public static final String YES = "yes";
    public static final String NO = "No";
    public static final String APPLICATION_IS_CONNECTED = "Application is connected";
    public static final String CONNECTED = "Connected";
    public static final String NONE = "None";
    public static final int ACTION_CODE = 500;
}
