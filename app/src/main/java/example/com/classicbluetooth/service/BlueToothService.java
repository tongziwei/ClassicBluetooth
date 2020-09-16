package example.com.classicbluetooth.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import example.com.classicbluetooth.MyApplication;
import example.com.classicbluetooth.constant.Constants;
/*蓝牙连接后台服务*/
public class BlueToothService extends Service {
    private static final String TAG = "BlueToothService";
    private final IBinder mBinder = new LocalBinder();
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private Intent intent;
    private BluetoothChatService mChatService;
    private BluetoothDevice device;
    private boolean isRun = true;
    private String address;

    @Override
    public void onCreate() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //获得BluetoothAdapter
        registerReceiver(receiver,intentFilter());
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder{
        public BlueToothService getService(Context context, Handler handler){
            mChatService = BluetoothChatService.getInstance(context,handler);
            isListen = true;
            StateListenThread thread = null;
            if (thread == null){
                thread = new StateListenThread();
            }
            thread.start();
            return BlueToothService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind: ");
        unregisterReceiver(receiver);
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: ");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        isListen = false;
        MyApplication.getInstance().setConnectState(false);
        mChatService.stop();
        super.onDestroy();
    }
/******************************************************************************************/
   /*开始扫描*/
    public void startScan(){
        if(mBluetoothAdapter != null){
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothAdapter.startDiscovery();
        }else{
            mBluetoothAdapter.startDiscovery();;
        }
    }

    /*停止扫描*/
    public void stopScan(){
        if (mBluetoothAdapter != null){
            if(mBluetoothAdapter.isDiscovering()){
                mBluetoothAdapter.cancelDiscovery();
            }
        }
    }

    /*连接蓝牙*/
    public void connect(String address){
        this.address = address;
        if (mBluetoothAdapter != null){
            if(!"0".equals(address)){
                device = mBluetoothAdapter.getRemoteDevice(address);
            }
        }
        if(mChatService != null){
            if(mChatService.getState() == BluetoothChatService.STATE_NONE){
                mChatService.connect(device,true);
            }else if(mChatService.getState() == BluetoothChatService.STATE_CONNECTED){
                intent = new Intent();
                intent.setAction(Constants.ACTION_BLUE_CONNECTED);
                BlueToothService.this.sendBroadcast(intent);
            }
        }
    }

    /*断开蓝牙连接*/
    public void stopConnect(){
        mChatService.stop();
    }

    /*写数据*/
    public void write(byte [] out){
        mChatService.write(out);
    }

    /*设置状态监听*/
    public void setStateListen(boolean isRun){
        this.isRun = isRun;
    }


/******************************************************************************************/
    private boolean isListen;
    /**
     * 状态监听，便于调试
     * @author Administrator
     */
    class StateListenThread extends Thread{

        @Override
        public void run() {
            while (isListen) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int state = mChatService.getState();
                Log.e(TAG,"StateListenThread----state="+state);
            }
        }
    }
    /*广播接收器，接收到扫描设备相关广播*/
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_FOUND)){  //搜索到蓝牙设备
                BluetoothDevice btd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
               /* String deviceInfo = btd.getName()+'\n'+btd.getAddress();
                String deviceName = btd.getName();
                String deviceAddress = btd.getAddress();
                intent.putExtra("deviceInfo",deviceInfo);*/
                intent.putExtra("device",btd);
                intent.setAction(Constants.ACTION_FOUND_BLUE);
                BlueToothService.this.sendBroadcast(intent);
            }else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                intent.setAction(Constants.ACTION_DISCOVERY_FINISHED);
                BlueToothService.this.sendBroadcast(intent);
            }else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
                intent.setAction(Constants.ACTION_DISCOVERY_STARTED);
                BlueToothService.this.sendBroadcast(intent);
            }
        }
    };

    /*广播过滤器*/
    private static IntentFilter intentFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND); //发现设备
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//设备配对状态改变
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //开始扫描
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//结束扫描
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);//扫描模式改变
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //动作状态改变
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);//连接状态改变
        return filter;
    }


}
