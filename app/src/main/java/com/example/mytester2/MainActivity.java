package com.example.mytester2;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;



import android.Manifest;
//import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    //----------------------------------------------
    int manufacture_id = 0xFFEE;//0x1C01;
    int raw_index = 5;
    int min_rssi = -45;
    int connect_once = 0;
    int connect_index = 0;
    boolean manu_id_adv_only = false;
    //----------------------------------------------
    Button startScanningButton;
    Button stopScanningButton;
    ListView deviceListView;
    TextView textViewTemp;
    //----------------------------------------------
    //    The ListViews in Android are backed by adapters, which hold the data being displayed in a ListView
    //    deviceList will hold the data to be displayed in ListView
    ArrayAdapter<String> listAdapter;
    ArrayList<BluetoothDevice> deviceList;
    BluetoothLeScanner bluetoothLeScanner;
    BluetoothAdapter bluetoothAdapter;
    BluetoothManager bluetoothManager;
    //************************************************************************************
    //                           I N I T
    //************************************************************************************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // получаем элемент textView
        textViewTemp =  findViewById(R.id.textView8);
        //----------------------------------------------
        //              bluetooth
        // получаем элемент ListView
        deviceListView = findViewById(R.id.listView1);
                // создаем адаптер
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        // устанавливаем для списка адаптер
        deviceListView.setAdapter(listAdapter);
        // создаем список БТ устройств
        deviceList = new ArrayList<>();

        initializeBluetooth();
        //----------------------------------------------
        //              button

        startScanningButton = findViewById(R.id.button);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { startScanning(); }});

        stopScanningButton =  findViewById(R.id.button2);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { stopScanning(); }});
    }
    //************************************************************************************
    //                           F U N C T
    //************************************************************************************
    public void initializeBluetooth() {
        /*
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        */
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if (bluetoothLeScanner != null) {
            Log.d(TAG, "scan started");
        }  else {
            Log.e(TAG, "could not get scanner object");
        }
    }

    @SuppressLint("MissingPermission")
    public void startScanning() {

        if (!bluetoothAdapter.isEnabled()) {
            promptEnableBluetooth();
        }
        //initialPageValueCheck();
        //    We only need location permission when we start scanning

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestLocationPermission();
        }
        else
        {
            deviceList.clear();
            listAdapter.clear();
            stopScanningButton.setVisibility(View.VISIBLE);
            startScanningButton.setVisibility(View.INVISIBLE);

            List<ScanFilter> filters = new ArrayList<>();
            ScanFilter.Builder scanFilterBuilder = new ScanFilter.Builder();
            filters.add(scanFilterBuilder.build());

            ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
            settingsBuilder.setLegacy(false);
            AsyncTask.execute(() -> bluetoothLeScanner.startScan(leScanCallBack));
            //AsyncTask.execute(() -> bluetoothLeScanner.startScan(filters,settingsBuilder.build(),leScanCallBack));
        }
    }

    @SuppressLint("MissingPermission")
    public void stopScanning() {
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        connect_once = 0;

        AsyncTask.execute(() -> bluetoothLeScanner.stopScan(leScanCallBack));
    }

    private boolean hasPermission(String permissionType) {
        return ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return;
        }
        runOnUiThread(() -> {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Location Permission Required");
            alertDialog.setMessage("This app needs location access to detect peripherals.");
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE));
            alertDialog.show();
        });
    }

    @SuppressLint("MissingPermission")
    private void promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activityResultLauncher.launch(enableIntent);
        }
    }
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != MainActivity.RESULT_OK) {
                    promptEnableBluetooth();
                }
            }
    );
    //************************************************************************************
    //                      C A L L   B A C K
    //************************************************************************************


    //    The BluetoothLEScanner requires a callback function, which would be called for every device found.
    private final ScanCallback leScanCallBack = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            if (result.getDevice() != null) {
                if (true) {
                    synchronized (result.getDevice()) {

                        BluetoothDevice device = result.getDevice();
                        String itemDetails;

                        //byte[] data = result.getScanRecord().getManufacturerSpecificData(manufacture_id);
                        final String deviceString = "device = " + result.getDevice().toString();
                        final String scanRecordString = "scanRecord = " + result.getScanRecord();
                        final String rssiString = "rssi = " + result.getRssi();

                        itemDetails = device.getAddress();
                        listAdapter.add(itemDetails);
                        deviceList.add(device);

                        textViewTemp.setText(itemDetails);
/*
                        //================================
                        String str_addr = null;
                        boolean found_dev = false;
                        boolean connect_dev = false;


                        if(manu_id_adv_only == true && data == null ){
                            return;
                        }

                        if (data != null && data.length >= (6+raw_index)) {
                            str_addr = String.format("%02X:%02X:%02X:%02X:%02X:%02X",data[5+raw_index],data[4+raw_index],data[3+raw_index],data[2+raw_index],data[1+raw_index],data[0+raw_index]);
                            Log.d(TAG, "Reading size: " + data.length + " with value: " +str_addr);
                        }

                        if(str_addr != null)
                        {
                            found_dev = true;
                            if(connect_once == 0 && result.getRssi() > min_rssi) {
                                connect_dev = true;
                                connect_once = 1;
                                ////////toastShow(result.getDevice(), str_addr);
                                //rfcommConnect(str_addr);
                                ////////bondA2dpConnect(str_addr);
                            }
                        }

                        /////////listShow(result,found_dev,connect_dev);
*/
                    }
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "onScanFailed: code:" + errorCode);
        }
    };





}