package com.example.krock.mytest.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;


/**
 * Created by krock on 2016/6/18.
 */
public class FirstService extends Service {
    private String serverMsg;
    private Channel channel;
    private ChannelFuture channelFuture;
    private ActivityReceiver receiver;
    private Thread thread;
    private boolean canClosed = false;
    //服务器连接状态
    private boolean isConnected;
    private EventLoopGroup group;
    private Bootstrap bootstrap;
    private InetSocketAddress address;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i("service", "服务创建");

        receiver = new ActivityReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.service.msg");
        this.registerReceiver(receiver, filter);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("service", "服务启动");
        //初始化参数
        address = new InetSocketAddress("192.168.1.112",8080);
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new TimeClientHandler());
                    }
                });
        if(isWifiEnabled()){
            startNetworkTread();
        }else{
            Toast.makeText(getApplicationContext(), "网络连接异常",
                    Toast.LENGTH_SHORT).show();
        }

        Intent isStart = new Intent("com.android.Yao.msg");
        isStart.putExtra("serviceValue", "true");
        sendBroadcast(isStart);
        System.out.println("网络线程id" + thread.getId());
        System.out.println("当前线程id" + Thread.currentThread().getId());
        return super.onStartCommand(intent, flags, startId);
    }
    private void startNetworkTread(){
        //检查网络状态后启动连接
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
//                while(!canClosed){
                    connectServer();
//                }

            }
        });
        thread.start();
    }
    public boolean isWifiEnabled() {
        ConnectivityManager mgrConn = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager mgrTel = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        return ((mgrConn.getActiveNetworkInfo() != null && mgrConn
                .getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED) || mgrTel
                .getNetworkType() == TelephonyManager.NETWORK_TYPE_UMTS);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        canClosed = true;
        Log.i("service","服务被销毁");
        Intent isStart = new Intent("com.android.Yao.msg");
        isStart.putExtra("serviceValue", "false");
        sendBroadcast(isStart);
        unregisterReceiver(receiver);
    }
    private void connectServer(){
        try{
            channelFuture = bootstrap.connect(address).sync();
            isConnected = true;
            channel =  channelFuture.channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            isConnected = false;
            e.printStackTrace();
        } finally{

            if(!isConnected){
                try {
                    thread.sleep(5000);
                    Log.i("service", "重连服务器");
                    connectServer();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }
    //广播类
    private class ActivityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //获取actvity传过来的信息
            serverMsg = intent.getStringExtra("msg");
            System.out.println(serverMsg);
            byte[] req = serverMsg.getBytes();
            ByteBuf servermm = Unpooled.buffer(req.length);
            servermm.writeBytes(req);
            channel.writeAndFlush(servermm);
        }
    }
    //信息处理类
    private class TimeClientHandler extends ChannelInboundHandlerAdapter {
        private final ByteBuf firstMessage;

        public TimeClientHandler(){
            byte[] req = ("获取时间").getBytes();
            firstMessage = Unpooled.buffer(req.length);
            firstMessage.writeBytes(req);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            //连接建立 立刻发送消息
            ctx.writeAndFlush(firstMessage);

        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            //服务器断开连接
            isConnected = false;
            super.channelInactive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buf =  (ByteBuf)msg;
            byte[] req = new byte[buf.readableBytes()];
            buf.readBytes(req);
            String body = new String(req,"UTF-8");
            Intent intent = new Intent("com.android.Yao.msg");
            intent.putExtra("serviceValue","现在是:"+ body);
            sendBroadcast(intent);
            System.out.println("现在是:" + body);
        }
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            Log.w("连接出现错误:",cause.getMessage());
            ctx.close();
        }
    }

}
