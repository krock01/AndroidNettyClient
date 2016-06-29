package com.example.krock.mytest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.krock.mytest.ui.LoginActivity;

import org.w3c.dom.Text;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private Button csBtn;
    private Button dsBtn;
    private boolean serviceState;
    private ConnectivityManager manager;
//    private ImageView imageView;
    private Button getDateBtn;
    private Button tologin;
    private String msg;
    private UpdateReceiver receiver;
    private  Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.textView);
        csBtn = (Button)findViewById(R.id.csBtn);
        dsBtn = (Button)findViewById(R.id.dsBtn);
        getDateBtn = (Button)findViewById(R.id.getDateBtn);
        tologin = (Button)findViewById(R.id.toLogin);
//        imageView = (ImageView)findViewById(R.id.imageView);
        receiver = new UpdateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.Yao.msg");
        this.registerReceiver(receiver, filter);
        //定义启动service的Intent
        intent = new Intent();
        intent.setAction("com.example.krock.mytest.service.FIRST_SERVICE");
        startService(intent);
        dsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("destroy被点击");
                stopService(intent);
            }
        });
        //添加获取时间按钮事件
        getDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("获取时间按钮被点击");
                Intent intent = new Intent("com.android.service.msg");
                intent.putExtra("msg", "获取时间");
                sendBroadcast(intent);
            }
        });
        tologin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginIn = new Intent(MainActivity.this, LoginActivity.class);

                startActivity(loginIn);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
//        System.out.println(checkNetworkState());
//        if(checkNetworkState()){
//            if(!serviceState){
//              //  startService(intent);
//            }
//
//        }else {
//            Toast.makeText(getApplicationContext(), "网络连接异常",
//                    Toast.LENGTH_SHORT).show();
//        }
        Log.i("activity", "activity重新激活");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        stopService(intent);

    }
    private class UpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //获取service传过来的信息
            String v = intent.getStringExtra("serviceValue");
            switch (v){
                case "true":
                    serviceState = true;
                    break;
                case "false":
                    serviceState = false;
                    break;
                default:
                    textView.append("\n"+v);
                    break;
            }


        }
    }
    private Bitmap loadIntenetImage(){
        URL myFileURL;
        Bitmap bitmap=null;
        try{
            myFileURL = new URL("http://pic.58pic.com/58pic/14/83/08/28858PIC4yQ_1024.jpg");
            //获得连接
            HttpURLConnection conn=(HttpURLConnection)myFileURL.openConnection();
            //设置超时时间为6000毫秒，conn.setConnectionTiem(0);表示没有时间限制
            conn.setConnectTimeout(6000);
            //连接设置获得数据流
            conn.setDoInput(true);
            //不使用缓存
            conn.setUseCaches(false);
            //这句可有可无，没有影响
            //conn.connect();
            //得到数据流
            InputStream is = conn.getInputStream();
            //解析得到图片
            bitmap = BitmapFactory.decodeStream(is);
            //关闭数据流
            is.close();
        }catch(Exception e){
            e.printStackTrace();
        }

        return bitmap;
    }
    /**
     * 检测网络是否连接
     * @return
     */
    private boolean checkNetworkState() {
        boolean flag = false;
        //得到网络连接信息
        manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        System.out.println("网络连接"+networkInfo.isAvailable());
        //去进行判断网络是否连接
        if (networkInfo.isConnected()) {
            System.out.println("网络连接状态"+networkInfo.getState());
//            flag =networkInfo.getState()
        }else{

        }

        return flag;
    }
}
