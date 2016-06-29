package com.example.krock.mytest.connect;


import com.example.krock.mytest.service.FirstService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
/**
 * Created by krock on 2016/6/22.
 */
public class ConnectTool {

        private int port;
        private InetAddress address;

        private EventLoopGroup group;

        private Bootstrap bootstrap;

        private Channel channel;

        private static final ConnectTool con = new ConnectTool();
        private ConnectTool(){}

        public static ConnectTool getInstance(){
            return con;
        }



        private ScheduledExecutorService executorService      = Executors.newScheduledThreadPool(1);

        private  boolean isConnected = false;

        public void init(String host, int port) throws UnknownHostException {
            this.address = InetAddress.getByName(host);
            this.port = port;
            group = new NioEventLoopGroup();
            bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast();
                        }
                    });
        }

        public void start() {
            if(!isConnected){
                connServer();
            }
        }

        /**
         * 是否连接成功
         * @return
         */
        public boolean isConnected(){
            return this.isConnected;
        }

        /**
         * 连接服务器
         */
        private void connServer() {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
//                        System.out.println(isConnected());
                        // 连接服务端
                        if (channel != null && channel.isOpen()) {
                            channel.close();
                        }
                        channel = bootstrap.connect(address, port).sync().channel();
                        isConnected = true;
                        // 此方法会阻塞
                        channel.closeFuture().sync();
                    } catch (Exception e) {
                        isConnected = false;
                        System.out.println("服务器连接失败,请稍后再试  connServer");
                    } finally {
                        if(!isConnected){
                            if(channel.isOpen()){
                                channel.close();
                            }
                            executorService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        TimeUnit.SECONDS.sleep(5000);
                                        connServer();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }

//        private NettyMessage getSrcMsg() {
//            NettyMessage nettyMessage = new NettyMessage();
//            // 设备码
//            // nettyMessage.setSn("sn_123456abcdfef");
//            // nettyMessage.setReqCode(Config.REQ_CODE);
//            return nettyMessage;
//        }

        public void stop() {
            isConnected = false;
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if(executorService!=null){
                executorService.shutdown();
            }
        }


}
