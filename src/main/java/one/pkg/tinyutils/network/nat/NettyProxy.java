package one.pkg.tinyutils.network.nat;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("all")
public class NettyProxy {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    /**
     * Starts UDP forwarding by automatically detecting STUN mapping and forwarding to the specified internal port.
     *
     * @param internalPort The port number of the internal destination to which packets are forwarded.
     * @throws Exception If an error occurs during STUN detection or UDP forwarding setup.
     */
    public void startUDPForward(int internalPort) throws Exception {
        NATTypeDetector detector = new NATTypeDetector();
        String stunServer = detector.findAvailableStunServer();
        if (stunServer == null) {
            throw new java.io.IOException("No available STUN server found");
        }
        String[] parts = stunServer.split(":");
        String stunHost = parts[0];
        int stunPort = Integer.parseInt(parts[1]);

        DatagramSocket tempSocket = new DatagramSocket();
        tempSocket.setSoTimeout(3000);
        try {
            NATTypeDetector.STUNResponse response = detector.sendSTUNRequest(tempSocket, stunHost, stunPort, false, false);
            if (response == null) {
                tempSocket.close();
                throw new java.io.IOException("STUN request failed");
            }
            int localPort = tempSocket.getLocalPort();
            tempSocket.close();
            startUDPForward(localPort, "127.0.0.1", internalPort);
        } catch (Exception e) {
            tempSocket.close();
            throw e;
        }
    }

    /**
     * Starts a UDP forwarding proxy that listens on the specified port, forwards incoming
     * UDP packets to the given internal host and port, and returns the responses to the sender.
     *
     * @param listenPort   The port on which the proxy listens for incoming UDP packets.
     * @param internalHost The host to which incoming UDP packets will be forwarded.
     * @param internalPort The port on the internal host to which incoming UDP packets will be forwarded.
     * @throws Exception If an error occurs while starting the UDP forwarding proxy or if
     *                   the proxy is already running.
     */
    public void startUDPForward(int listenPort, String internalHost, int internalPort) throws Exception {
        if (running.getAndSet(true)) {
            throw new IllegalStateException("Proxy is already running");
        }

        workerGroup = new NioEventLoopGroup();
        InetSocketAddress internalAddress = new InetSocketAddress(internalHost, internalPort);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioDatagramChannel.class)
                .handler(new UDPForwardHandler(workerGroup, internalAddress));

