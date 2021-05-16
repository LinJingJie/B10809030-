package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName()+"My";
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 102;
    private static final int REQUEST_ENABLE_BT = 2;
    private boolean isScanning = false;
    ArrayList<ScannedData> findDevice = new ArrayList<>();
    RecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**權限相關認證*/
        checkPermission();
        /**初始藍牙掃描及掃描開關之相關功能*/
        bluetoothScan();
        /**取得欲連線之裝置後跳轉頁面*/
        mAdapter.OnItemClick(itemClick);

    }
    private void checkPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            int hasGone = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasGone != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOCATION_PERMISSION);
            }
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this,"Not support Bluetooth", Toast.LENGTH_SHORT).show();
                finish();
            }
            if(!mBluetoothAdapter.isEnabled()){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
            }
        }else finish();
    }
    private void bluetoothScan() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        isScanning = true;
        RecyclerView recyclerView = findViewById(R.id.recyclerView_ScannedList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new RecyclerViewAdapter(this);
        recyclerView.setAdapter(mAdapter);
        final Button btScan = findViewById(R.id.button_Scan);
        btScan.setOnClickListener((v)-> {
            if (isScanning) {
                isScanning = false;
                btScan.setText("開始掃描");
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }else{
                isScanning = true;
                btScan.setText("停止掃描");
                findDevice.clear();
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                mAdapter.clearDevice();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        final Button btScan = findViewById(R.id.button_Scan);
        isScanning = true;
        btScan.setText("停止掃描");
        findDevice.clear();
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        mAdapter.clearDevice();
    }
    @Override
    protected void onStop() {
        super.onStop();
        final Button btScan = findViewById(R.id.button_Scan);
        isScanning = false;
        btScan.setText("開始掃描");
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            new Thread(()->{
                if (device.getName()!= null){
                    findDevice.add(new ScannedData(device.getName()
                            , String.valueOf(rssi)
                            , byteArrayToHexStr(scanRecord)
                            , device.getAddress()));
                    ArrayList newList = getSingle(findDevice);
                    runOnUiThread(()->{
                        mAdapter.addDevice(newList);
                    });
                }
            }).start();
        }
    };
    private ArrayList getSingle(ArrayList list) {
        ArrayList tempList = new ArrayList<>();
        try {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                if (!tempList.contains(obj)) {
                    tempList.add(obj);
                } else {
                    tempList.set(getIndex(tempList, obj), obj);
                }
            }
            return tempList;
        } catch (ConcurrentModificationException e) {
            return tempList;
        }
    }
    private int getIndex(ArrayList temp, Object obj) {
        for (int i = 0; i < temp.size(); i++) {
            if (temp.get(i).toString().contains(obj.toString())) {
                return i;
            }
        }
        return -1;
    }
    public static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }

        StringBuilder hex = new StringBuilder(byteArray.length * 2);
        for (byte aData : byteArray) {
            hex.append(String.format("%02X", aData));
        }
        String gethex = hex.toString();
        return gethex;
    }
    private RecyclerViewAdapter.OnItemClick itemClick = new RecyclerViewAdapter.OnItemClick() {
        @Override
        public void onItemClick(ScannedData selectedDevice) {

            Intent intent = new Intent(MainActivity.this,ScannedData.class);
            intent.putExtra("ScannedData.intent_key",selectedDevice);
            startActivity(intent);
        }
    };

}