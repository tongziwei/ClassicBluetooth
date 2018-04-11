package example.com.classicbluetooth;

import android.app.Application;

/**
 * Created by clara.tong on 2018/4/8.
 */

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static MyApplication sInstance = null;
    public static boolean connectState = true;

    public static synchronized MyApplication getInstance(){
        return sInstance;
    }

    public static void setConnectState(boolean connect){
        if(connect){
            connectState = true;
        }else {
            connectState = false;
        }
    }

    public static boolean getConnectState(){
        return connectState;
    }




}
