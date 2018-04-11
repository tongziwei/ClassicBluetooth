package example.com.classicbluetooth.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

import example.com.classicbluetooth.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Toolbar toolbar;
    private ListView devicesList;
    private ListView bondedList;
    private TextView tvBlueState;

    private boolean mScanning;

    private LeDeviceListAdapter mDeviceListAdapter;
    private LeDeviceListAdapter mBondedDeviceListAdapter;

    private BluetoothAdapter mBluetoothAdapter;

/************************************Activity Method************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initBlueTooth();
        itemClickListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDeviceListAdapter = new LeDeviceListAdapter();
        devicesList.setAdapter(mDeviceListAdapter);
        scanDevices(true);
        initFilter();
        mBondedDeviceListAdapter = new LeDeviceListAdapter();
        getBonedDevice();
        bondedList.setAdapter(mBondedDeviceListAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(deviceReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

/***********************************************************************************/
    /***
     * 初始化控件
     */
    private void initView(){
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        devicesList = (ListView)findViewById(R.id.bluetooth_device_list);
        bondedList = (ListView)findViewById(R.id.bluetooth_bonded_device_list);
        tvBlueState = (TextView)findViewById(R.id.tv_bluetooth_state);
    }

    /**
     * 设置广播过滤器
     */
    private void initFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND); //发现设备
      //  filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//设备配对状态改变
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //开始扫描
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//结束扫描
       // filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);//扫描模式改变
        //filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //动作状态改变
        //filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);//连接状态改变
        registerReceiver(deviceReceiver, filter);
    }
/******************************menu method**********************************************************/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_scan,menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_scan:
                mDeviceListAdapter.clear();
                scanDevices(true);
                break;
            case R.id.menu_stop:
                scanDevices(false);
                break;
        }
        return true;
    }

    /**
     * 开启本机蓝牙
     */
    private void initBlueTooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) { // Device support Bluetooth
            // 确认开启蓝牙
            if (!mBluetoothAdapter.isEnabled()) {
                // 请求用户开启
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, RESULT_FIRST_USER);
            }
        } else { // Device does not support Bluetooth
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("No bluetooth devices");
            dialog.setMessage("Your equipment does not support bluetooth, please change device");
            dialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            dialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RESULT_FIRST_USER && resultCode == Activity.RESULT_CANCELED){
            finish();
        }

    }

    /**
     * 扫描蓝牙设备
     * */
    private void scanDevices(boolean enable){
        if(enable){
            if(mBluetoothAdapter != null){
                mBluetoothAdapter.cancelDiscovery();
                mBluetoothAdapter.startDiscovery();
            }else{
                mBluetoothAdapter.startDiscovery();;
            }
            mScanning = true;
        }else{
            if (mBluetoothAdapter != null){
                if(mBluetoothAdapter.isDiscovering()){
                    mBluetoothAdapter.cancelDiscovery();
                }
            }
            mScanning = false;
        }
        invalidateOptionsMenu();
    }

 /**
  * 广播接收器，接收到扫描设备相关广播
  * */
   private final BroadcastReceiver deviceReceiver = new BroadcastReceiver() {
       @Override
       public void onReceive(Context context, Intent intent) {
           String action = intent.getAction();
           if(action.equals(BluetoothDevice.ACTION_FOUND)){  //搜索到蓝牙设备
               BluetoothDevice mBluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
               mDeviceListAdapter.addDevice(mBluetoothDevice);
               mDeviceListAdapter.notifyDataSetChanged();
               tvBlueState.setText(R.string.device_scanned);
               tvBlueState.setTextColor(Color.RED);
           }else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
               mScanning = false;
               invalidateOptionsMenu();
               tvBlueState.setText(R.string.scan_finished);
               tvBlueState.setTextColor(Color.GREEN);
           }else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
               tvBlueState.setText(R.string.scanning);
               tvBlueState.setTextColor(Color.GREEN);
           }
       }
   };
    /**
     * 获得已配对的设备
     */
    private void getBonedDevice(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
         // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
               mBondedDeviceListAdapter.addDevice(device);
                mBondedDeviceListAdapter.notifyDataSetChanged();
            }
        }else{
            Log.e(TAG, "getBonedDevice: 没有已经配对过的设备" );
            mBondedDeviceListAdapter.notifyDataSetChanged();
        }
    }

    /**************************************************************************************************/
    private void itemClickListener(){
        devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice device = mDeviceListAdapter.getDevice(i);
                if(device == null) return;
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                Intent intent =  new Intent(MainActivity.this,DeviceControlActivity.class);
                intent.putExtra(DeviceControlActivity.EXTRA_DEVICE_NAME,deviceName);
                intent.putExtra(DeviceControlActivity.EXTRA_DEVICE_ADDRESS,deviceAddress);
                if(mScanning){
                    scanDevices(false);
                }
                startActivity(intent);
            }
        });

        bondedList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice device = mBondedDeviceListAdapter.getDevice(i);
                if(device == null) return;
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                Intent intent =  new Intent(MainActivity.this,DeviceControlActivity.class);
                intent.putExtra(DeviceControlActivity.EXTRA_DEVICE_NAME,deviceName);
                intent.putExtra(DeviceControlActivity.EXTRA_DEVICE_ADDRESS,deviceAddress);
                if(mScanning){
                    scanDevices(false);
                }
                startActivity(intent);
            }
        });
    }

/*****************************************************************************************************/
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices=new ArrayList<BluetoothDevice>();
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device){
            if(!mLeDevices.contains(device)){
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position){
            return mLeDevices.get(position);
        }

        public void clear(){
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if(view == null){
                view = mInflator.inflate(R.layout.bluetooth_device_list_item,null);
                //view = LayoutInflater.from(getApplicationContext()).inflate(resourceId,viewGroup,false);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = (TextView)view.findViewById(R.id.device_name);
                viewHolder.deviceAddress = (TextView)view.findViewById(R.id.device_address);
                view.setTag(viewHolder);
            }else {
                viewHolder = (ViewHolder)view.getTag();
            }

            BluetoothDevice device = (BluetoothDevice) getItem(i);
            String deviceName = device.getName();
            if(deviceName != null && deviceName.length() > 0){
                viewHolder.deviceName.setText(deviceName);
            }else{
                viewHolder.deviceName.setText(R.string.unknow_device);
            }
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }

        class ViewHolder{
            TextView deviceName;
            TextView deviceAddress;
        }
    }


}