        serverChannel = bootstrap.bind(listenPort).sync().channel();
    }

    /**
     * Starts TCP forwarding by automatically detecting STUN mapping and forwarding to the specified internal port.
     *
     * @param internalPort The port number of the internal destination to which connections are forwarded.
     * @throws Exception If an error occurs during STUN detection or TCP forwarding setup.
     */
    public void startTCPForward(int internalPort) throws Exception {
        NATTypeDetector detector = new NATTypeDetector();
        String stunServer = detector.findAvailableStunServer();
        if (stunServer == null) {
            throw new IOException("No available STUN server found");
        }
        String[] parts = stunServer.split(":");
        String stunHost = parts[0];
        int stunPort = Integer.parseInt(parts[1]);

        DatagramSocket tempSocket = new DatagramSocket();
        tempSocket.setSoTimeout(3000);
        try {
            NATTypeDetector.STUNResponse response = detector.sendSTUNRequest(tempSocket, stunHost, stunPort, false, false);
            if (response == null) {
                tempSocket.close();
                throw new IOException("STUN request failed");
            }
            int localPort = tempSocket.getLocalPort();
            tempSocket.close();
            startTCPForward(localPort, "127.0.0.1", internalPort);
        } catch (Exception e) {
            tempSocket.close();
            throw e;
        }
    }

    /**
     * Starts a TCP forwarding proxy. Listens for incoming connections on the specified port
     * and forwards the traffic to an internal host and port. This method initializes the
     * necessary Netty components and sets up the channel pipeline for handling TCP traffic.
     *
     * @param listenPort   The port on which the proxy will listen for incoming connections.
     * @param internalHost The hostname or IP address of the internal server to which
     *                     connections will be forwarded.
     * @param internalPort The port on the internal host to which connections will be forwarded.
     * @throws Exception If an error occurs during the initialization or if the proxy
     *                   is already running.
     */
    public void startTCPForward(int listenPort, String internalHost, int internalPort) throws Exception {
        if (running.getAndSet(true)) {
            throw new IllegalStateException("Proxy is already running");
        }

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new TCPFrontendHandler(internalHost, internalPort));
                    }
                });

        serverChannel = bootstrap.bind(listenPort).sync().channel();
    }

    /**
     * Stops the operation of the proxy server.
     * <p>
     * This method sets the running state to false, closes the server channel
     * if it is active, and gracefully shuts down the boss and worker thread groups
     * used by the server.
     */
    public void stop() {
        running.set(false);

        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * Checks whether the proxy server is currently running.
     *
     * @return true if the proxy server is running, false otherwise.
     */
    public boolean isRunning() {
        return running.get();
    }

    private static class UDPForwardHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        private final EventLoopGroup workerGroup;
        private final InetSocketAddress internalAddress;
        private Channel internalChannel;

        UDPForwardHandler(EventLoopGroup workerGroup, InetSocketAddress internalAddress) {
            this.workerGroup = workerGroup;
            this.internalAddress = internalAddress;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
            InetSocketAddress sender = msg.sender();
            ByteBuf content = msg.content().retain();

            if (internalChannel == null || !internalChannel.isActive()) {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(workerGroup)
                        .channel(NioDatagramChannel.class)
                        .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext innerCtx, DatagramPacket innerMsg) {
                                ByteBuf response = innerMsg.content().retain();
                                ctx.writeAndFlush(new DatagramPacket(response, sender));
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext innerCtx, Throwable cause) {
                                cause.printStackTrace();
                                innerCtx.close();
                            }
                        });

                internalChannel = bootstrap.bind(0).sync().channel();
            }

            internalChannel.writeAndFlush(new DatagramPacket(content, internalAddress));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

    private static class TCPFrontendHandler extends ChannelInboundHandlerAdapter {
        private final String internalHost;
        private final int internalPort;
        private Channel internalChannel;

        TCPFrontendHandler(String internalHost, int internalPort) {
            this.internalHost = internalHost;
            this.internalPort = internalPort;
        }

        private static void closeOnFlush(Channel ch) {
            if (ch.isActive()) {
                ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            Channel frontendChannel = ctx.channel();

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(frontendChannel.eventLoop())
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new TCPBackendHandler(frontendChannel));
                        }
                    });

            ChannelFuture future = bootstrap.connect(internalHost, internalPort);
            internalChannel = future.channel();

            future.addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    frontendChannel.read();
                } else {
                    frontendChannel.close();
                }
            });
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (internalChannel != null && internalChannel.isActive()) {
                internalChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        ctx.channel().read();
                    } else {
                        future.channel().close();
                    }
                });
            } else {
                if (msg instanceof ByteBuf) {
                    ((ByteBuf) msg).release();
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (internalChannel != null) {
                closeOnFlush(internalChannel);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            closeOnFlush(ctx.channel());
        }
    }

    private static class TCPBackendHandler extends ChannelInboundHandlerAdapter {
        private final Channel frontendChannel;

        TCPBackendHandler(Channel frontendChannel) {
            this.frontendChannel = frontendChannel;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (frontendChannel.isActive()) {
                frontendChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        ctx.channel().read();
                    } else {
                        future.channel().close();
                    }
                });
            } else {
                if (msg instanceof ByteBuf) {
                    ((ByteBuf) msg).release();
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (frontendChannel.isActive()) {
                frontendChannel.writeAndFlush(Unpooled.EMPTY_BUFFER)
                        .addListener(ChannelFutureListener.CLOSE);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
