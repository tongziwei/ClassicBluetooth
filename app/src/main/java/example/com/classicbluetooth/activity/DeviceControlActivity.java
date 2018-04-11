package example.com.classicbluetooth.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import example.com.classicbluetooth.R;
import example.com.classicbluetooth.constant.Constants;
import example.com.classicbluetooth.service.BlueToothService;
import example.com.classicbluetooth.service.BluetoothChatService;

public class DeviceControlActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "DeviceControlActivity";

    private Toolbar connectToolbar;
    private TextView deviceNameText;
    private TextView deviceAddressText;
    private TextView connectStateText;
    private TextView dataText;
    private EditText dataEditText;
    private Button sendDataButton;

    public static final String EXTRA_DEVICE_NAME =  "Device name";
    public static final String EXTRA_DEVICE_ADDRESS = "Device address";

    private boolean mConnected = false;

    private String mDeviceName;
    private String mDeviceAddress;

    private BlueToothService mBlueToothService;
    private BlueToothService.LocalBinder mBinder;
    private BluetoothChatService mChatService;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBinder = (BlueToothService.LocalBinder)iBinder;
            mBlueToothService = mBinder.getService(DeviceControlActivity.this, handler);
            mBlueToothService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBlueToothService = null;
        }
    };

/**************************************Activity Method**********************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        initView();

        Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);

        deviceNameText.setText(mDeviceName);
        deviceAddressText.setText(mDeviceAddress);

        initService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    private void initView(){
        connectToolbar = (Toolbar)findViewById(R.id.toolbar);
        deviceNameText = (TextView)findViewById(R.id.tv_device_name);
        deviceAddressText = (TextView)findViewById(R.id.tv_device_address);
        connectStateText = (TextView)findViewById(R.id.tv_connect_state);
        dataText = (TextView)findViewById(R.id.tv_data);
        dataEditText = (EditText) findViewById(R.id.et_data);
        sendDataButton = (Button)findViewById(R.id.btn_send_data);

        setSupportActionBar(connectToolbar);

        sendDataButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_send_data:
                String data = dataEditText.getText().toString();
                byte[] dataByte = data.getBytes();
                mBlueToothService.write(dataByte);
                break;
            default:
                break;
        }
    }

    /**
     * 启动服务和绑定服务*/
     private boolean mIsStartService = false;//只StartService一次，设置flag
     private void initService() {
         Intent bindIntent = new Intent(DeviceControlActivity.this, BlueToothService.class);
         if(!mIsStartService){
             startService(bindIntent);
             mIsStartService = true;
         }
         bindService(bindIntent, mServiceConnection, BIND_AUTO_CREATE);
     }
/**************************************Menu Method*************************************************/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_connect,menu);
        if(mConnected){
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        }else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_connect:
                 mBlueToothService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBlueToothService.stopConnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /***********************************************************************************************/
    /**
     **处理程序从BluetoothChatService回来的信息
    **/

    private  Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            try {
                                mConnected = true;
                                invalidateOptionsMenu();
                                connectStateText.setText("已连接");
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            connectStateText.setText("正在连接设备.......");
                            break;
                        case Constants.CONNECT_LOST:
                            connectStateText.setText("连接断开");
                            mConnected = false;
                            invalidateOptionsMenu();
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                            Log.i(TAG, "handleMessage: 正在监听连接状态");
                        case BluetoothChatService.STATE_NONE:
                            Log.i(TAG, "handleMessage: 未连接");
                            mConnected = false;
                            invalidateOptionsMenu();
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    //发送的数据
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    Log.i(TAG, "handleMessage: MESSAGE_WRITE ="+ writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    //接收的数据
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    dataText.setText(readMessage);
                    break;
                case Constants.MESSAGE_TOAST:
                    try {
                        Bundle bundle = msg.getData();
                        String mseeage = bundle.getString(Constants.TOAST);
                        Toast.makeText(DeviceControlActivity.this, mseeage, Toast.LENGTH_SHORT).show();
                        connectStateText.setText("连接失败");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case Constants.STATE_CONNECTED:
                    Bundle bundle = msg.getData();
                    String address = bundle.getString(Constants.DEVICE_ADDRESS);
                    String name = bundle.getString(Constants.DEVICE_NAME);
                    break;
            }
        }
    };
}
